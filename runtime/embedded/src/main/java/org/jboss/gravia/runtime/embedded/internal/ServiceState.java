/*
 * #%L
 * JBossOSGi Framework
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.gravia.runtime.embedded.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.runtime.Constants;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceException;
import org.jboss.gravia.runtime.ServiceFactory;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.runtime.spi.AbstractModule;
import org.jboss.gravia.runtime.util.NotNullException;
import org.jboss.gravia.runtime.util.UnmodifiableDictionary;
import org.jboss.logging.Logger;
import org.jboss.osgi.metadata.CaseInsensitiveDictionary;

/**
 * The internal representation of a service
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 *
 * @ThreadSafe
 */
@SuppressWarnings("rawtypes")
final class ServiceState<S> implements ServiceRegistration<S>, ServiceReference<S> {

    private static Logger LOGGER = Logger.getLogger(ServiceState.class);

    private final RuntimeServicesManager serviceManager;
    private final Module ownerModule;
    private final String[] classNames;
    private final ValueProvider<S> valueProvider;
    private final ServiceReference<S> reference;
    private final Set<AbstractModule> usingModules = new HashSet<AbstractModule>(); // @GuardedBy("usingModules")
    private final Map<ResourceIdentity, ServiceFactoryHolder<S>> factoryValues; // @GuardedBy("ConcurrentHashMap")
    private final ServiceRegistration<S> registration;

    // The properties
    private CaseInsensitiveDictionary prevProperties; // @GuardedBy("propsLock")
    private CaseInsensitiveDictionary currProperties; // @GuardedBy("propsLock")
    private Object propsLock = new Object();

    private String cachedToString;

    interface ValueProvider<S> {
        boolean isFactoryValue();
        S getValue();
    }

    @SuppressWarnings("unchecked")
    ServiceState(RuntimeServicesManager serviceManager, Module owner, long serviceId, String[] classNames, ValueProvider<S> valueProvider, Dictionary properties) {
        assert serviceManager != null : "Null serviceManager";
        assert owner != null : "Null owner";
        assert classNames != null && classNames.length > 0 : "Null clazzes";
        assert valueProvider != null : "Null valueProvider";

        this.serviceManager = serviceManager;
        this.ownerModule = owner;
        this.valueProvider = valueProvider;
        this.classNames = classNames;

        if (!valueProvider.isFactoryValue() && !checkValidClassNames(owner, classNames, valueProvider.getValue()))
            throw new IllegalArgumentException("Invalid objectClass: " + Arrays.toString(classNames));

        if (properties == null)
            properties = new Hashtable();

        properties.put(Constants.SERVICE_ID, serviceId);
        properties.put(Constants.OBJECTCLASS, classNames);
        this.currProperties = new CaseInsensitiveDictionary(properties);
        this.cachedToString = updateCachedToString();

        // Create the {@link ServiceRegistration} and {@link ServiceReference}
        this.registration = new ServiceRegistrationWrapper(this);
        this.reference = new ServiceReferenceWrapper(this);

        this.factoryValues = valueProvider.isFactoryValue() ? new ConcurrentHashMap<ResourceIdentity, ServiceFactoryHolder<S>>() : null;
    }

    static ServiceState assertServiceState(ServiceReference sref) {
        assert sref != null : "Null sref";
        if (sref instanceof ServiceReferenceWrapper) {
            sref = ((ServiceReferenceWrapper) sref).getServiceState();
        }
        return (ServiceState) sref;
    }

    S getScopedValue(Module module) {

        // For non-factory services, return the value
        if (valueProvider.isFactoryValue() == false)
            return valueProvider.getValue();

        // Get the ServiceFactory value
        S result = null;
        try {
            ServiceFactoryHolder<S> factoryHolder = getFactoryHolder(module);
            if (factoryHolder == null) {
                ServiceFactory factory = (ServiceFactory) valueProvider.getValue();
                factoryHolder = new ServiceFactoryHolder<S>(module, factory);
                factoryValues.put(module.getIdentity(), factoryHolder);
            }

            result = factoryHolder.getService();

            // If the service object returned by the ServiceFactory object is not an instanceof all the classes named
            // when the service was registered or the ServiceFactory object throws an exception,
            // null is returned and a Framework event of type {@link FrameworkEvent#ERROR}
            // containing a {@link ServiceException} describing the error is fired.
            if (result == null) {
                ServiceException sex = new ServiceException("Cannot get factory value", ServiceException.FACTORY_ERROR);
                LOGGER.error(sex);
            }
        } catch (Throwable th) {
            ServiceException sex = new ServiceException("Cannot get factory value", ServiceException.FACTORY_EXCEPTION, th);
            LOGGER.error(sex);
        }
        return result;
    }


    void ungetScopedValue(Module module) {
        if (valueProvider.isFactoryValue()) {
            ServiceFactoryHolder factoryHolder = getFactoryHolder(module);
            if (factoryHolder != null) {
                try {
                    factoryHolder.ungetService();
                } catch (RuntimeException rte) {
                    ServiceException sex = new ServiceException("Cannot unget factory value", ServiceException.FACTORY_EXCEPTION, rte);
                    LOGGER.error(sex);
                }
            }
        }
    }

    private ServiceFactoryHolder<S> getFactoryHolder(Module module) {
        return factoryValues != null ? factoryValues.get(module.getIdentity()) : null;
    }


    ServiceRegistration<S> getRegistration() {
        return registration;
    }


    List<String> getClassNames() {
        return Arrays.asList(classNames);
    }


    @Override
    public ServiceReference<S> getReference() {
        assertNotUnregistered();
        return reference;
    }


    @Override
    public void unregister() {
        assertNotUnregistered();
        unregisterInternal();
    }


    private void unregisterInternal() {
        serviceManager.unregisterService(this);
    }


    @Override
    public Object getProperty(String key) {
        synchronized (propsLock) {
            return key != null ? currProperties.get(key) : null;
        }
    }


    @Override
    public String[] getPropertyKeys() {
        synchronized (propsLock) {
            List<String> result = new ArrayList<String>();
            Enumeration<String> keys = currProperties.keys();
            while (keys.hasMoreElements())
                result.add(keys.nextElement());
            return result.toArray(new String[result.size()]);
        }
    }


    @Override
    @SuppressWarnings({ "unchecked" })
    public void setProperties(Dictionary properties) {
        assertNotUnregistered();

        // Remember the previous properties for a potential
        // delivery of the MODIFIED_ENDMATCH event
        synchronized (propsLock) {
            prevProperties = currProperties;

            if (properties == null)
                properties = new Hashtable();

            properties.put(Constants.SERVICE_ID, currProperties.get(Constants.SERVICE_ID));
            properties.put(Constants.OBJECTCLASS, currProperties.get(Constants.OBJECTCLASS));
            currProperties = new CaseInsensitiveDictionary(properties);
        }

        // This event is synchronously delivered after the service properties have been modified.
        serviceManager.fireServiceEvent(ownerModule, ServiceEvent.MODIFIED, this);
    }


    @SuppressWarnings("unchecked")
    Dictionary<String, ?> getPreviousProperties() {
        synchronized (propsLock) {
            return new UnmodifiableDictionary(prevProperties);
        }
    }


    Module getServiceOwner() {
        return ownerModule;
    }


    @Override
    public Module getModule() {
        return (isUnregistered() ? null : ownerModule);
    }


    void addUsingModule(AbstractModule module) {
        synchronized (usingModules) {
            usingModules.add(module);
        }
    }


    void removeUsingModule(AbstractModule module) {
        synchronized (usingModules) {
            usingModules.remove(module);
        }
    }


    Set<AbstractModule> getUsingModulesInternal() {
        // Return an unmodifieable snapshot of the set
        synchronized (usingModules) {
            return Collections.unmodifiableSet(new HashSet<AbstractModule>(usingModules));
        }
    }

    @Override
    public boolean isAssignableTo(Module module, String className) {
        NotNullException.assertValue(module, "module");
        NotNullException.assertValue(className, "className");

        if (module == ownerModule || className.startsWith("java."))
            return true;

        if (module.getState() == Module.State.UNINSTALLED)
            return false;

        ClassLoader moduleClassLoader = module.adapt(ClassLoader.class);
        if (moduleClassLoader == null) {
            LOGGER.infof("No ClassLoader for: %s", module);
            return false;
        }

        Class<?> targetClass;
        try {
            targetClass = moduleClassLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            // If the requesting module does not have a wire to the
            // service package it cannot be constraint on that package.
            LOGGER.tracef("Requesting module [%s] cannot load class: %s", module, className);
            return true;
        }

        ClassLoader ownerClassLoader = ownerModule.adapt(ClassLoader.class);
        if (ownerClassLoader == null) {
            LOGGER.tracef("Registrant module [%s] has no class loader for: %s", ownerModule, className);
            return true;
        }

        // For the module that registered the service referenced by this ServiceReference (registrant module);
        // find the source for the package. If no source is found then return true if the registrant module
        // is equal to the specified module; otherwise return false
        Class<?> serviceClass;
        try {
            serviceClass = ownerClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.tracef("Registrant module [%s] cannot load class: %s", ownerModule, className);
            return true;
        }

        // If the package source of the registrant module is equal to the package source of the specified module
        // then return true; otherwise return false.
        if (targetClass != serviceClass) {
            LOGGER.tracef("Not assignable: %s", className);
            return false;
        }

        return true;
    }


    @Override
    public int compareTo(Object sref) {
        if (!(sref instanceof ServiceReference))
            throw new IllegalArgumentException("Invalid ServiceReference: " + sref);

        Comparator<ServiceReference<?>> comparator = ServiceReferenceComparator.getInstance();
        return comparator.compare(this, (ServiceReference) sref);
    }

    boolean isUnregistered() {
        return registration == null;
    }

    private void assertNotUnregistered() {
        if (isUnregistered())
            throw new IllegalStateException("Service unregistered: " + this);
    }

    private boolean checkValidClassNames(Module module, String[] classNames, Object value) {
        assert module != null : "Null module";
        assert classNames != null && classNames.length > 0 : "Null service classes";
        assert value != null : "Null value";

        if (value instanceof ServiceFactory)
            return true;

        boolean result = true;
        for (String className : classNames) {
            if (className == null) {
                result = false;
                break;
            }
            try {
                Class<?> valueClass = value.getClass();
                // Use Class.forName with classloader argument as the classloader
                // might be null (for JRE provided types).
                Class<?> clazz = Class.forName(className, false, valueClass.getClassLoader());
                if (clazz.isAssignableFrom(valueClass) == false) {
                    LOGGER.errorf("Service interface [%s] loaded from [%s] is not assignable from [%s] loaded from [%s]", className, clazz.getClassLoader(), valueClass.getName(), valueClass.getClassLoader());
                    result = false;
                    break;
                }
            } catch (ClassNotFoundException ex) {
                LOGGER.errorf("Cannot load [%s] from: %s", className, module);
                result = false;
                break;
            }
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private String updateCachedToString() {
        synchronized (propsLock) {
            Hashtable<String, Object> props = new Hashtable<String, Object>(currProperties);
            String[] classes = (String[]) currProperties.get(Constants.OBJECTCLASS);
            props.put(Constants.OBJECTCLASS, Arrays.asList(classes));
            return "ServiceState" + props;
        }
    }

    @Override
    public String toString() {
        return cachedToString.toString();
    }

    class ServiceFactoryHolder<T> {

        ServiceFactory factory;
        Module module;
        AtomicInteger useCount;
        T value;

        ServiceFactoryHolder(Module module, ServiceFactory factory) {
            this.module = module;
            this.factory = factory;
            this.useCount = new AtomicInteger();
        }

        @SuppressWarnings("unchecked")
        T getService() {
            // Multiple calls to getService() return the same value
            if (useCount.get() == 0) {
                // The Framework must not allow this method to be concurrently called for the same module
                synchronized (module) {
                    T retValue = (T) factory.getService(module, getRegistration());
                    if (retValue == null)
                        return null;

                    // The Framework will check if the returned service object is an instance of all the
                    // classes named when the service was registered. If not, then null is returned to the module.
                    if (checkValidClassNames(ownerModule, (String[]) getProperty(Constants.OBJECTCLASS), retValue) == false)
                        return null;

                    value = retValue;
                }
            }

            useCount.incrementAndGet();
            return value;
        }

        @SuppressWarnings("unchecked")
        void ungetService() {
            if (useCount.get() == 0)
                return;

            // Call unget on the factory when done
            if (useCount.decrementAndGet() == 0) {
                synchronized (module) {
                    factory.ungetService(module, getRegistration(), value);
                    value = null;
                }
            }
        }
    }
}

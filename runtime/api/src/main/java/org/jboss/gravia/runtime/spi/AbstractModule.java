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
package org.jboss.gravia.runtime.spi;

import static org.jboss.gravia.runtime.spi.RuntimeLogger.LOGGER;

import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.resource.AttachmentKey;
import org.jboss.gravia.resource.DictionaryResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.resource.spi.AttachableSupport;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.utils.CaseInsensitiveDictionary;
import org.jboss.gravia.utils.NotNullException;
import org.jboss.gravia.utils.UnmodifiableDictionary;
import org.osgi.framework.Bundle;

/**
 * The abstract base implementaiton for all {@link Module}s.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public abstract class AbstractModule implements Module, Attachable {

    public static AttachmentKey<ModuleEntriesProvider> MODULE_ENTRIES_PROVIDER_KEY = AttachmentKey.create(ModuleEntriesProvider.class);

    private final AbstractRuntime runtime;
    private final ClassLoader classLoader;
    private final Resource resource;
    private final Dictionary<String, String> headers;
    private final Attachable attachments = new AttachableSupport();
    private final ConcurrentHashMap<ServiceReference<?>, AtomicInteger> usedServices = new ConcurrentHashMap<ServiceReference<?>, AtomicInteger>();

    protected AbstractModule(AbstractRuntime runtime, ClassLoader classLoader, Resource resource, Dictionary<String, String> headers) {
        NotNullException.assertValue(runtime, "runtime");
        NotNullException.assertValue(classLoader, "classLoader");
        this.runtime = runtime;
        this.classLoader = classLoader;

        // One of the two must be given to determine the identity
        if (resource == null && headers == null)
            throw new IllegalArgumentException("Cannot create module identity");

        // Build the resource
        if (resource == null) {
            ResourceBuilder builder = new DictionaryResourceBuilder().load(headers);
            resource = builder.getResource();
        }
        this.resource = resource;

        // Build the headers
        ResourceIdentity resourceIdentity = resource.getIdentity();
        Hashtable<String, String> coloned = new Hashtable<String, String>();
        if (headers != null) {
            Enumeration<String> keys = headers.keys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = headers.get(key);
                coloned.put(key, value);
            }
        }
        if (coloned.get(Constants.GRAVIA_IDENTITY_CAPABILITY) == null) {
            String identityHeader = getIdentityHeader(resourceIdentity);
            coloned.put(Constants.GRAVIA_IDENTITY_CAPABILITY, identityHeader);
        }
        this.headers = new UnmodifiableDictionary<String, String>(new CaseInsensitiveDictionary<String>(coloned));

        // Verify the resource & headers identity
        ResourceIdentity headersIdentity = new DictionaryResourceBuilder().load(coloned).getResource().getIdentity();
        if (!resourceIdentity.equals(headersIdentity))
            throw new IllegalArgumentException("Resource and header identity does not match: " + resourceIdentity);
    }

    private String getIdentityHeader(ResourceIdentity identity) {
        String symbolicName = identity.getSymbolicName();
        Version version = identity.getVersion();
        return symbolicName + ";version=" + version;
    }

    protected abstract void setState(State newState);

    protected AbstractRuntime getRuntime() {
        return runtime;
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public ResourceIdentity getIdentity() {
        return resource.getIdentity();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type) {
        A result = null;
        if (type.isAssignableFrom(Bundle.class)) {
            result = (A) getBundleAdaptor(this);
        } else if (type.isAssignableFrom(Runtime.class)) {
            result = (A) runtime;
        } else if (type.isAssignableFrom(AbstractRuntime.class)) {
            result = (A) runtime;
        } else if (type.isAssignableFrom(ClassLoader.class)) {
            result = (A) classLoader;
        } else if (type.isAssignableFrom(Resource.class)) {
            result = (A) resource;
        } else if (type.isAssignableFrom(Module.class)) {
            result = (A) this;
        } else if (type.isAssignableFrom(ModuleContext.class)) {
            result = (A) getModuleContext();
        } else if (type.isAssignableFrom(ModuleEntriesProvider.class)) {
            result = (A) getAttachment(MODULE_ENTRIES_PROVIDER_KEY);
        }
        return result;
    }

    protected abstract Bundle getBundleAdaptor(Module module);

    @Override
    public <T> T putAttachment(AttachmentKey<T> key, T value) {
        return attachments.putAttachment(key, value);
    }

    @Override
    public <T> T getAttachment(AttachmentKey<T> key) {
        return attachments.getAttachment(key);
    }

    @Override
    public <T> T removeAttachment(AttachmentKey<T> key) {
        return attachments.removeAttachment(key);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        assertNotUninstalled();
        return classLoader.loadClass(className);
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return headers;
    }

    @Override
    public URL getEntry(String path) {
        ModuleEntriesProvider entriesProvider = adapt(ModuleEntriesProvider.class);
        return entriesProvider != null ? entriesProvider.getEntry(path) : null;
    }

    @Override
    public List<String> getEntryPaths(String path) {
        ModuleEntriesProvider entriesProvider = adapt(ModuleEntriesProvider.class);
        return entriesProvider != null ? entriesProvider.getEntryPaths(path) : Collections.<String>emptyList();
    }

    @Override
    public List<URL> findEntries(String path, String filePattern, boolean recurse) {
        ModuleEntriesProvider entriesProvider = adapt(ModuleEntriesProvider.class);
        return entriesProvider != null ? entriesProvider.findEntries(path, filePattern, recurse) : Collections.<URL>emptyList();
    }

    public Set<ServiceReference<?>> getServicesInUseInternal() {
        return Collections.unmodifiableSet(usedServices.keySet());
    }

    public void addServiceInUse(ServiceReference<?> serviceState) {
        LOGGER.trace("Add service in use {} to: {}", serviceState, this);
        usedServices.putIfAbsent(serviceState, new AtomicInteger());
        AtomicInteger count = usedServices.get(serviceState);
        count.incrementAndGet();
    }

    public int removeServiceInUse(ServiceReference<?> serviceState) {
        LOGGER.trace("Remove service in use {} from: {}", serviceState, this);
        AtomicInteger count = usedServices.get(serviceState);
        if (count == null)
            return -1;

        int countVal = count.decrementAndGet();
        if (countVal == 0)
            usedServices.remove(serviceState);

        return countVal;
    }

    protected void assertNotUninstalled() {
        if (getState() == State.UNINSTALLED)
            throw new IllegalStateException("Module already uninstalled: " + this);
    }

    @Override
    public String toString() {
        return "Module[" + getIdentity() + "]";
    }
}

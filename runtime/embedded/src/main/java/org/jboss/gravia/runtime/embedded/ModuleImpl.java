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
package org.jboss.gravia.runtime.embedded;

import static org.jboss.gravia.runtime.embedded.EmbeddedRuntime.LOGGER;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Manifest;

import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.resource.AttachmentKey;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.ManifestResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.spi.AttachableSupport;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleActivator;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleEvent;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.embedded.osgi.BundleLifecycleHandler;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;

/**
 * [TODO]
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
final class ModuleImpl implements Module {

    private static AttachmentKey<ModuleActivator> MODULE_ACTIVATOR_KEY = AttachmentKey.create(ModuleActivator.class);
    private static final AtomicLong moduleIdGenerator = new AtomicLong();
    private static final Long START_STOP_TIMEOUT = new Long(10000);

    private final EmbeddedRuntime runtime;
    private final ClassLoader classLoader;
    private final Resource resource;
    private final AtomicReference<State> stateRef = new AtomicReference<State>();
    private final AtomicReference<ModuleContext> contextRef = new AtomicReference<ModuleContext>();
    private final ReentrantLock startStopLock = new ReentrantLock();
    private final Attachable attachments = new AttachableSupport();
    private final long moduleId;

    ModuleImpl(EmbeddedRuntime runtime, ClassLoader classLoader, Manifest manifest) {
        this(runtime, classLoader, buildResource(manifest));
        putAttachment(MANIFEST_KEY, manifest);
    }

    ModuleImpl(EmbeddedRuntime runtime, ClassLoader classLoader, Resource resource) {
        this.runtime = runtime;
        this.classLoader = classLoader;
        this.resource = resource;
        this.moduleId = moduleIdGenerator.incrementAndGet();
        this.stateRef.set(State.UNINSTALLED);
    }

    private static Resource buildResource(Manifest manifest) {
        Resource resource;
        if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
            OSGiMetaData metaData = OSGiMetaDataBuilder.load(manifest);
            DefaultResourceBuilder builder = new DefaultResourceBuilder();
            builder.addIdentityCapability(metaData.getBundleSymbolicName(), metaData.getBundleVersion().toString());
            resource = builder.getResource();
        } else {
            resource = new ManifestResourceBuilder().load(manifest).getResource();
        }
        return resource;
    }

    // Module API

    @Override
    public long getModuleId() {
        return moduleId;
    }

    @Override
    public ResourceIdentity getIdentity() {
        return resource.getIdentity();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type) {
        A result = null;
        if (type.isAssignableFrom(Runtime.class)) {
            result = (A) runtime;
        } else if (type.isAssignableFrom(ClassLoader.class)) {
            result = (A) classLoader;
        } else if (type.isAssignableFrom(Resource.class)) {
            result = (A) resource;
        } else if (type.isAssignableFrom(Manifest.class)) {
            result = (A) getAttachment(MANIFEST_KEY);
        } else if (type.isAssignableFrom(Module.class)) {
            result = (A) this;
        }
        return result;
    }

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
    public State getState() {
        return stateRef.get();
    }

    void setState(State newState) {
        stateRef.set(newState);
    }

    @Override
    public ModuleContext getModuleContext() {
        return contextRef.get();
    }

    private void createModuleContext() {
        contextRef.set(new ModuleContextImpl(this));
    }

    private void destroyModuleContext() {
        ModuleContextImpl context = (ModuleContextImpl) contextRef.get();
        if (context != null) {
            context.destroy();
        }
        contextRef.set(null);
    }

    @Override
    public void start() throws ModuleException {
        assertNotUninstalled();
        try {
            // #1 If this module is in the process of being activated or deactivated
            // then this method must wait for activation or deactivation to complete
            if (!startStopLock.tryLock(START_STOP_TIMEOUT, TimeUnit.MILLISECONDS))
                throw new ModuleException("Cannot aquire start lock for: " + this);

            // #2 If this module's state is {@code ACTIVE} then this method returns immediately.
            if (getState() == State.ACTIVE) {
                LOGGER.debugf("Already active: %s", this);
                return;
            }

            // #3 This bundle's state is set to {@code STARTING}.
            setState(State.STARTING);

            // #4 A module event of type {@link ModuleEvent#STARTING} is fired.
            RuntimeEventsHandler eventHandler = runtime.adapt(RuntimeEventsHandler.class);
            eventHandler.fireModuleEvent(this, ModuleEvent.STARTING);

            // Create the {@link ModuleContext}
            createModuleContext();

            // #5 The {@link ModuleActivator#start(ModuleContext)} method if one is specified, is called.
            try {
                if (BundleLifecycleHandler.isBundle(this)) {
                    BundleLifecycleHandler.start(this);
                } else {
                    String className = getModuleActivatorClassName();
                    if (className != null) {
                        ModuleActivator moduleActivator;
                        synchronized (MODULE_ACTIVATOR_KEY) {
                            moduleActivator = attachments.getAttachment(MODULE_ACTIVATOR_KEY);
                            if (moduleActivator == null) {
                                Object result = loadClass(className).newInstance();
                                moduleActivator = (ModuleActivator) result;
                                attachments.putAttachment(MODULE_ACTIVATOR_KEY, moduleActivator);
                            }
                        }
                        if (moduleActivator != null) {
                            moduleActivator.start(getModuleContext());
                        }
                    }
                }
            }

            // If the {@code ModuleActivator} is invalid or throws an exception then:
            catch (Throwable th) {

                // This module's state is set to {@code STOPPING}.
                setState(State.STOPPING);

                // A module event of type {@link BundleEvent#STOPPING} is fired.
                eventHandler.fireModuleEvent(this, ModuleEvent.STARTING);

                // [TODO] Any services registered by this module must be unregistered.
                // [TODO] Any services used by this module must be released.
                // [TODO] Any listeners registered by this module must be removed.

                // This module's state is set to {@code RESOLVED}.
                setState(State.RESOLVED);

                // A module event of type {@link BundleEvent#STOPPED} is fired.
                eventHandler.fireModuleEvent(this, ModuleEvent.STOPPED);

                // Destroy the {@link ModuleContext}
                destroyModuleContext();

                // A {@code ModuleException} is then thrown.
                throw new ModuleException("Cannot start module: " + this, th);
            }

            // #6 This bundle's state is set to {@code ACTIVE}.
            setState(State.ACTIVE);

            // #7 A module event of type {@link ModuleEvent#STARTED} is fired.
            eventHandler.fireModuleEvent(this, ModuleEvent.STARTED);

            LOGGER.infof("Started: %s", this);
        } catch (InterruptedException ex) {
            throw ModuleException.launderThrowable(ex);
        } finally {
            startStopLock.unlock();
        }
    }

    private String getModuleActivatorClassName() {
        Manifest manifest = getAttachment(MANIFEST_KEY);
        return manifest != null ? manifest.getMainAttributes().getValue(ManifestBuilder.GRAVIA_ACTIVATOR) : null;
    }

    @Override
    public void stop() throws ModuleException {
        assertNotUninstalled();
        try {
            // #1 If this module is in the process of being activated or deactivated
            // then this method must wait for activation or deactivation to complete
            if (!startStopLock.tryLock(START_STOP_TIMEOUT, TimeUnit.MILLISECONDS))
                throw new ModuleException("Cannot aquire stop lock for: " + this);

            // #2 If this module's state is not {@code ACTIVE} then this method returns immediately
            if (getState() != State.ACTIVE) {
                return;
            }

            // #3 This module's state is set to {@code STOPPING}
            setState(State.STOPPING);

            // #4 A module event of type {@link ModuleEvent#STOPPING} is fired.
            RuntimeEventsHandler eventHandler = runtime.adapt(RuntimeEventsHandler.class);
            eventHandler.fireModuleEvent(this, ModuleEvent.STOPPING);

            // #5 The {@link ModuleActivator#stop(ModuleContext)} is called
            Throwable stopException = null;
            try {
                if (BundleLifecycleHandler.isBundle(this)) {
                    BundleLifecycleHandler.stop(this);
                } else {
                    ModuleActivator moduleActivator = attachments.getAttachment(MODULE_ACTIVATOR_KEY);
                    if (moduleActivator != null) {
                        moduleActivator.stop(getModuleContext());
                    }
                }
            } catch (Throwable th) {
                stopException = th;
            }

            // #6 [TODO] Any services registered by this module must be unregistered.
            // #7 [TODO] Any services used by this module must be released.
            // #8 [TODO] Any listeners registered by this module must be removed.

            // #9 This module's state is set to {@code RESOLVED}.
            setState(State.RESOLVED);

            // #10 A module event of type {@link ModuleEvent#STOPPED} is fired.
            eventHandler.fireModuleEvent(this, ModuleEvent.STOPPED);

            // Destroy the {@link ModuleContext}
            destroyModuleContext();

            if (stopException != null)
                throw new ModuleException("Cannot stop module: " + this, stopException);

            LOGGER.infof("Stopped: %s", this);

        } catch (InterruptedException ex) {
            throw ModuleException.launderThrowable(ex);
        } finally {
            startStopLock.unlock();
        }
    }

    @Override
    public void uninstall() {
        assertNotUninstalled();

        // #1 This module is stopped as described in the {@code Module.stop} method.
        try {
            stop();
        } catch (Exception ex) {
            LOGGER.errorf(ex, "Cannot stop module on uninstall: %s", this);
        }

        // #2 This bundle's state is set to {@code UNINSTALLED}.
        setState(State.UNINSTALLED);

        // #3 A module event of type {@link ModuleEvent#UNINSTALLED} is fired.
        RuntimeEventsHandler eventHandler = runtime.adapt(RuntimeEventsHandler.class);
        eventHandler.fireModuleEvent(this, ModuleEvent.UNINSTALLED);

        runtime.uninstallModule(this);

        LOGGER.infof("Uninstalled: %s", this);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    private void assertNotUninstalled() {
        if (stateRef.get() == State.UNINSTALLED)
            throw new IllegalStateException("Module already uninstalled: " + this);
    }

    @Override
    public String toString() {
        return "Module[" + getIdentity() + "]";
    }
}

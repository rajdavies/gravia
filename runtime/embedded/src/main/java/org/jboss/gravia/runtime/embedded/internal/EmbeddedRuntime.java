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

import static org.jboss.gravia.runtime.spi.RuntimeLogger.LOGGER;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.spi.AbstractModule;
import org.jboss.gravia.runtime.spi.AbstractRuntime;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.RuntimeEventsManager;
import org.jboss.gravia.runtime.spi.RuntimePlugin;
import org.osgi.service.log.LogService;

/**
 * The embedded runtome implemenation
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public class EmbeddedRuntime extends AbstractRuntime {

    private final RuntimeServicesManager serviceManager;
    private final RuntimeStorageHandler storageHandler;

    public EmbeddedRuntime(PropertiesProvider propertiesProvider, Attachable context) {
        super(propertiesProvider);
        serviceManager = new RuntimeServicesManager(adapt(RuntimeEventsManager.class));
        storageHandler = new RuntimeStorageHandler(propertiesProvider, true);

        // Install system module
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(getSystemIdentity()).getResource();
        try {
            installModule(EmbeddedRuntime.class.getClassLoader(), resource, null, context);
        } catch (ModuleException ex) {
            throw new IllegalStateException("Cannot install system module", ex);
        }
    }

    @Override
    public void init() {

        // Register the LogService
        ModuleContext syscontext = adapt(ModuleContext.class);
        syscontext.registerService(LogService.class.getName(), new EmbeddedLogServiceFactory(), null);

        // Register the MBeanServer service
        MBeanServer mbeanServer = findOrCreateMBeanServer();
        syscontext.registerService(MBeanServer.class, mbeanServer, null);

        // Install the plugin modules
        List<Module> pluginModules = new ArrayList<Module>();
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceLoader<RuntimePlugin> services = ServiceLoader.load(RuntimePlugin.class, EmbeddedRuntime.class.getClassLoader());
        Iterator<RuntimePlugin> iterator = services.iterator();
        while (iterator.hasNext()) {
            RuntimePlugin plugin = iterator.next();
            try {
                Module module = plugin.installPluginModule(this, classLoader);
                pluginModules.add(module);
            } catch (ModuleException ex) {
                LOGGER.error("Cannot load plugin: " + plugin.getClass().getName(), ex);
            }
        }

        // Start the plugin modules
        for (Module module : pluginModules) {
            try {
                module.start();
            } catch (ModuleException ex) {
                LOGGER.error("Cannot start plugin: " + module, ex);
            }
        }
    }

    @Override
    public AbstractModule createModule(ClassLoader classLoader, Resource resource, Dictionary<String, String> headers, Attachable context) {
        AbstractModule module;
        if (resource != null && resource.getIdentity().equals(getSystemIdentity())) {
            module = new SystemModule(this, classLoader, resource);
        } else {
            module = new EmbeddedModule(this, classLoader, resource, headers);
        }
        ModuleEntriesProvider entriesProvider = context.getAttachment(AbstractModule.MODULE_ENTRIES_PROVIDER_KEY);
        if (entriesProvider != null) {
            module.putAttachment(AbstractModule.MODULE_ENTRIES_PROVIDER_KEY, entriesProvider);
        }
        return module;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type) {
        A result = super.adapt(type);
        if (result == null) {
            if (type.isAssignableFrom(RuntimeServicesManager.class)) {
                result = (A) serviceManager;
            } else if (type.isAssignableFrom(RuntimeStorageHandler.class)) {
                result = (A) storageHandler;
            }
        }
        return result;
    }

    @Override
    protected void uninstallModule(Module module) {
        super.uninstallModule(module);
    }

    private MBeanServer findOrCreateMBeanServer() {
        MBeanServer mbeanServer = null;

        ArrayList<MBeanServer> serverArr = MBeanServerFactory.findMBeanServer(null);
        if (serverArr.size() > 1)
            LOGGER.warn("Multiple MBeanServer instances: {}", serverArr);

        if (serverArr.size() > 0) {
            mbeanServer = serverArr.get(0);
            LOGGER.debug("Found MBeanServer: {}", mbeanServer.getDefaultDomain());
        }

        if (mbeanServer == null) {
            LOGGER.debug("No MBeanServer, create one ...");
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        return mbeanServer;
    }
}

/*
 * #%L
 * JBossOSGi SPI
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
package org.jboss.test.gravia.runtime.embedded;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.jboss.gravia.runtime.Constants;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.PropertiesProvider;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.embedded.EmbeddedRuntime;
import org.junit.Before;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * [TODO].
 *
 * @author thomas.diesler@jbos.com
 * @since 27-Sep-2013
 */
public abstract class AbstractRuntimeTest {

    private Runtime runtime;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RUNTIME_STORAGE, new File("target/runtime").getAbsolutePath());
        props.put(Constants.RUNTIME_STORAGE_CLEAN, Constants.RUNTIME_STORAGE_CLEAN_ONFIRSTINIT);
        PropertiesProvider propertiesProvider = new PropertiesProvider() {

            @Override
            public Object getProperty(String key, Object defaultValue) {
                Object value = props.get(key);
                return value != null ? value : defaultValue;
            }

            @Override
            public Object getProperty(String key) {
                return getProperty(key, null);
            }
        };
        runtime = new EmbeddedRuntime(propertiesProvider);
        RuntimeLocator.setRuntime(runtime);
    }

    Runtime getRuntime() {
        return runtime;
    }

    ConfigurationAdmin getConfigurationAdmin(Module module) {
        ModuleContext context = module.getModuleContext();
        return context.getService(context.getServiceReference(ConfigurationAdmin.class));
    }

    void installInternalBundles(String... names) throws Exception {
        List<Module> modules = new ArrayList<Module>();
        for (String name : names) {
            modules.add(installInternalBundle(name));
        }
        for (Module module : modules) {
            module.start();
        }
    }

    Module installInternalBundle(String symbolicName) throws Exception {
        JarFile jarFile = new JarFile("target/test-libs/bundles/" + symbolicName + ".jar");
        return runtime.installModule(getClass().getClassLoader(), jarFile.getManifest());
    }
}
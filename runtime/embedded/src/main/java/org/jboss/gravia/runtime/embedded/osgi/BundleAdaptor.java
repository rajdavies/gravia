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
package org.jboss.gravia.runtime.embedded.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.spi.AbstractRuntime;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.osgi.metadata.CaseInsensitiveDictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Bundle implementation that delegates all functionality to
 * the underlying Module.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public final class BundleAdaptor implements Bundle {

    private final Module module;

    public BundleAdaptor(Module module) {
        this.module = module;
    }

    @Override
    public long getBundleId() {
        return module.getModuleId();
    }


    @Override
    public String getSymbolicName() {
        return module.getIdentity().getSymbolicName();
    }

    @Override
    public Version getVersion() {
        String version = module.getIdentity().getVersion().toString();
        return  Version.parseVersion(version);
    }

    @Override
    public String getLocation() {
        return module.getIdentity().toString();
    }

    @Override
    public int getState() {
        switch (module.getState()) {
            case INSTALLED:
                return Bundle.INSTALLED;
            case RESOLVED:
                return Bundle.RESOLVED;
            case STARTING:
                return Bundle.STARTING;
            case ACTIVE:
                return Bundle.ACTIVE;
            case STOPPING:
                return Bundle.STOPPING;
            case UNINSTALLED:
                return Bundle.UNINSTALLED;
        }
        return Bundle.UNINSTALLED;
    }

    @Override
    public BundleContext getBundleContext() {
        ModuleContext context = module.getModuleContext();
        return context != null ? new BundleContextAdaptor(context) : null;
    }

    @Override
    public <A> A adapt(Class<A> type) {
        return module.adapt(type);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return module.loadClass(className);
    }

    @Override
    public URL getResource(String name) {
        ClassLoader classLoader = module.adapt(ClassLoader.class);
        return classLoader.getResource(name);
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return getHeaders(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Dictionary<String, String> getHeaders(String locale) {

        // Get the raw (unlocalized) manifest headers
        Dictionary<String, String> rawHeaders = module.getHeaders();

        // If the specified locale is the empty string, this method will return the
        // raw (unlocalized) manifest headers including any leading "%"
        if ("".equals(locale))
            return rawHeaders;

        Dictionary<String, String> result = new Hashtable<String, String>();
        Enumeration<String> e = rawHeaders.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String value = rawHeaders.get(key);
            if (value.startsWith("%"))
                value = value.substring(1);

            result.put(key, value);
        }

        return new CaseInsensitiveDictionary(result);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        ClassLoader classLoader = module.adapt(ClassLoader.class);
        return classLoader.getResources(name);
    }

    @Override
    public URL getEntry(String path) {
        ModuleEntriesProvider entriesProvider = getModuleEntriesProvider();
        return entriesProvider.getEntry(path);
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        ModuleEntriesProvider entriesProvider = getModuleEntriesProvider();
        return entriesProvider.getEntryPaths(path);
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
        ModuleEntriesProvider entriesProvider = getModuleEntriesProvider();
        return entriesProvider.findEntries(path, filePattern, recurse);
    }

    @Override
    public int compareTo(Bundle bundle) {
        throw new UnsupportedOperationException("Bundle.compareTo(Bundle)");
    }

    @Override
    public void start(int options) throws BundleException {
        throw new UnsupportedOperationException("Bundle.start(int)");
    }

    @Override
    public void start() throws BundleException {
        throw new UnsupportedOperationException("Bundle.start()");
    }

    @Override
    public void stop(int options) throws BundleException {
        throw new UnsupportedOperationException("Bundle.stop(int)");
    }

    @Override
    public void stop() throws BundleException {
        throw new UnsupportedOperationException("Bundle.stop()");
    }

    @Override
    public void update(InputStream input) throws BundleException {
        throw new UnsupportedOperationException("Bundle.update(InputStream)");
    }

    @Override
    public void update() throws BundleException {
        throw new UnsupportedOperationException("Bundle.update()");
    }

    @Override
    public void uninstall() throws BundleException {
        throw new UnsupportedOperationException("Bundle.uninstall()");
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        throw new UnsupportedOperationException("Bundle.getRegisteredServices()");
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        throw new UnsupportedOperationException("Bundle.getServicesInUse()");
    }

    @Override
    public long getLastModified() {
        throw new UnsupportedOperationException("Bundle.getLastModified()");
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
        throw new UnsupportedOperationException("Bundle.getSignerCertificates(int)");
    }

    @Override
    public File getDataFile(String filename) {
        return module.getDataFile(filename);
    }

    @Override
    public boolean hasPermission(Object permission) {
        throw new UnsupportedOperationException("Bundle.hasPermission(Object)");
    }

    private ModuleEntriesProvider getModuleEntriesProvider() {
        AbstractRuntime runtime = (AbstractRuntime) module.adapt(Runtime.class);
        return runtime.getModuleEntriesProvider(module);
    }

    @Override
    public String toString() {
        return "Bundle[" + module.getIdentity() + "]";
    }
}

/*
 * #%L
 * JBossOSGi Runtime
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
package org.jboss.gravia.runtime.util;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.jboss.gravia.utils.NotNullException;

/**
 * Provides Moduel headers from a manifest
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 *
 * @ThreadSafe
 */
public final class ManifestHeadersProvider {

    private final Manifest manifest;

    public ManifestHeadersProvider(Manifest manifest) {
        NotNullException.assertValue(manifest, "manifest");
        this.manifest = manifest;
    }

    /**
     * Return a mutable dictionary of manifest headers
     */
    public Dictionary<String, String> getHeaders() {
        Hashtable<String, String> headers = new Hashtable<String, String>();
        Attributes mainAttributes = manifest.getMainAttributes();
        for (Object key : mainAttributes.keySet()) {
            Name name = (Name) key;
            String value = mainAttributes.getValue(name);
            headers.put(name.toString(), value);
        }
        return headers;
    }
}

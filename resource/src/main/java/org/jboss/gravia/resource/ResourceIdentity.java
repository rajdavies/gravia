/*
 * #%L
 * Gravia Resource
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
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
package org.jboss.gravia.resource;

import org.jboss.gravia.utils.NotNullException;


/**
 * A resource identity.
 *
 * A resource is identified by its symbolic name and {@link Version}
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2013
 */
public final class ResourceIdentity {

    private final String symbolicName;
    private final Version version;
    private final String canonicalName;

    public static ResourceIdentity create(String symbolicName, String version) {
        return new ResourceIdentity(symbolicName, version != null ? Version.parseVersion(version) : null);
    }

    public static ResourceIdentity create(String symbolicName, Version version) {
        return new ResourceIdentity(symbolicName, version);
    }

    public static ResourceIdentity fromString(String identity) {
        int index = identity.indexOf(':');
        String namePart = index > 0 ? identity.substring(0, index) : identity;
        String versionPart = index > 0 ? identity.substring(index + 1) : "0.0.0";
        return new ResourceIdentity(namePart, Version.parseVersion(versionPart));
    }

    private ResourceIdentity(String symbolicName, Version version) {
        NotNullException.assertValue(symbolicName, "symbolicName");
        this.symbolicName = symbolicName;
        this.version = version != null ? version : Version.emptyVersion;
        this.canonicalName = symbolicName + ":" + version;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceIdentity))
            return false;
        if (obj == this)
            return true;
        ResourceIdentity other = (ResourceIdentity) obj;
        return canonicalName.equals(other.canonicalName);
    }

    @Override
    public int hashCode() {
        return canonicalName.hashCode();
    }

    @Override
    public String toString() {
        return canonicalName;
    }
}

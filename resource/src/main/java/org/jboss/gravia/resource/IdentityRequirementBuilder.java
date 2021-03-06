/*
 * #%L
 * JBossOSGi Resolver API
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

import java.util.Map;



/**
 * An identity {@link Requirement} builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class IdentityRequirementBuilder extends DefaultRequirementBuilder {

    public IdentityRequirementBuilder(String symbolicName, String range) {
        this(symbolicName, range != null ? new VersionRange(range) : null, null, null);
    }

    public IdentityRequirementBuilder(ResourceIdentity identity) {
        this(identity.getSymbolicName(), identity.getVersion().toString());
    }

    public IdentityRequirementBuilder(String symbolicName, VersionRange range) {
        this(symbolicName, range, null, null);
    }

    public IdentityRequirementBuilder(String symbolicName, VersionRange range, Map<String, Object> atts, Map<String, String> dirs) {
        super(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        if (range != null) {
            getAttributes().put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, range);
        }
        if (atts != null) {
            getAttributes().putAll(atts);
        }
        if (dirs != null) {
            getDirectives().putAll(dirs);
        }
    }
}

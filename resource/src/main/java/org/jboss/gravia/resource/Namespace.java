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

/**
 * General namespace constants.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
public interface Namespace {

    /**
     * The capability directive used to specify the effective time for the
     * capability. The default value is {@link #EFFECTIVE_RESOLVE resolve}.
     *
     * @see #EFFECTIVE_RESOLVE resolve
     */
    String  CAPABILITY_EFFECTIVE_DIRECTIVE      = "effective";

    /**
     * The requirement directive used to specify the resolution type for a
     * requirement. The default value is {@link #RESOLUTION_MANDATORY mandatory}
     * .
     *
     * @see #RESOLUTION_MANDATORY mandatory
     * @see #RESOLUTION_OPTIONAL optional
     */
    String  REQUIREMENT_RESOLUTION_DIRECTIVE    = "resolution";

    /**
     * The directive value identifying a mandatory requirement resolution type.
     * A mandatory resolution type indicates that the requirement must be
     * resolved when the resource is resolved. If such a requirement cannot be
     * resolved, the resource fails to resolve.
     *
     * @see #REQUIREMENT_RESOLUTION_DIRECTIVE
     */
    String  RESOLUTION_MANDATORY                = "mandatory";

    /**
     * The directive value identifying an optional requirement resolution type.
     * An optional resolution type indicates that the requirement is optional
     * and the resource may be resolved without the requirement being resolved.
     *
     * @see #REQUIREMENT_RESOLUTION_DIRECTIVE
     */
    String  RESOLUTION_OPTIONAL                 = "optional";

    /**
     * The requirement directive used to specify the effective time for the
     * requirement. The default value is {@link #EFFECTIVE_RESOLVE resolve}.
     *
     * @see #EFFECTIVE_RESOLVE resolve
     */
    String  REQUIREMENT_EFFECTIVE_DIRECTIVE     = "effective";

    /**
     * The directive value identifying a {@link #CAPABILITY_EFFECTIVE_DIRECTIVE
     * capability} or {@link #REQUIREMENT_EFFECTIVE_DIRECTIVE requirement} that
     * is effective at resolve time. Capabilities and requirements with an
     * effective time of resolve are the only capabilities which are processed
     * while resolving a resource.
     *
     * @see #REQUIREMENT_EFFECTIVE_DIRECTIVE
     * @see #CAPABILITY_EFFECTIVE_DIRECTIVE
     */
    String  EFFECTIVE_RESOLVE                   = "resolve";
}

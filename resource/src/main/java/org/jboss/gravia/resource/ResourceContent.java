/*
 * #%L
 * Gravia Repository
 * %%
 * Copyright (C) 2012 - 2013 JBoss by Red Hat
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

import java.io.InputStream;

/**
 * An accessor for the default content of a resource.
 *
 * A {@link Resource} may be adaptable to this type.
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2012
 */
public interface ResourceContent {

    /**
     * Returns a new input stream to the default content of this resource.
     * @return An input stream for the default content or null if the resource does not contain a content capability
     */
    InputStream getContent();
}

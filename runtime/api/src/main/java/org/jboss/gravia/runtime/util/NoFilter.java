package org.jboss.gravia.runtime.util;
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

import java.util.Dictionary;
import java.util.Map;

import org.jboss.gravia.runtime.Filter;
import org.jboss.gravia.runtime.ServiceReference;

/**
 * A dummy filter that matches everything.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public final class NoFilter implements Filter {

    /** Singleton instance */
    public static final Filter INSTANCE = new NoFilter();

    private NoFilter() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean match(Dictionary dictionary) {
        return true;
    }

    @Override
    public boolean match(ServiceReference<?> reference) {
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean matchCase(Dictionary dictionary) {
        return true;
    }

    @Override
    public boolean matches(Map<String, ?> map) {
        return true;
    }
}

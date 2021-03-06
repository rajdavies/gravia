/*
 * #%L
 * JBossOSGi Repository
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
package org.jboss.gravia.repository;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.gravia.repository.spi.AbstractRepositoryXMLReader;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.ResourceBuilder;


/**
 * Read repository contnet from XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-May-2012
 */
public class DefaultRepositoryXMLReader extends AbstractRepositoryXMLReader {

    public DefaultRepositoryXMLReader(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    protected XMLStreamReader createXMLStreamReader(InputStream inputStream) {
        try {
            return XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot initialize repository reader", ex);
        }
    }

    @Override
    protected ResourceBuilder createResourceBuilder() {
        return new DefaultResourceBuilder();
    }
}

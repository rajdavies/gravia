/*
 * #%L
 * JBossOSGi Repository: API
 * %%
 * Copyright (C) 2011 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.gravia.repository;

import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.gravia.repository.spi.AbstractRepositoryXMLWriter;


/**
 * Write repository contnet to XML.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-May-2012
 */
public class DefaultRepositoryXMLWriter extends AbstractRepositoryXMLWriter {


    public DefaultRepositoryXMLWriter(OutputStream output) {
        super(output);
    }

    @Override
    protected XMLStreamWriter createXMLStreamWriter(OutputStream outputSteam) {
        try {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(outputSteam);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot initialize repository writer", ex);
        }
    }

}

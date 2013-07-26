/*
 * #%L
 * JBossOSGi Provision: Core
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
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
package org.jboss.gravia.provision;

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;

/**
 * The {@link Provisioner}
 *
 * @author thomas.diesler@jboss.com
 * @since 06-May-2013
 */
public interface Provisioner {

    Resolver getResolver();

    Repository getRepository();

    ProvisionResult findResources(Set<Requirement> reqs);

    ProvisionResult findResources(Environment env, Set<Requirement> reqs);

    ResourceHandle installResource(Resource resource, Map<Requirement, Resource> mapping) throws ProvisionException;

    interface ResourceHandle {

        Resource getResource();

        <T> T adapt(Class<T> type);

        void uninstall() throws ProvisionException;
    }
}
/*
 * #%L
 * Wildfly Gravia Subsystem
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


package org.wildfly.extension.gravia.service;

import java.util.Iterator;
import java.util.Set;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.provision.DefaultEnvironment;
import org.jboss.gravia.provision.DefaultProvisioner;
import org.jboss.gravia.provision.Environment;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service providing the {@link Provisioner}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jun-2013
 */
public class ProvisionerService extends AbstractService<Provisioner> {

    static final Logger LOGGER = LoggerFactory.getLogger(GraviaConstants.class.getPackage().getName());

    private final InjectedValue<ModuleContext> injectedModuleContext = new InjectedValue<ModuleContext>();
    private final InjectedValue<Environment> injectedEnvironment = new InjectedValue<Environment>();
    private final InjectedValue<Repository> injectedRepository = new InjectedValue<Repository>();
    private final InjectedValue<Resolver> injectedResolver = new InjectedValue<Resolver>();
    private final InjectedValue<ResourceInstaller> injectedResourceInstaller = new InjectedValue<ResourceInstaller>();
    private ServiceRegistration<Provisioner> registration;
    private Provisioner provisioner;

    public ServiceController<Provisioner> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceBuilder<Provisioner> builder = serviceTarget.addService(GraviaConstants.PROVISIONER_SERVICE_NAME, this);
        builder.addDependency(GraviaConstants.ENVIRONMENT_SERVICE_NAME, Environment.class, injectedEnvironment);
        builder.addDependency(GraviaConstants.MODULE_CONTEXT_SERVICE_NAME, ModuleContext.class, injectedModuleContext);
        builder.addDependency(GraviaConstants.REPOSITORY_SERVICE_NAME, Repository.class, injectedRepository);
        builder.addDependency(GraviaConstants.RESOLVER_SERVICE_NAME, Resolver.class, injectedResolver);
        builder.addDependency(GraviaConstants.RESOURCE_INSTALLER_SERVICE_NAME, ResourceInstaller.class, injectedResourceInstaller);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        Resolver resolver = injectedResolver.getValue();
        Repository repository = injectedRepository.getValue();
        Environment environment = injectedEnvironment.getValue();
        provisioner = new DefaultProvisioner(environment, resolver, repository) {
            @Override
            public ResourceInstaller getResourceInstaller() {
                return injectedResourceInstaller.getValue();
            }

            @Override
            protected Environment cloneEnvironment(Environment env) {
                Environment clone = new DefaultEnvironment("Cloned " + env.getName()) {
                    @Override
                    public Set<Capability> findProviders(Requirement req) {
                        Set<Capability> providers = super.findProviders(req);
                        if (providers.isEmpty() && req.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE)) {
                            providers = EnvironmentService.findModuleProviders(req);
                        }
                        return providers;
                    }

                    @Override
                    public Resource getResource(ResourceIdentity resid) {
                        Resource resource = super.getResource(resid);
                        if (resource == null) {
                            resource = EnvironmentService.getModuleResource(resid);
                        }
                        return resource;
                    }
                };
                Iterator<Resource> itres = env.getResources();
                while (itres.hasNext()) {
                    clone.addResource(itres.next());
                }
                return clone;
            }
        };

        // Register the provisioner as a service
        ModuleContext syscontext = injectedModuleContext.getValue();
        registration = syscontext.registerService(Provisioner.class, provisioner, null);
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public Provisioner getValue() throws IllegalStateException {
        return provisioner;
    }
}

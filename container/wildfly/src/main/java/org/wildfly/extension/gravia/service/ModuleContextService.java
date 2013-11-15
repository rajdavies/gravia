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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service providing the system {@link ModuleContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Nov-2013
 */
public class ModuleContextService extends AbstractService<ModuleContext> {

    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private ModuleContext syscontext;

    public static ServiceController<ModuleContext> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ModuleContextService service = new ModuleContextService();
        ServiceBuilder<ModuleContext> builder = serviceTarget.addService(GraviaConstants.MODULE_CONTEXT_SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private ModuleContextService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        syscontext = injectedRuntime.getValue().getModule(0).getModuleContext();
    }

    @Override
    public void stop(StopContext context) {
        syscontext = null;
    }

    @Override
    public ModuleContext getValue() throws IllegalStateException {
        return syscontext;
    }
}
/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

/**
 * Instantiates default instances of {@link IProfileRegistry}.
 */
public class ProfileRegistryComponent implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		IAgentLocation location = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(agent, SimpleProfileRegistry.getDefaultRegistryDirectory(location));
		registry.setEventBus((IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME));
		return registry;
	}
}

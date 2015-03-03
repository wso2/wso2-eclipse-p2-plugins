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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.engine.IEngine;

/**
 * Component that provides a factory that can create and initialize
 * {@link IEngine} instances.
 */
public class EngineComponent implements IAgentServiceFactory {

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.spi.IAgentServiceFactory#createService(org.eclipse.equinox.p2.core.IProvisioningAgent)
	 */
	public Object createService(IProvisioningAgent agent) {
		//ensure there is a garbage collector created for this agent if available
		agent.getService("org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector"); //$NON-NLS-1$
		//various parts of the engine may need an open-ended set of services, so we pass the agent to the engine directly
		return new Engine(agent);
	}
}

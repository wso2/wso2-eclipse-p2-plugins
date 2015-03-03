/*******************************************************************************
 * Copyright (c) 2012 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A preference implementation that stores preferences in the engine's profile
 * data area. There is one preference file per profile, with an additional file
 * that is used when there is no currently running profile.
 */
public class SharedProfilePreferences extends ProfilePreferences {
	public SharedProfilePreferences() {
		this(null, null);
	}

	public SharedProfilePreferences(EclipsePreferences nodeParent, String nodeName) {
		super(nodeParent, nodeName);

		//path is /profile/shared/{agent location}/{profile id}/qualifier

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);

		if (segmentCount <= 3)
			return;

		if (segmentCount == 4)
			profileLock = new Object();

		if (segmentCount < 5)
			return;
		// cache the qualifier
		qualifier = getQualifierSegment();
	}

	protected IProvisioningAgent getAgent(String segment) throws BackingStoreException {
		IProvisioningAgent agent = super.getAgent(segment);
		return (IProvisioningAgent) agent.getService(IProvisioningAgent.SHARED_BASE_AGENT);
	}

	protected void doSave(IProvisioningAgent agent) throws BackingStoreException {
		throw new BackingStoreException("Can't store in shared install"); //$NON-NLS-1$
	}

	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new SharedProfilePreferences(nodeParent, nodeName);
	}

	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null)
				return null;
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 5; i < segmentCount; i++)
				node = (EclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	protected synchronized void save() throws BackingStoreException {
		throw new BackingStoreException("Can't store in shared install");
	}

	protected String getQualifierSegment() {
		return getSegment(absolutePath(), 4);
	}

	protected String getProfileIdSegment() {
		return getSegment(absolutePath(), 3);
	}

	protected String getAgentLocationSegment() {
		return getSegment(absolutePath(), 2);
	}
}

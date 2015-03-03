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
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.equinox.internal.p2.engine.SlashEncode;
import org.eclipse.equinox.p2.core.IAgentLocation;

/**
 * A profile scope contains the preferences associated with a particular profile
 * in a provisioned system.
 * @see IProfile
 * @since 2.0
 */
public final class ProfileScope implements IScopeContext {

	/**
	 * String constant (value of <code>"profile"</code>) used for the 
	 * scope name for this preference scope.
	 */
	public static final String SCOPE = "profile"; //$NON-NLS-1$

	private String profileId;

	private IAgentLocation location;

	/**
	 * Creates and returns a profile scope for the given profile id and agent.
	 * @param agentLocation The location of the provisioning agent to obtain profile preferences for
	 * @param profileId The id of the profile to obtain preferences for
	 */
	public ProfileScope(IAgentLocation agentLocation, String profileId) {
		super();
		Assert.isNotNull(agentLocation);
		Assert.isNotNull(profileId);
		this.profileId = profileId;
		this.location = agentLocation;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IScopeContext#getLocation()
	 */
	public IPath getLocation() {
		// Null returned as the location should only be used when the profile is locked
		return null;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IScopeContext#getName()
	 */
	public String getName() {
		return SCOPE;
	}

	/*
	 * Default path hierarchy for profile nodes is /profile/<profileId>/<qualifier>.
	 * 
	 * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
	 */
	public IEclipsePreferences getNode(String qualifier) {
		if (qualifier == null)
			throw new IllegalArgumentException();
		String locationString = SlashEncode.encode(location.getRootLocation().toString());
		//format is /profile/{agentLocationURI}/{profileId}/qualifier
		return (IEclipsePreferences) PreferencesService.getDefault().getRootNode().node(getName()).node(locationString).node(profileId).node(qualifier);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProfileScope))
			return false;
		ProfileScope other = (ProfileScope) obj;
		return profileId.equals(other.profileId);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() + profileId.hashCode();
	}
}

/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - reading preferences from base in shared install
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.util.*;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.osgi.framework.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A preference implementation that stores preferences in the engine's profile
 * data area. There is one preference file per profile, with an additional file
 * that is used when there is no currently running profile.
 */
public class ProfilePreferences extends EclipsePreferences {
	private class SaveJob extends Job {
		IProvisioningAgent agent;

		SaveJob(IProvisioningAgent agent) {
			super(Messages.ProfilePreferences_saving);
			setSystem(true);
			this.agent = agent;
		}

		public boolean belongsTo(Object family) {
			return family == PROFILE_SAVE_JOB_FAMILY;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				doSave(agent);
			} catch (IllegalStateException e) {
				if (Tracing.DEBUG_PROFILE_PREFERENCES) {
					Tracing.debug("Attempt to save preferences after agent has been stopped"); //$NON-NLS-1$
					e.printStackTrace();
				}
				//ignore - this means the provisioning agent has already been stopped, and since
				//this job is joined during agent stop, it means this job has been scheduled after the 
				//agent stopped and therefore can't have any interesting changes to save
			} catch (BackingStoreException e) {
				LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "Exception saving profile preferences", e)); //$NON-NLS-1$
			} catch (RuntimeException e) {
				LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "Exception saving profile preferences", e)); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		}
	}

	// cache which nodes have been loaded from disk
	private static Set<String> loadedNodes = Collections.synchronizedSet(new HashSet<String>());

	public static final Object PROFILE_SAVE_JOB_FAMILY = new Object();

	private static final long SAVE_SCHEDULE_DELAY = 500;

	//private IPath location;
	protected IEclipsePreferences loadLevel;
	protected Object profileLock;
	protected String qualifier;

	private SaveJob saveJob;
	protected int segmentCount;

	public ProfilePreferences() {
		this(null, null);
	}

	public ProfilePreferences(EclipsePreferences nodeParent, String nodeName) {
		super(nodeParent, nodeName);

		//path is /profile/{agent location}/{profile id}/qualifier

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);

		if (segmentCount <= 2)
			return;

		if (segmentCount == 3)
			profileLock = new Object();

		if (segmentCount < 4)
			return;
		// cache the qualifier
		qualifier = getQualifierSegment();
	}

	private boolean containsProfile(IProfileRegistry profileRegistry, String profileId) {
		if (profileId == null || profileRegistry == null)
			return false;
		return profileRegistry.containsProfile(profileId);
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to save profile preferences
	 */
	protected void doSave(IProvisioningAgent agent) throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			String profileId = getProfileIdSegment();
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			//can't save anything without a profile registry
			if (registry == null)
				return;
			if (!containsProfile(registry, profileId)) {
				//use the default location for the self profile, otherwise just do nothing and return
				if (IProfileRegistry.SELF.equals(profileId)) {
					IPath location = getDefaultLocation(agent);
					if (location != null) {
						super.save(location);
						return;
					}
				}
				if (Tracing.DEBUG_PROFILE_PREFERENCES)
					Tracing.debug("Not saving preferences since there is no file for node: " + absolutePath()); //$NON-NLS-1$
				return;
			}
			super.save(getProfileLocation(registry, profileId));
		}
	}

	/**
	 * Returns a reference to the agent service corresponding to the given encoded
	 * agent location. Never returns null; throws an exception if the agent could not be found.
	 */
	protected IProvisioningAgent getAgent(String segment) throws BackingStoreException {
		String locationString = SlashEncode.decode(segment);
		Exception failure = null;
		IProvisioningAgent result = null;
		try {
			String filter = "(locationURI=" + encodeForFilter(locationString) + ')'; //$NON-NLS-1$
			final BundleContext context = EngineActivator.getContext();
			if (context != null) {
				Collection<ServiceReference<IProvisioningAgent>> refs = context.getServiceReferences(IProvisioningAgent.class, filter);
				if (!refs.isEmpty()) {
					ServiceReference<IProvisioningAgent> ref = refs.iterator().next();
					result = EngineActivator.getContext().getService(ref);
					EngineActivator.getContext().ungetService(ref);
					return result;
				}
			}
		} catch (InvalidSyntaxException e) {
			failure = e;
		}
		throw new BackingStoreException("Unable to determine provisioning agent from location: " + segment, failure); //$NON-NLS-1$
	}

	/**
	 * Encodes a string so that it is suitable for use as a value for a filter property.
	 * Any reserved filter characters are escaped.
	 */
	private String encodeForFilter(String string) {
		StringBuffer result = new StringBuffer(string.length());
		char[] input = string.toCharArray();
		for (int i = 0; i < input.length; i++) {
			switch (input[i]) {
				case '(' :
				case ')' :
				case '*' :
				case '\\' :
					result.append('\\');
					//fall through
				default :
					result.append(input[i]);
			}
		}
		return result.toString();
	}

	/**
	 * Returns the preference file to use when there is no active profile.
	 */
	private IPath getDefaultLocation(IProvisioningAgent agent) {
		//use engine agent location for preferences if there is no self profile
		IAgentLocation location = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		if (location == null) {
			LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "Agent location service not available", new RuntimeException())); //$NON-NLS-1$
			return null;
		}
		IPath dataArea = new Path(URIUtil.toFile(location.getDataArea(EngineActivator.ID)).getAbsolutePath());
		return computeLocation(dataArea, qualifier);
	}

	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null)
				return null;
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 4; i < segmentCount; i++)
				node = (EclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	/**
	 * Returns the location of the preference file for the given profile.
	 */
	private IPath getProfileLocation(IProfileRegistry registry, String profileId) {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) registry;
		File profileDataDirectory = profileRegistry.getProfileDataDirectory(profileId);
		return computeLocation(new Path(profileDataDirectory.getAbsolutePath()), qualifier);
	}

	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		if (nodeName.equals("shared") && segmentCount == 1) { //$NON-NLS-1$
			return new SharedProfilePreferences(nodeParent, nodeName);
		}
		return new ProfilePreferences(nodeParent, nodeName);
	}

	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.absolutePath());
	}

	protected boolean isAlreadyLoaded(String path) {
		return loadedNodes.contains(path);
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to load profile preferences
	 */
	protected void load() throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			IProvisioningAgent agent = getAgent(getAgentLocationSegment());
			if (agent == null)
				return;
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			String profileId = getProfileIdSegment();
			if (!containsProfile(registry, profileId)) {
				//use the default location for the self profile, otherwise just do nothing and return
				if (IProfileRegistry.SELF.equals(profileId)) {
					IPath location = getDefaultLocation(agent);
					if (location != null) {
						load(location);
						return;
					}
				}
				if (Tracing.DEBUG_PROFILE_PREFERENCES)
					Tracing.debug("Not loading preferences since there is no file for node: " + absolutePath()); //$NON-NLS-1$
				return;
			}
			load(getProfileLocation(registry, profileId));
		}
	}

	protected void loaded() {
		loadedNodes.add(name());
	}

	public void removeNode() throws BackingStoreException {
		super.removeNode();
		loadedNodes.remove(this.absolutePath());
	}

	/**
	 * Schedules the save job. This method is synchronized to protect lazily initialization 
	 * of the save job instance.
	 */
	protected synchronized void save() throws BackingStoreException {
		try {
			IProvisioningAgent agent = getAgent(getAgentLocationSegment());
			if (saveJob == null || saveJob.agent != agent)
				saveJob = new SaveJob(agent);
		} catch (BackingStoreException e) {
			if (Tracing.DEBUG_PROFILE_PREFERENCES)
				e.printStackTrace();
			//get agent has already gone away so we can't save preferences
			//TODO see bug 300450
		}
		//only schedule a save if the engine bundle is still running
		BundleContext context = EngineActivator.getContext();
		if (context == null || saveJob == null)
			return;
		try {
			if (context.getBundle().getState() == Bundle.ACTIVE)
				saveJob.schedule(SAVE_SCHEDULE_DELAY);
		} catch (IllegalStateException e) {
			//bundle has been stopped concurrently, so don't save
		}
	}

	protected String getQualifierSegment() {
		return getSegment(absolutePath(), 3);
	}

	protected String getProfileIdSegment() {
		return getSegment(absolutePath(), 2);
	}

	protected String getAgentLocationSegment() {
		return getSegment(absolutePath(), 1);
	}

}

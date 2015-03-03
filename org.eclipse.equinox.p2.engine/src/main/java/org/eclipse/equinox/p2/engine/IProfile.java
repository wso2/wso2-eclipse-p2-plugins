/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;

/**
 * Represents the state of a profile in a profile registry at a given moment in time.
 * Note this object contains only a snapshot of a particular profile state, and will
 * never be updated if subsequent changes are made to this profile. A client should
 * never retain an {@link IProfile} instance, but rather retain the profile id and obtain
 * the current state of the profile from the profile registry only when required.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IProfile extends IQueryable<IInstallableUnit> {

	/**
	 * Constant used to indicate that an installable unit is not locked in anyway.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_NONE = 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be uninstalled.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UNINSTALL = 1 << 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be updated.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UPDATE = 1 << 1;

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.lock"</code>) for an
	 * integer property indicating how an installable unit is locked in its profile.
	 * The integer is a bit-mask indicating the different locks defined on the installable
	 * unit.  The property should be obtained from a profile using 
	 * IProfile#getInstallableUnitProperty(IInstallableUnit, String).
	 * 
	 * @see #LOCK_UNINSTALL
	 * @see #LOCK_UPDATE
	 * @see #LOCK_NONE
	 */
	public static final String PROP_PROFILE_LOCKED_IU = "org.eclipse.equinox.p2.type.lock"; //$NON-NLS-1$

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.root"</code>) for a
	 * boolean property indicating whether an installable unit should be considered
	 * a root of the install. Typically this means the unit will appear to the end user
	 * as a top-level installed item. The property should be obtained from a profile using 
	 * IProfile#getInstallableUnitProperty(IInstallableUnit, String).
	 * 
	 * @see #LOCK_UNINSTALL
	 * @see #LOCK_UPDATE
	 * @see #LOCK_NONE
	 */
	public static final String PROP_PROFILE_ROOT_IU = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the install folder for the profile.
	 */
	public static final String PROP_INSTALL_FOLDER = "org.eclipse.equinox.p2.installFolder"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the configuration folder for the profile.
	 */
	public static final String PROP_CONFIGURATION_FOLDER = "org.eclipse.equinox.p2.configurationFolder"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the location of the launcher configuration file for the profile.
	 */
	public static final String PROP_LAUNCHER_CONFIGURATION = "org.eclipse.equinox.p2.launcherConfiguration"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the installed language(s) for the profile.
	 */
	public static final String PROP_NL = "org.eclipse.equinox.p2.nl"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible short 
	 * textual description of this profile. May be empty or <code>null</code>, and 
	 * generally will be for non-top level install contexts.
	 */
	public static final String PROP_DESCRIPTION = "org.eclipse.equinox.p2.description"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible name of this profile.
	 * May be empty or <code>null</code>, and generally will be for non-top level
	 * install contexts.
	 */
	public static final String PROP_NAME = "org.eclipse.equinox.p2.name"; //$NON-NLS-1$	
	/**
	 * Profile property constant indicating the list of environments
	 * (e.g., OS, WS, ...) in which a profile can operate. The value of the property
	 * is a comma-delimited string of key/value pairs.
	 */
	public static final String PROP_ENVIRONMENTS = "org.eclipse.equinox.p2.environments"; //$NON-NLS-1$
	/**
	 * Profile property constant for a boolean property indicating if the profiling
	 * is roaming.  A roaming profile is one whose physical install location varies
	 * and is updated whenever it runs.
	 */
	public static final String PROP_ROAMING = "org.eclipse.equinox.p2.roaming"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the bundle pool cache location.
	 */
	public static final String PROP_CACHE = "org.eclipse.equinox.p2.cache"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating a shared read-only bundle pool cache location.
	 */
	public static final String PROP_SHARED_CACHE = "org.eclipse.equinox.p2.cache.shared"; //$NON-NLS-1$

	/**
	 * Profile property constant for a boolean property indicating if update features should
	 * be installed in this profile
	 */
	public static final String PROP_INSTALL_FEATURES = "org.eclipse.update.install.features"; //$NON-NLS-1$

	/**
	  * Profile state meta property key.  Can be used to mark a profile state that should be hidden.
	  * The value of the property is not relevant as the property's existence is enough.  Although <code>true</code>
	  * would be a typical value.
	  * 
	  * @since 2.1
	  */
	public static final String STATE_PROP_HIDDEN = "org.eclipse.equinox.p2.state.hidden"; //$NON-NLS-1$

	/**
	 * Profile state metadata property key used to associate with a profile state a user readable name.
	 * @since 2.1
	 */
	public static final String STATE_PROP_TAG = "org.eclipse.equinox.p2.state.tag"; //$NON-NLS-1$

	/**
	 * Profile state metadata property key used to represent the state of the user profile when running in shared install.
	 * The value for this property could be: {@link IProfile#STATE_SHARED_INSTALL_VALUE_INITIAL}, {@link IProfile#STATE_SHARED_INSTALL_VALUE_BEFOREFLUSH} or {@link IProfile#STATE_SHARED_INSTALL_VALUE_NEW}
	 * @since 2.3
	 */
	public static final String STATE_PROP_SHARED_INSTALL = "org.eclipse.equinox.p2.state.shared"; //$NON-NLS-1$

	/**
	 * Value to represent a user profile the first time it is created.
	 * @since 2.3
	 */
	public static final String STATE_SHARED_INSTALL_VALUE_INITIAL = "initial"; //$NON-NLS-1$
	/**
	 * Value to represent a user profile before it is being flushed because the base had changed.
	 * @since 2.3
	 */
	public static final String STATE_SHARED_INSTALL_VALUE_BEFOREFLUSH = "beforeFlush"; //$NON-NLS-1$
	/**
	 * Value to represent the new user profile created once the base profile has been flushed.
	 * @since 2.3
	 */
	public static final String STATE_SHARED_INSTALL_VALUE_NEW = "new"; //$NON-NLS-1$

	/**
	 * Profile property constant for additional parameters of the downloading stats(e.g., package=jee&os=linux).
	 * @since 2.2 
	 */
	public static final String PROP_STATS_PARAMETERS = "org.eclipse.equinox.p2.stats.parameters"; //$NON-NLS-1$

	/**
	 * Returns the provisioning agent that manages this profile
	 * @return A provisioning agent.
	 */
	public IProvisioningAgent getProvisioningAgent();

	/**
	 * Returns the id of this profile, unique within a given profile registry
	 * @return the profile id
	 */
	public String getProfileId();

	/**
	 * Returns the profile property associated with the given key,
	 * or <code>null</code> if this property is not present
	 * @param key The property kid
	 * @return the property value, or <code>null</code>
	 */
	public String getProperty(String key);

	/**
	 * Returns the profile property associated with the given installable unit.
	 * @param iu the installable unit to return the property for
	 * @param key the property key
	 * @return the property value, or <code>null</code> if no such property is defined
	 */
	public String getInstallableUnitProperty(IInstallableUnit iu, String key);

	/**
	 * Returns an unmodifiable map of all profile properties.
	 * @return a map of all profile properties.
	 */
	public Map<String, String> getProperties();

	/**
	 * Returns an unmodifiable map of all profile properties associated with the given
	 * installable unit in this profile.
	 * @param iu the installable unit to return profile properties for
	 * @return an unmodifiable map of installable unit profile properties
	 */
	public Map<String, String> getInstallableUnitProperties(IInstallableUnit iu);

	/**
	 * Returns a timestamp describing when this profile snapshot was created.
	 * @return A profile timestamp
	 */
	public long getTimestamp();

	/**
	 * Returns the installable units in this profile that match the given query. In a shared
	 * install, this will include both the installable units in the shared base location, and in
	 * the current user's private install area.
	 * @param query
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The installable units that match the given query
	 */
	public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor);

}
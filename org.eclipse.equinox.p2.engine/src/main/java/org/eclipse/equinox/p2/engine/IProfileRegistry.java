/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * This encapsulates the access to the profile registry. 
 * It deals with persistence in a transparent way.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IProfileRegistry {
	/**
	 * A special profile id representing the profile of the currently running system.
	 * This constant can be used when invoking {@link #getProfile(String)} to obtain
	 * the profile of the currently running system. Note that a given profile registry
	 * may not have a defined self profile, for example if the running system doesn't
	 * have a profile, or resides in a different profile registry.
	 */
	public static final String SELF = "_SELF_"; //$NON-NLS-1$
	/**
	 * Service name constant for the profile registry service.
	 */
	public static final String SERVICE_NAME = IProfileRegistry.class.getName();

	/**
	 * Return the profile in the registry that has the given id. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @return the profile or <code>null</code>
	 */
	public IProfile getProfile(String id);

	/**
	 * Return the profile in the registry that has the given id and timestamp. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @param timestamp the profile's timestamp
	 * @return the profile or <code>null</code>
	 */
	public IProfile getProfile(String id, long timestamp);

	/**
	 * Return an array of timestamps in ascending order for the profile id in question. 
	 * If there are none, then return an empty array.
	 * 
	 * @param id the id of the profile to list timestamps for
	 * @return the array of timestamps
	 */
	public long[] listProfileTimestamps(String id);

	/**
	 * Return an array of profiles known to this registry. If there are none, then
	 * return an empty array.
	 * 
	 * @return the array of profiles
	 */
	public IProfile[] getProfiles();

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 * @return the new empty profile
	 */
	public IProfile addProfile(String id) throws ProvisionException;

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @param properties the profile properties
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 * @return the new empty profile
	 */
	public IProfile addProfile(String id, Map<String, String> properties) throws ProvisionException;

	/**
	 * Returns whether this profile registry contains a profile with the given id.
	 * 
	 * @param profileId The id of the profile to search for
	 * @return <code>true</code> if this registry contains a profile with the given id,
	 * and <code>false</code> otherwise.
	 */
	public boolean containsProfile(String profileId);

	/**
	 * Remove the given profile snapshot from this profile registry. This method has no effect
	 * if this registry does not contain a profile with the given id and timestamp.
	 * The current profile cannot be removed using this method. When a particular profile state
	 * is removed from the registry, the corresponding profile state properties for that
	 * particular state are also removed.
	 * 
	 * @param id the profile to remove
	 * @param timestamp the timestamp of the profile to remove 
	 * @throws ProvisionException if the profile with the specified id and timestamp is the current profile.
	 */
	public void removeProfile(String id, long timestamp) throws ProvisionException;

	/**
	 * Remove the given profile from this profile registry.  This method has no effect
	 * if this registry does not contain a profile with the given id. When a profile is removed
	 * from the registry, all of its associated profile state properties are removed as well.
	 * 
	 * @param id the profile to remove
	 */
	public void removeProfile(String id);

	/**
	 * Check if the given profile from this profile registry is up-to-date.
	 * 
	 * @param profile the profile to check
	 * @return boolean  true if the profile is current; false otherwise.
	 */
	public boolean isCurrent(IProfile profile);

	/**
	 * Set properties on a specific profile state.  Overwrite existing properties if present.
	 * 
	 * @param id the identifier of the profile
	 * @param timestamp the timestamp of the profile
	 * @param properties the properties to set on the profile
	 * @return status object indicating success or failure
	 * @throws NullPointerException if either id or properties are <code>null</code> 
	 * @since 2.1
	 */
	public IStatus setProfileStateProperties(String id, long timestamp, Map<String, String> properties);

	/**
	 * Set a specific property on a specific profile state.  Overwrite existing properties if present.
	 * <p>
	 * Use of this method is discouraged if multiple properties will be set on the same state since
	 * the implementation of this method may access the file-system with each call.  Callers should use 
	 * {@link #setProfileStateProperties(String, long, Map)} instead. 
	 * </p>
	 * 
	 * @param id the profile identifier
	 * @param timestamp the timestamp of the profile
	 * @param key the property key to set
	 * @param value the property value to set
	 * @return status object indicating success or failure
	 * @throws NullPointerException if any of id, key or value is <code>null</code>
	 * @since 2.1
	 */
	public IStatus setProfileStateProperty(String id, long timestamp, String key, String value);

	/**
	 * Return all properties for a particular profile state. Both the key and the values are <code>String</code>.
	 * Return an empty map if there was a problem accessing the properties.
	 * <p>
	 * There is no guarantee that all state timestamps returned will still exist in the registry 
	 * since the user could delete profile states from the file system.
	 * </p>
	 * @param id the profile identifier
	 * @param timestamp the profile timestamp
	 * @return a property map of key value pairs.  An empty map if the profile state has no properties or does not exist
	 * @throws NullPointerException if profile id is <code>null</code>.
	 * @since 2.1
	 */
	public Map<String, String> getProfileStateProperties(String id, long timestamp);

	/**
	 * Return a map of profile timestamps to values for all profile states that contain the given property key.
	 * Both the key and value are of type <code>String</code>.
	 * Return an empty map if there was a problem accessing the properties.
	 * <p>
	 * There is no guarantee that all state timestamps returned will still exist in the registry 
	 * since the user could delete profile states from the file system.
	 * </p>
	 * @param id the profile identifier
	 * @param key the property key
	 * @return A map of timestamp and values for the given key.  An empty map if no states define the given key.
	 * @throws NullPointerException if the profile id or key is <code>null</code>.
	 * @since 2.1
	 */
	public Map<String, String> getProfileStateProperties(String id, String key);

	/**
	 * Remove all properties with matching keys from the given profile state.  Non-existent keys are
	 * ignored.  If the state does not exist the method performs a no-op and returns normally. If the keys
	 * parameter is <code>null</code> then remove all properties from the profile state.
	 * 
	 * @param id the profile identifier
	 * @param timestamp the profile timestamp
	 * @param keys the property keys to remove, or <code>null</code>
	 * @return a status object indicating success or failure
	 * @throws NullPointerException if the profile id is <code>null</code>.
	 * @since 2.1
	 */
	public IStatus removeProfileStateProperties(String id, long timestamp, Collection<String> keys);
}

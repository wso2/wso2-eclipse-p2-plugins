/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.query;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * A query matching all the {@link IInstallableUnit}s that are marked visible to the user. 
 * @since 2.0
 */
public class UserVisibleRootQuery extends IUProfilePropertyQuery {

	/**
	 * Creates a new query that will match all installable units marked visible to the user.
	 */
	public UserVisibleRootQuery() {
		super(IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
	}

	/**
	 * Test if the {@link IInstallableUnit}, in the context of a {@link IProfile} is visible to the user 
	 * @param iu the element being tested.
	 * @param profile the context in which the iu is tested
	 * @return <tt>true</tt> if the element is visible to the user.
	 */
	public static boolean isUserVisible(IInstallableUnit iu, IProfile profile) {
		String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
		return Boolean.valueOf(value).booleanValue();
	}
}

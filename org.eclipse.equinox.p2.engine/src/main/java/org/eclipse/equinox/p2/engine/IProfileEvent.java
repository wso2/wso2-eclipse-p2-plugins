/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;

/**
 * An event indicating that a profile has been added, removed, or changed.
 * @see IProvisioningEventBus
 * @since 2.0
 */
public interface IProfileEvent {

	/**
	 * Event constant (value 0) indicating that a profile has been added to a profile registry.
	 */
	public static final int ADDED = 0;
	/**
	 * Event constant (value 1) indicating that a profile has been removed from a profile registry.
	 */
	public static final int REMOVED = 1;
	/**
	 * Event constant (value 0) indicating that a profile has been changed in a profile registry.
	 */
	public static final int CHANGED = 2;

	/**
	 * Returns the reason for the event. The reason will be one of the event constants
	 * {@link #ADDED}, {@link #REMOVED}, or {@link #CHANGED}.
	 * @return the reason for the event
	 */
	public int getReason();

	/**
	 * Returns the id of the profile that changed.
	 * @return the id of the profile that changed
	 */
	public String getProfileId();

}
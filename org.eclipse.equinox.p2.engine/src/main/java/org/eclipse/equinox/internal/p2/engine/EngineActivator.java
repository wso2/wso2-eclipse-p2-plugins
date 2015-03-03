/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EngineActivator implements BundleActivator {
	private static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$

	/**
	 * System property describing the profile registry file format
	 */
	public static final String PROP_PROFILE_FORMAT = "eclipse.p2.profileFormat"; //$NON-NLS-1$

	/**
	 * Value for the PROP_PROFILE_FORMAT system property specifying raw XML file
	 * format (used in p2 until and including 3.5.0 release).
	 */
	public static final String PROFILE_FORMAT_UNCOMPRESSED = "uncompressed"; //$NON-NLS-1$

	/**
	 * System property specifying how the engine should handle unsigned artifacts.
	 * If this property is undefined, the default value is assumed to be "prompt".
	 */
	public static final String PROP_UNSIGNED_POLICY = "eclipse.p2.unsignedPolicy"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should prompt for confirmation
	 * when installing unsigned artifacts.
	 */
	public static final String UNSIGNED_PROMPT = "prompt"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should fail when an attempt
	 * is made to install unsigned artifacts.
	 */
	public static final String UNSIGNED_FAIL = "fail"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should silently allow unsigned
	 * artifacts to be installed.
	 */
	public static final String UNSIGNED_ALLOW = "allow"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext aContext) throws Exception {
		EngineActivator.context = aContext;
	}

	public void stop(BundleContext aContext) throws Exception {
		EngineActivator.context = null;
	}

}

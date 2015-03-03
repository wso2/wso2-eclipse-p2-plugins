/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 2.0
 */
public class InstallableUnitPropertyOperand extends PropertyOperand {
	private final IInstallableUnit iu;

	/**
	 * Creates a new operand that represents replacing a property value associated
	 * with an IU with another.  At least one of the provided property values must be
	 * non-null.
	 * 
	 * @param iu The IInstallableUnit with which the property is associated
	 * @param key The key of the property being modified
	 * @param first The property value being removed, or <code>null</code>
	 * @param second The property value being added, or <code>null</code>
	 */
	public InstallableUnitPropertyOperand(IInstallableUnit iu, String key, Object first, Object second) {
		super(key, first, second);
		//the iu must be specified.
		Assert.isTrue(iu != null);
		this.iu = iu;
	}

	public IInstallableUnit getInstallableUnit() {
		return iu;
	}

	public String toString() {
		return "[IInstallableUnit property for " + iu.toString() + "] " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}

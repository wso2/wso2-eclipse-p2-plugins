/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.runtime.Assert;

/**
 * @since 2.0
 */
public class PropertyOperand extends Operand {
	private final Object first;
	private final Object second;
	private final String key;

	/**
	 * Creates a new operand that represents replacing a property value
	 * with another.  At least one of the provided property values must be
	 * non-null.
	 * 
	 * @param key The key of the property being modified
	 * @param first The property value being removed, or <code>null</code>
	 * @param second The property value being added, or <code>null</code>
	 */
	public PropertyOperand(String key, Object first, Object second) {
		//the operand must specify have a key and have at least one non-null value
		Assert.isTrue(key != null && (first != null || second != null));
		this.first = first;
		this.second = second;
		this.key = key;
	}

	public Object first() {
		return first;
	}

	public Object second() {
		return second;
	}

	public String getKey() {
		return key;
	}

	public String toString() {
		return key + " = " + first + " --> " + second; //$NON-NLS-1$ //$NON-NLS-2$
	}
}

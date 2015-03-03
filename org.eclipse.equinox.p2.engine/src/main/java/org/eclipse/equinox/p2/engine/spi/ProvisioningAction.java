/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Landmark Graphics Corporation - bug 397183
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.spi;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;

/**
 * An action that performs one step of a provisioning operation for a particular
 * {@link Touchpoint}.
 * @since 2.0
 */
public abstract class ProvisioningAction {

	private Memento memento;
	private Touchpoint touchpoint;

	protected Memento getMemento() {
		if (memento == null)
			memento = new Memento();
		return memento;
	}

	/**
	 * Performs the provisioning action.
	 * @param parameters The action parameters
	 * @return A status indicating whether the action was successful
	 */
	public abstract IStatus execute(Map<String, Object> parameters);

	/**
	 * Performs the inverse of this provisioning action. This should reverse
	 * any effects from a previous invocation of the {@link #execute(Map)} method.
	 * @param parameters The action parameters
	 * @return A status indicating whether the action was successful
	 */
	public abstract IStatus undo(Map<String, Object> parameters);

	/**
	 * This method is meant for provisioning actions that need to communicate the result of their execution  
	 * to subsequent actions.
	 * This method is only invoked by p2 once execute() has been executed.  
	 * @return the result of the action execution. 
	 * @since 2.3
	 */
	public Value<?> getResult() {
		return Value.NO_VALUE;
	}

	// TODO: these probably should not be visible
	public void setTouchpoint(Touchpoint touchpoint) {
		this.touchpoint = touchpoint;
	}

	/**
	 * Returns the touchpoint that this action is operating under.
	 * @return the touchpoint
	 */
	public Touchpoint getTouchpoint() {
		return touchpoint;
	}
}

/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 */
public class MissingActionsException extends ProvisionException {

	private static final long serialVersionUID = 8617693596359747490L;
	private final MissingAction[] missingActions;

	public MissingActionsException(MissingAction[] missingActions) {
		super(getMissingActionsMessage(missingActions));
		this.missingActions = missingActions;
	}

	private static String getMissingActionsMessage(MissingAction[] missingActions) {

		if (missingActions.length == 0)
			throw new IllegalArgumentException("Bad exception: No missing actions"); //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < missingActions.length; i++) {
			MissingAction missingAction = missingActions[i];
			buffer.append(missingAction.getActionId());
			if (missingAction.getVersionRange() != null) {
				buffer.append("/"); //$NON-NLS-1$
				buffer.append(missingAction.getVersionRange().toString());
			}
			if (i + 1 != missingActions.length)
				buffer.append(", "); //$NON-NLS-1$
		}

		return NLS.bind(Messages.actions_not_found, buffer.toString());
	}

	public MissingAction[] getMissingActions() {
		return missingActions;
	}
}

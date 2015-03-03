/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.EventObject;
import org.eclipse.equinox.p2.engine.IProfileEvent;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ProfileEvent extends EventObject implements IProfileEvent {
	private static final long serialVersionUID = 3082402920617281765L;

	private int reason;

	public ProfileEvent(String profileId, int reason) {
		super(profileId);
		this.reason = reason;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfileEvent#getReason()
	 */
	public int getReason() {
		return reason;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfileEvent#getProfileId()
	 */
	public String getProfileId() {
		return (String) getSource();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.EventObject#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("ProfileEvent["); //$NON-NLS-1$
		buffer.append(getProfileId());
		buffer.append("-->"); //$NON-NLS-1$
		switch (reason) {
			case IProfileEvent.ADDED :
				buffer.append("ADDED"); //$NON-NLS-1$
				break;
			case IProfileEvent.REMOVED :
				buffer.append("REMOVED"); //$NON-NLS-1$
				break;
			case IProfileEvent.CHANGED :
				buffer.append("CHANGED"); //$NON-NLS-1$
				break;
		}
		buffer.append("] "); //$NON-NLS-1$
		return buffer.toString();
	}
}

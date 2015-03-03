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

import org.eclipse.equinox.internal.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.engine.ISizingPhaseSet;

public class SizingPhaseSet extends PhaseSet implements ISizingPhaseSet {

	private static Sizing sizing;

	public SizingPhaseSet() {
		super(new Phase[] {sizing = new Sizing(100)});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.ISizingPhaseSet#getDiskSize()
	 */
	public long getDiskSize() {
		return sizing.getDiskSize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.ISizingPhaseSet#getDownloadSize()
	 */
	public long getDownloadSize() {
		return sizing.getDownloadSize();
	}
}

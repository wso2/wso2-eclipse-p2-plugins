/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.EventObject;

public class PhaseEvent extends EventObject {

	private static final long serialVersionUID = 8971345257149340658L;

	public static final int TYPE_START = 1;
	public static final int TYPE_END = 2;

	private int type;

	private String phaseId;

	private Operand[] operands;

	public PhaseEvent(String phaseId, Operand[] operands, int type) {
		super(operands);
		this.phaseId = phaseId;
		this.type = type;
		this.operands = operands;
	}

	public String getPhaseId() {
		return phaseId;
	}

	public int getType() {
		return type;
	}

	public Operand[] getOperands() {
		return operands;
	}
}

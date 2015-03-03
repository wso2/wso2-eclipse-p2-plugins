/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.*;

public abstract class InstallableUnitPhase extends Phase {
	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$

	protected InstallableUnitPhase(String phaseId, int weight, boolean forced) {
		super(phaseId, weight, forced);
	}

	protected InstallableUnitPhase(String phaseId, int weight) {
		this(phaseId, weight, false);
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		parameters.put(PARM_INSTALL_FOLDER, profile.getProperty(IProfile.PROP_INSTALL_FOLDER));
		return super.initializePhase(monitor, profile, parameters);
	}

	protected IStatus initializeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, initializeOperand(profile, iuOperand, parameters, monitor));
		IInstallableUnit unit = (IInstallableUnit) parameters.get(PARM_IU);
		if (unit != null) {
			Touchpoint touchpoint = getActionManager().getTouchpointPoint(unit.getTouchpointType());
			if (touchpoint != null) {
				parameters.put(PARM_TOUCHPOINT, touchpoint);
			}
		}
		mergeStatus(status, super.initializeOperand(profile, operand, parameters, monitor));
		return status;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected IStatus completeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, super.completeOperand(profile, iuOperand, parameters, monitor));
		mergeStatus(status, completeOperand(profile, iuOperand, parameters, monitor));
		return status;
	}

	protected IStatus completeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	final protected List<ProvisioningAction> getActions(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return null;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return getActions(iuOperand);
	}

	protected abstract List<ProvisioningAction> getActions(InstallableUnitOperand operand);

	final public boolean isApplicable(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return false;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return isApplicable(iuOperand);
	}

	protected boolean isApplicable(InstallableUnitOperand operand) {
		return true;
	}

	protected final List<ProvisioningAction> getActions(IInstallableUnit unit, String key) {
		List<ITouchpointInstruction> instructions = getInstructions(unit, key);
		int instrSize = instructions.size();
		if (instrSize == 0)
			return null;

		List<ProvisioningAction> actions = new ArrayList<ProvisioningAction>();
		InstructionParser instructionParser = new InstructionParser(getActionManager());
		for (int i = 0; i < instrSize; i++) {
			actions.addAll(instructionParser.parseActions(instructions.get(i), unit.getTouchpointType()));
		}
		return actions;
	}

	private final static List<ITouchpointInstruction> getInstructions(IInstallableUnit unit, String key) {
		Collection<ITouchpointData> data = unit.getTouchpointData();
		int dataSize = data.size();
		if (dataSize == 0)
			return CollectionUtils.emptyList();

		ArrayList<ITouchpointInstruction> matches = new ArrayList<ITouchpointInstruction>(dataSize);
		for (ITouchpointData td : data) {
			ITouchpointInstruction instructions = td.getInstruction(key);
			if (instructions != null)
				matches.add(instructions);
		}
		return matches;
	}
}

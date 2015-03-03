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
package org.eclipse.equinox.internal.p2.engine.phases;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitPhase;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointType;

/**
 * An install phase that checks if the certificates used to sign the artifacts
 * being installed are from a trusted source.
 */
public class CheckTrust extends InstallableUnitPhase {

	public static final String PARM_ARTIFACT_FILES = "artifactFiles"; //$NON-NLS-1$

	public CheckTrust(int weight) {
		super(PhaseSetFactory.PHASE_CHECK_TRUST, weight);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null);
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		@SuppressWarnings("unchecked")
		Collection<File> artifactRequests = (Collection<File>) parameters.get(PARM_ARTIFACT_FILES);
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);

		// Instantiate a check trust manager
		CertificateChecker certificateChecker = new CertificateChecker(agent);
		certificateChecker.add(artifactRequests.toArray());
		IStatus status = certificateChecker.start();

		return status;
	}

	protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		List<ProvisioningAction> parsedActions = getActions(unit, phaseId);
		if (parsedActions != null)
			return parsedActions;

		ITouchpointType type = unit.getTouchpointType();
		if (type == null || type == ITouchpointType.NONE)
			return null;

		String actionId = getActionManager().getTouchpointQualifiedActionId(phaseId, type);
		ProvisioningAction action = getActionManager().getAction(actionId, null);
		if (action == null) {
			return null;
		}
		return Collections.singletonList(action);
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		parameters.put(PARM_IU, iu);

		return super.initializeOperand(profile, operand, parameters, monitor);
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		parameters.put(PARM_ARTIFACT_FILES, new ArrayList<File>());
		return super.initializePhase(monitor, profile, parameters);
	}

}

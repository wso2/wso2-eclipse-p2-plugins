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
package org.eclipse.equinox.internal.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.*;

public class Sizing extends InstallableUnitPhase {
	private static final String PHASE_ID = "sizing"; //$NON-NLS-1$
	private static final String COLLECT_PHASE_ID = "collect"; //$NON-NLS-1$

	private long sizeOnDisk;
	private long dlSize;

	public Sizing(int weight) {
		super(PHASE_ID, weight);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null && !op.second().equals(op.first()));
	}

	public long getDiskSize() {
		return sizeOnDisk;
	}

	public long getDownloadSize() {
		return dlSize;
	}

	protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		List<ProvisioningAction> parsedActions = getActions(unit, COLLECT_PHASE_ID);
		if (parsedActions != null)
			return parsedActions;

		ITouchpointType type = unit.getTouchpointType();
		if (type == null || type == ITouchpointType.NONE)
			return null;

		String actionId = getActionManager().getTouchpointQualifiedActionId(COLLECT_PHASE_ID, type);
		ProvisioningAction action = getActionManager().getAction(actionId, null);
		if (action == null) {
			return null;
		}
		return Collections.singletonList(action);
	}

	protected String getProblemMessage() {
		return Messages.Phase_Sizing_Error;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		@SuppressWarnings("unchecked")
		List<IArtifactRequest[]> artifactRequests = (List<IArtifactRequest[]>) parameters.get(Collect.PARM_ARTIFACT_REQUESTS);
		ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
		int statusCode = 0;

		Set<IArtifactRequest> artifactsToObtain = new HashSet<IArtifactRequest>(artifactRequests.size());

		for (IArtifactRequest[] requests : artifactRequests) {
			if (requests == null)
				continue;
			for (int i = 0; i < requests.length; i++) {
				artifactsToObtain.add(requests[i]);
			}
		}

		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;

		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		IQueryable<IArtifactRepository> repoQueryable = context.getArtifactRepositories(sub.newChild(500));
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<IArtifactRepository>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IArtifactRepository[] repositories = repoQueryable.query(all, sub.newChild(500)).toArray(IArtifactRepository.class);

		for (IArtifactRequest artifactRequest : artifactsToObtain) {
			if (sub.isCanceled())
				break;
			boolean found = false;
			for (int i = 0; i < repositories.length; i++) {
				IArtifactRepository repo = repositories[i];
				if (sub.isCanceled())
					return Status.CANCEL_STATUS;
				IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(artifactRequest.getArtifactKey());
				if (descriptors.length > 0) {
					if (descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE) != null)
						sizeOnDisk += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
					else
						statusCode = ProvisionException.ARTIFACT_INCOMPLETE_SIZING;
					if (descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE) != null)
						dlSize += Long.parseLong(descriptors[0].getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
					else
						statusCode = ProvisionException.ARTIFACT_INCOMPLETE_SIZING;
					found = true;
					break;
				}
			}
			if (!found)
				// The artifact wasn't present in any repository
				return new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.ARTIFACT_NOT_FOUND, Messages.Phase_Sizing_Error, null);
		}
		if (statusCode != 0)
			return new Status(IStatus.WARNING, EngineActivator.ID, statusCode, Messages.Phase_Sizing_Warning, null);
		return null;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		parameters.put(Collect.PARM_ARTIFACT_REQUESTS, new ArrayList<IArtifactRequest[]>());
		return null;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IStatus status = super.initializeOperand(profile, operand, parameters, monitor);
		// defer setting the IU until after the super method to avoid triggering touchpoint initialization
		IInstallableUnit iu = operand.second();
		parameters.put(PARM_IU, iu);
		return status;
	}
}

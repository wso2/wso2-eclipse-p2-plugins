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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;

/**
 * Concrete implementation of the {@link IEngine} API.
 */
public class Engine implements IEngine {

	private static final String ENGINE = "engine"; //$NON-NLS-1$
	private IProvisioningAgent agent;

	public Engine(IProvisioningAgent agent) {
		this.agent = agent;
		agent.registerService(ActionManager.SERVICE_NAME, new ActionManager());
	}

	private void checkArguments(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		if (iprofile == null)
			throw new IllegalArgumentException(Messages.null_profile);

		if (phaseSet == null)
			throw new IllegalArgumentException(Messages.null_phaseset);

		if (operands == null)
			throw new IllegalArgumentException(Messages.null_operands);
	}

	public IStatus perform(IProvisioningPlan plan, IPhaseSet phaseSet, IProgressMonitor monitor) {
		return perform(plan.getProfile(), phaseSet, ((ProvisioningPlan) plan).getOperands(), plan.getContext(), monitor);
	}

	public IStatus perform(IProvisioningPlan plan, IProgressMonitor monitor) {
		return perform(plan, PhaseSetFactory.createDefaultPhaseSet(), monitor);
	}

	public IStatus perform(IProfile iprofile, IPhaseSet phases, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		PhaseSet phaseSet = (PhaseSet) phases;
		checkArguments(iprofile, phaseSet, operands, context, monitor);
		if (operands.length == 0)
			return Status.OK_STATUS;
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		IProvisioningEventBus eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);

		if (context == null)
			context = new ProvisioningContext(agent);

		if (monitor == null)
			monitor = new NullProgressMonitor();

		Profile profile = profileRegistry.validate(iprofile);

		profileRegistry.lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));
			if (DebugHelper.DEBUG_ENGINE)
				DebugHelper.debug(ENGINE, "Beginning engine operation for profile=" + profile.getProfileId() + " [" + profile.getTimestamp() + "]:" + DebugHelper.LINE_SEPARATOR + DebugHelper.formatOperation(phaseSet, operands, context)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

			EngineSession session = new EngineSession(agent, profile, context);

			MultiStatus result = phaseSet.perform(session, operands, monitor);
			if (result.isOK() || result.matches(IStatus.INFO | IStatus.WARNING)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Preparing to commit engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				result.merge(session.prepare(monitor));
			}
			if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Rolling back engine operation for profile=" + profile.getProfileId() + ". Reason was: " + result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				IStatus status = session.rollback(monitor, result.getSeverity());
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
			} else {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Committing engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				if (profile.isChanged())
					profileRegistry.updateProfile(profile);
				IStatus status = session.commit(monitor);
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, operands, this));
			}
			//if there is only one child status, return that status instead because it will have more context
			IStatus[] children = result.getChildren();
			return children.length == 1 ? children[0] : result;
		} finally {
			profileRegistry.unlockProfile(profile);
			profile.setChanged(false);
		}
	}

	protected IStatus validate(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		checkArguments(iprofile, phaseSet, operands, context, monitor);

		if (context == null)
			context = new ProvisioningContext(agent);

		if (monitor == null)
			monitor = new NullProgressMonitor();

		ActionManager actionManager = (ActionManager) agent.getService(ActionManager.SERVICE_NAME);
		return phaseSet.validate(actionManager, iprofile, operands, context, monitor);
	}

	public IProvisioningPlan createPlan(IProfile profile, ProvisioningContext context) {
		return new ProvisioningPlan(profile, null, context);
	}
}

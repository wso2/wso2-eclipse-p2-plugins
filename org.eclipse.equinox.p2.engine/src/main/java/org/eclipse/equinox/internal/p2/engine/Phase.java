/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Landmark Graphics Corporation - bug 397183
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	protected static final String PARM_PHASE_ID = "phaseId"; //$NON-NLS-1$
	protected static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	protected static final String PARM_PROFILE_DATA_DIRECTORY = "profileDataDirectory"; //$NON-NLS-1$
	protected static final String PARM_CONTEXT = "context"; //$NON-NLS-1$
	/**
	 * Internal property.
	 */
	protected static final String PARM_AGENT = "agent"; //$NON-NLS-1$
	protected static final String PARM_FORCED = "forced"; //$NON-NLS-1$
	protected static final String PARM_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$
	protected static final String LAST_RESULT_INTERNAL_NAME = "_p2_internal_last_result_variable_"; //$NON-NLS-1$ //
	protected static final String LAST_RESULT_PUBLIC_NAME = "lastResult"; //$NON-NLS-1$

	protected final String phaseId;
	protected final int weight;
	protected final boolean forced;
	protected int prePerformWork = 1000;
	protected int mainPerformWork = 10000;
	protected int postPerformWork = 1000;
	private Map<String, Object> operandParameters = null;
	private Map<String, Object> phaseParameters = new HashMap<String, Object>();
	private Map<Touchpoint, Map<String, Object>> touchpointToTouchpointPhaseParameters = new HashMap<Touchpoint, Map<String, Object>>();
	private Map<Touchpoint, Map<String, Object>> touchpointToTouchpointOperandParameters = new HashMap<Touchpoint, Map<String, Object>>();
	ActionManager actionManager; // injected from phaseset
	protected boolean isPaused = false;

	protected Phase(String phaseId, int weight, boolean forced) {
		if (phaseId == null || phaseId.length() == 0)
			throw new IllegalArgumentException(Messages.phaseid_not_set);
		if (weight <= 0)
			throw new IllegalArgumentException(Messages.phaseid_not_positive);
		this.weight = weight;
		this.phaseId = phaseId;
		this.forced = forced;
	}

	protected Phase(String phaseId, int weight) {
		this(phaseId, weight, false);
	}

	final protected ActionManager getActionManager() {
		return actionManager;
	}

	public String toString() {
		return getClass().getName() + " - " + this.weight; //$NON-NLS-1$
	}

	private void broadcastPhaseEvent(EngineSession session, Operand[] operands, int type) {
		IProvisioningEventBus bus = (IProvisioningEventBus) session.getAgent().getService(IProvisioningEventBus.SERVICE_NAME);
		if (bus != null) {
			bus.publishEvent(getPhaseEvent(operands, type));
		}
	}

	protected PhaseEvent getPhaseEvent(final Operand[] operands, int type) {
		return new PhaseEvent(phaseId, operands, type);
	}

	void perform(MultiStatus status, EngineSession session, Operand[] operands, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, prePerformWork + mainPerformWork + postPerformWork);
		session.recordPhaseEnter(this);
		broadcastPhaseEvent(session, operands, PhaseEvent.TYPE_START);
		prePerform(status, session, subMonitor.newChild(prePerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;
		session.recordPhaseStart(this);

		subMonitor.setWorkRemaining(mainPerformWork + postPerformWork);
		mainPerform(status, session, operands, subMonitor.newChild(mainPerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;

		session.recordPhaseEnd(this);
		subMonitor.setWorkRemaining(postPerformWork);
		postPerform(status, session, subMonitor.newChild(postPerformWork));
		phaseParameters.clear();
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;
		broadcastPhaseEvent(session, operands, PhaseEvent.TYPE_END);
		session.recordPhaseExit(this);
		subMonitor.done();
	}

	void prePerform(MultiStatus status, EngineSession session, IProgressMonitor monitor) {
		IProfile profile = session.getProfile();
		phaseParameters.put(PARM_PROFILE, profile);
		phaseParameters.put(PARM_PROFILE_DATA_DIRECTORY, session.getProfileDataDirectory());
		phaseParameters.put(PARM_CONTEXT, session.getProvisioningContext());
		phaseParameters.put(PARM_PHASE_ID, phaseId);
		phaseParameters.put(PARM_FORCED, Boolean.toString(forced));
		phaseParameters.put(PARM_AGENT, session.getAgent());
		mergeStatus(status, initializePhase(monitor, profile, phaseParameters));
	}

	private void mainPerform(MultiStatus status, EngineSession session, Operand[] operands, SubMonitor subMonitor) {
		IProfile profile = session.getProfile();
		subMonitor.beginTask(null, operands.length);
		for (int i = 0; i < operands.length; i++) {
			subMonitor.setWorkRemaining(operands.length - i);
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			while (isPaused) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					mergeStatus(status, new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_thread_interrupted_error, phaseId), e));
					return;
				}
				if (subMonitor.isCanceled())
					throw new OperationCanceledException();
			}
			Operand operand = operands[i];
			if (!isApplicable(operand))
				continue;

			session.recordOperandStart(operand);
			List<ProvisioningAction> actions = getActions(operand);
			operandParameters = new HashMap<String, Object>(phaseParameters);
			operandParameters.put(PARM_OPERAND, operand);
			mergeStatus(status, initializeOperand(profile, operand, operandParameters, subMonitor));
			if (status.matches(IStatus.ERROR | IStatus.CANCEL)) {
				operandParameters = null;
				return;
			}

			Touchpoint operandTouchpoint = (Touchpoint) operandParameters.get(PARM_TOUCHPOINT);
			if (operandTouchpoint != null) {
				mergeStatus(status, initializeTouchpointParameters(profile, operand, operandTouchpoint, subMonitor));
				if (status.matches(IStatus.ERROR | IStatus.CANCEL))
					return;

				operandParameters = touchpointToTouchpointOperandParameters.get(operandTouchpoint);
			}

			if (actions != null) {
				Object lastResult = null;
				for (int j = 0; j < actions.size(); j++) {
					ProvisioningAction action = actions.get(j);
					Map<String, Object> parameters = operandParameters;
					Touchpoint touchpoint = action.getTouchpoint();
					if (touchpoint != null) {
						mergeStatus(status, initializeTouchpointParameters(profile, operand, touchpoint, subMonitor));
						if (status.matches(IStatus.ERROR | IStatus.CANCEL))
							return;

						parameters = touchpointToTouchpointOperandParameters.get(touchpoint);
					}
					if (lastResult != null) {
						parameters = new HashMap<String, Object>(parameters);
						parameters.put(LAST_RESULT_INTERNAL_NAME, lastResult);
					}
					parameters = Collections.unmodifiableMap(parameters);

					IStatus actionStatus = null;
					try {
						session.recordActionExecute(action, parameters);
						actionStatus = action.execute(parameters);
						lastResult = action.getResult();
					} catch (RuntimeException e) {
						if (!forced)
							throw e;
						// "action.execute" calls user code and might throw an unchecked exception
						// we catch the error here to gather information on where the problem occurred.
						actionStatus = new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.forced_action_execute_error, action.getClass().getName()), e);
					} catch (LinkageError e) {
						if (!forced)
							throw e;
						// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
						actionStatus = new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.forced_action_execute_error, action.getClass().getName()), e);
					}
					if (forced && actionStatus != null && actionStatus.matches(IStatus.ERROR)) {
						MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, getProblemMessage(), null);
						result.add(new Status(IStatus.ERROR, EngineActivator.ID, session.getContextString(this, operand, action), null));
						LogHelper.log(result);
						actionStatus = Status.OK_STATUS;
					}
					mergeStatus(status, actionStatus);
					if (status.matches(IStatus.ERROR | IStatus.CANCEL))
						return;
				}
			}
			mergeStatus(status, touchpointCompleteOperand(profile, operand, operandParameters, subMonitor));
			mergeStatus(status, completeOperand(profile, operand, operandParameters, subMonitor));
			if (status.matches(IStatus.ERROR | IStatus.CANCEL))
				return;
			operandParameters = null;
			session.recordOperandEnd(operand);
			subMonitor.worked(1);
		}
	}

	private IStatus initializeTouchpointParameters(IProfile profile, Operand operand, Touchpoint touchpoint, IProgressMonitor monitor) {
		if (touchpointToTouchpointOperandParameters.containsKey(touchpoint))
			return Status.OK_STATUS;

		Map<String, Object> touchpointPhaseParameters = touchpointToTouchpointPhaseParameters.get(touchpoint);
		if (touchpointPhaseParameters == null) {
			touchpointPhaseParameters = new HashMap<String, Object>(phaseParameters);
			IStatus status = touchpoint.initializePhase(monitor, profile, phaseId, touchpointPhaseParameters);
			if (status != null && status.matches(IStatus.ERROR | IStatus.CANCEL))
				return status;
			touchpointToTouchpointPhaseParameters.put(touchpoint, touchpointPhaseParameters);
		}

		Map<String, Object> touchpointOperandParameters = new HashMap<String, Object>(touchpointPhaseParameters);
		touchpointOperandParameters.putAll(operandParameters);
		IStatus status = touchpoint.initializeOperand(profile, touchpointOperandParameters);
		if (status != null && status.matches(IStatus.ERROR | IStatus.CANCEL))
			return status;
		touchpointToTouchpointOperandParameters.put(touchpoint, touchpointOperandParameters);
		return Status.OK_STATUS;
	}

	/**
	 * Merges a given IStatus into a MultiStatus
	 */
	protected static void mergeStatus(MultiStatus multi, IStatus status) {
		if (status != null && !status.isOK())
			multi.merge(status);
	}

	void postPerform(MultiStatus status, EngineSession session, IProgressMonitor monitor) {
		IProfile profile = session.getProfile();
		mergeStatus(status, touchpointCompletePhase(monitor, profile, phaseParameters));
		mergeStatus(status, completePhase(monitor, profile, phaseParameters));
	}

	void undo(MultiStatus status, EngineSession session, IProfile profile, Operand operand, ProvisioningAction[] actions, ProvisioningContext context) {
		if (operandParameters == null) {
			operandParameters = new HashMap<String, Object>(phaseParameters);
			operandParameters.put(PARM_OPERAND, operand);
			mergeStatus(status, initializeOperand(profile, operand, operandParameters, new NullProgressMonitor()));
			Touchpoint operandTouchpoint = (Touchpoint) operandParameters.get(PARM_TOUCHPOINT);
			if (operandTouchpoint != null) {
				mergeStatus(status, initializeTouchpointParameters(profile, operand, operandTouchpoint, new NullProgressMonitor()));
				if (status.matches(IStatus.ERROR | IStatus.CANCEL))
					return;

				operandParameters = touchpointToTouchpointOperandParameters.get(operandTouchpoint);
			}
			operandParameters = Collections.unmodifiableMap(operandParameters);
		}
		for (int j = 0; j < actions.length; j++) {
			ProvisioningAction action = actions[j];
			Map<String, Object> parameters = operandParameters;
			Touchpoint touchpoint = action.getTouchpoint();
			if (touchpoint != null) {
				mergeStatus(status, initializeTouchpointParameters(profile, operand, touchpoint, new NullProgressMonitor()));
				if (status.matches(IStatus.ERROR))
					return;

				parameters = touchpointToTouchpointOperandParameters.get(touchpoint);
			}
			IStatus actionStatus = null;
			try {
				session.recordActionUndo(action, parameters);
				actionStatus = action.undo(parameters);
			} catch (RuntimeException e) {
				// "action.undo" calls user code and might throw an unchecked exception
				// we catch the error here to gather information on where the problem occurred.
				actionStatus = new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.action_undo_error, action.getClass().getName()), e);
			} catch (LinkageError e) {
				// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
				actionStatus = new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.action_undo_error, action.getClass().getName()), e);
			}
			if (actionStatus != null && actionStatus.matches(IStatus.ERROR)) {
				MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, getProblemMessage(), null);
				result.add(new Status(IStatus.ERROR, EngineActivator.ID, session.getContextString(this, operand, action), null));
				result.merge(actionStatus);
			}
		}
		mergeStatus(status, touchpointCompleteOperand(profile, operand, operandParameters, new NullProgressMonitor()));
		mergeStatus(status, completeOperand(profile, operand, operandParameters, new NullProgressMonitor()));
		operandParameters = null;
	}

	public boolean isApplicable(Operand operand) {
		return true;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	IStatus touchpointCompletePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		if (touchpointToTouchpointPhaseParameters.isEmpty())
			return Status.OK_STATUS;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Map.Entry<Touchpoint, Map<String, Object>> entry : touchpointToTouchpointPhaseParameters.entrySet()) {
			Touchpoint touchpoint = entry.getKey();
			Map<String, Object> touchpointParameters = entry.getValue();
			mergeStatus(status, touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
		}
		touchpointToTouchpointPhaseParameters.clear();
		return status;
	}

	protected IStatus completeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	IStatus touchpointCompleteOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		if (touchpointToTouchpointOperandParameters.isEmpty())
			return Status.OK_STATUS;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Map.Entry<Touchpoint, Map<String, Object>> entry : touchpointToTouchpointOperandParameters.entrySet()) {
			Touchpoint touchpoint = entry.getKey();
			Map<String, Object> touchpointParameters = entry.getValue();
			mergeStatus(status, touchpoint.completeOperand(profile, touchpointParameters));
		}
		touchpointToTouchpointOperandParameters.clear();
		return status;
	}

	protected IStatus initializeOperand(IProfile profile, Operand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected abstract List<ProvisioningAction> getActions(Operand operand);

	/**
	 * Returns a human-readable message to be displayed in case of an error performing
	 * this phase. Subclasses should override.
	 */
	protected String getProblemMessage() {
		return NLS.bind(Messages.phase_error, getClass().getName());
	}

	protected void setPaused(boolean isPaused) {
		this.isPaused = isPaused;
	}
}

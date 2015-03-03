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
package org.eclipse.equinox.p2.engine.spi;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.*;

/**
 * A touchpoint is responsible for executing the required provisioning steps
 * for each phase corresponding to a particular targeted system (eclipse, native). 
 * The order of phases is defined in the {@link IPhaseSet}.  
 * @since 2.0
 */
public abstract class Touchpoint {

	/** 
	 * This method is for backwards compatibility only, to be used by touchpoints
	 * that existed prior to action ids being fully qualified by the engine.
	 * @param actionId the unqualified action id
	 * @return the qualified action id
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public String qualifyAction(String actionId) {
		return actionId;
	}

	/**
	 * This method is called at the beginning of execution of an engine phase. This
	 * is an opportunity for the touchpoint to initialize any phase-specific structures.
	 * <p>
	 * The result of this method can be used to abort execution of the entire engine
	 * operation, by using a severity of {@link IStatus#ERROR} or {@link IStatus#CANCEL}.
	 * The result can also contain warnings or informational status that will be aggregated
	 * and returned to the caller of {@link IEngine#perform(org.eclipse.equinox.p2.engine.IProvisioningPlan, IProgressMonitor)}.
	 * </p>
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @param profile the profile that is being operated on
	 * @param phaseId the id of the phase
	 * @param parameters data provided by the engine to the touchpoint 
	 * @return the result of phase initialization
	 */
	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the end of execution of an engine phase. This
	 * is an opportunity for the touchpoint to clean up any phase-specific structures
	 * or perform any final work for the current phase.
	 * <p>
	 * The result of this method can be used to abort execution of the entire engine
	 * operation, by using a severity of {@link IStatus#ERROR} or {@link IStatus#CANCEL}.
	 * The result can also contain warnings or informational status that will be aggregated
	 * and returned to the caller of {@link IEngine#perform(org.eclipse.equinox.p2.engine.IProvisioningPlan, IProgressMonitor)}.
	 * </p>
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @param profile the profile that is being operated on
	 * @param phaseId the id of the phase
	 * @param parameters data provided by the engine to the touchpoint 
	 * @return the result of phase completion
	 */
	public IStatus completePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the beginning of processing of a single engine operand
	 * (for example a given installable unit being installed or uninstalled). This
	 * is an opportunity for the touchpoint to initialize any data structures specific
	 * to a single operand.
	 * <p>
	 * The result of this method can be used to abort execution of the entire engine
	 * operation, by using a severity of {@link IStatus#ERROR} or {@link IStatus#CANCEL}.
	 * The result can also contain warnings or informational status that will be aggregated
	 * and returned to the caller of {@link IEngine#perform(org.eclipse.equinox.p2.engine.IProvisioningPlan, IProgressMonitor)}.
	 * </p>
	 * @param profile the profile that is being operated on
	 * @param parameters data provided by the engine to the touchpoint 
	 * @return the result of initialization
	 */
	public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the end of processing of a single engine operand
	 * (for example a given installable unit being installed or uninstalled). This
	 * is an opportunity for the touchpoint to clean up any data structures specific
	 * to a single operand.
	 * <p>
	 * The result of this method can be used to abort execution of the entire engine
	 * operation, by using a severity of {@link IStatus#ERROR} or {@link IStatus#CANCEL}.
	 * The result can also contain warnings or informational status that will be aggregated
	 * and returned to the caller of {@link IEngine#perform(org.eclipse.equinox.p2.engine.IProvisioningPlan, IProgressMonitor)}.
	 * </p>
	 * @param profile the profile that is being operated on
	 * @param parameters data provided by the engine to the touchpoint 
	 * @return the result of the completion work
	 */
	public IStatus completeOperand(IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the end of an engine operation after all phases have 
	 * been executed but prior to the operation being formally committed/persisted. This is an opportunity to perform any final checks
	 * against the profile, or log any information that might be needed to recover if the operation fails to complete in a regular
	 * manner.
	 * <p>
	 * The result of this method can be used to abort execution of the entire engine
	 * operation, by using a severity of {@link IStatus#ERROR} or {@link IStatus#CANCEL}.
	 * The result can also contain warnings or informational status that will be aggregated
	 * and returned to the caller of {@link IEngine#perform(org.eclipse.equinox.p2.engine.IProvisioningPlan, IProgressMonitor)}.
	 * </p>
	 * 
	 * @param profile the profile about to be modified
	 * @return the result of preparation work
	 */
	public IStatus prepare(IProfile profile) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the end of an engine operation after all phases have 
	 * been executed and after the touchpoint has had prepare called. When this method is invoked,
	 * it signals that the engine operation was a complete success, and the touchpoint should commit
	 * or persist any changes it has made to some persistent storage (for
	 * example the file system).
	 * <p>
	 * The result of this method can be used to report on the success or failure
	 * of the commit. However, at this point it is too late for the engine operation
	 * to fail, and the result returned from this method will not prevent the engine
	 * from completing its work.
	 * </p>
	 * 
	 * @param profile the profile that was modified
	 * @return the result of commit work
	 */
	public IStatus commit(IProfile profile) {
		return Status.OK_STATUS;
	}

	/**
	 * This method is called at the end of an engine operation after all phases have 
	 * been executed. When this method is invoked, it signals that the engine
	 * operation was a failure, and the touchpoint should discard any
	 * changes it has made.
	 * <p>
	 * The result of this method can be used to report on the success or failure
	 * of the rollback. However, at this point it is too late for the engine operation
	 * to be stopped, and the result returned from this method will not prevent the engine
	 * from completing its rollback work.
	 * </p>
	 * 
	 * @param profile the profile that was modified
	 * @return the result of commit work
	 */
	public IStatus rollback(IProfile profile) {
		return Status.OK_STATUS;
	}
}

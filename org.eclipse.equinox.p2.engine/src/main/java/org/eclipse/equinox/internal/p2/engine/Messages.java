/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	public static String action_not_found;

	public static String action_syntax_error;
	public static String action_undo_error;

	public static String ActionManager_Exception_Creating_Action_Extension;
	public static String ActionManager_Required_Touchpoint_Not_Found;

	public static String actions_not_found;
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.engine.messages"; //$NON-NLS-1$

	public static String CertificateChecker_CertificateError;
	public static String CertificateChecker_CertificateRejected;
	public static String CertificateChecker_KeystoreConnectionError;

	public static String CertificateChecker_SignedContentError;
	public static String CertificateChecker_SignedContentIOError;
	public static String CertificateChecker_UnsignedNotAllowed;

	public static String committing;
	public static String download_artifact;
	public static String download_no_repository;
	public static String Engine_Operation_Canceled_By_User;
	public static String error_parsing_profile;
	public static String error_persisting_profile;
	public static String forced_action_execute_error;
	public static String InstallableUnitEvent_type_not_install_or_uninstall_or_configure;
	public static String io_FailedRead;
	public static String io_NotFound;
	public static String not_current_operand;
	public static String not_current_phase;
	public static String null_action;

	public static String null_operand;
	public static String null_operands;
	public static String null_phase;
	public static String null_phases;
	public static String null_phaseset;
	public static String null_profile;
	public static String operand_not_started;

	public static String operand_started;

	public static String ParameterizedProvisioningAction_action_or_parameters_null;
	public static String phase_error;
	public static String phase_not_started;
	public static String phase_started;
	public static String phase_undo_error;
	public static String phase_undo_operand_error;

	public static String Phase_Collect_Error;
	public static String Phase_Install_Error;
	public static String Phase_Configure_Error;
	public static String Phase_Configure_Task;
	public static String Phase_Install_Task;
	public static String Phase_Sizing_Error;
	public static String Phase_Sizing_Warning;

	public static String phase_thread_interrupted_error;
	public static String Phase_Unconfigure_Error;
	public static String Phase_Uninstall_Error;

	public static String phaseid_not_positive;
	public static String phaseid_not_set;
	public static String preparing;
	public static String profile_does_not_exist;
	public static String Profile_Duplicate_Root_Profile_Id;
	public static String profile_lock_not_reentrant;
	public static String profile_not_current;
	public static String profile_changed;
	public static String profile_not_registered;
	public static String Profile_Null_Profile_Id;
	public static String Profile_Parent_Not_Found;
	public static String ProfilePreferences_saving;
	public static String reg_dir_not_available;
	public static String rollingback_cancel;
	public static String rollingback_error;
	public static String session_commit_error;
	public static String session_context;
	public static String session_prepare_error;
	public static String shared_profile_not_found;
	public static String Shared_Profile;

	public static String SimpleProfileRegistry_Bad_profile_location;
	public static String SimpleProfileRegistry_CannotRemoveCurrentSnapshot;
	public static String SimpleProfileRegistry_Parser_Error_Parsing_Registry;
	public static String SimpleProfileRegistry_Parser_Has_Incompatible_Version;
	public static String SimpleProfileRegistry_Profile_in_use;
	public static String SimpleProfileRegistry_Profile_not_locked;
	public static String SimpleProfileRegistry_Profile_not_locked_due_to_exception;
	public static String SimpleProfileRegistry_States_Error_Reading_File;
	public static String SimpleProfileRegistry_States_Error_Writing_File;
	public static String SimpleProfileRegistry_state_not_found;

	public static String thread_not_owner;
	public static String touchpoint_commit_error;
	public static String touchpoint_prepare_error;
	public static String touchpoint_rollback_error;

	public static String TouchpointManager_Attribute_Not_Specified;
	public static String TouchpointManager_Conflicting_Touchpoint_Types;
	public static String TouchpointManager_Exception_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Incorrectly_Named_Extension;
	public static String TouchpointManager_Null_Creating_Touchpoint_Extension;
	public static String TouchpointManager_Null_Touchpoint_Type_Argument;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}

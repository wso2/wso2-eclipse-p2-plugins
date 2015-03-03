/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.service.debug.DebugOptions;

public class DebugHelper {
	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	public static final boolean DEBUG_PROFILE_REGISTRY;
	public static final boolean DEBUG_ENGINE;
	public static final boolean DEBUG_ENGINE_SESSION;
	public static final boolean DEBUG_CERTIFICATE_CHECKER_UNSIGNED;
	public static final boolean DEBUG_CERTIFICATE_CHECKER_UNTRUSTED;

	static {
		DebugOptions options = (DebugOptions) ServiceHelper.getService(EngineActivator.getContext(), DebugOptions.class.getName());
		if (options != null) {
			DEBUG_PROFILE_REGISTRY = options.getBooleanOption(EngineActivator.ID + "/profileregistry/debug", false); //$NON-NLS-1$
			DEBUG_ENGINE = options.getBooleanOption(EngineActivator.ID + "/engine/debug", false); //$NON-NLS-1$
			DEBUG_ENGINE_SESSION = options.getBooleanOption(EngineActivator.ID + "/enginesession/debug", false); //$NON-NLS-1$
			DEBUG_CERTIFICATE_CHECKER_UNSIGNED = options.getBooleanOption(EngineActivator.ID + "/certificatechecker/unsigned", false); //$NON-NLS-1$
			DEBUG_CERTIFICATE_CHECKER_UNTRUSTED = options.getBooleanOption(EngineActivator.ID + "/certificatechecker/untrusted", false); //$NON-NLS-1$
		} else {
			DEBUG_PROFILE_REGISTRY = false;
			DEBUG_ENGINE = false;
			DEBUG_ENGINE_SESSION = false;
			DEBUG_CERTIFICATE_CHECKER_UNSIGNED = false;
			DEBUG_CERTIFICATE_CHECKER_UNTRUSTED = false;
		}
	}

	public static void debug(String name, String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("["); //$NON-NLS-1$
		buffer.append(EngineActivator.ID + "-" + name); //$NON-NLS-1$
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] " + LINE_SEPARATOR); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	public static String formatArray(Collection<? extends Object> array, boolean toString, boolean newLines) {
		if (array == null || array.size() == 0)
			return "[]"; //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		boolean first = true;
		for (Object elem : array) {
			if (first)
				first = false;
			else
				buffer.append(',');

			if (toString)
				buffer.append(elem.toString());
			else
				buffer.append(elem.getClass().getName());
			if (newLines)
				buffer.append(DebugHelper.LINE_SEPARATOR);
			else
				buffer.append(' ');
		}
		buffer.append(']');
		return buffer.toString();
	}

	public static String formatOperation(PhaseSet phaseSet, Operand[] operands, ProvisioningContext context) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("phaseSet=" + formatPhaseSet(phaseSet)); //$NON-NLS-1$
		buffer.append(","); //$NON-NLS-1$
		buffer.append(DebugHelper.LINE_SEPARATOR);
		buffer.append("operands=" + formatOperands(operands)); //$NON-NLS-1$
		buffer.append(","); //$NON-NLS-1$
		buffer.append(DebugHelper.LINE_SEPARATOR);
		buffer.append("context=" + formatContext(context)); //$NON-NLS-1$
		return buffer.toString();
	}

	public static String formatOperands(Operand[] operands) {
		String[] operandStrings = new String[operands.length];
		for (int i = 0; i < operands.length; i++) {
			if (operands[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOperand = (InstallableUnitOperand) operands[i];
				operandStrings[i] = formatInstallableUnitOperand(iuOperand);
			} else {
				operandStrings[i] = operands[i].toString();
			}
		}
		return DebugHelper.formatArray(Arrays.asList(operandStrings), true, true);
	}

	public static String formatInstallableUnitOperand(InstallableUnitOperand iuOperand) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(iuOperand.first());
		if (iuOperand.first() != null && iuOperand.first().getFragments() != null)
			buffer.append(DebugHelper.formatArray(iuOperand.first().getFragments(), true, false));
		buffer.append(" --> "); //$NON-NLS-1$
		buffer.append(iuOperand.second());
		if (iuOperand.second() != null && iuOperand.second().getFragments() != null)
			buffer.append(DebugHelper.formatArray(iuOperand.second().getFragments(), true, false));
		return buffer.toString();
	}

	public static String formatPhaseSet(PhaseSet phaseSet) {
		StringBuffer buffer = new StringBuffer(phaseSet.getClass().getName());
		buffer.append(DebugHelper.formatArray(Arrays.asList(phaseSet.getPhases()), false, false));
		return buffer.toString();
	}

	public static String formatContext(ProvisioningContext context) {
		return context.toString();
	}

	public static String formatAction(ProvisioningAction action, Map<String, Object> parameters) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(action.getClass().getName());
		if (action instanceof ParameterizedProvisioningAction) {
			ParameterizedProvisioningAction parameterizedAction = (ParameterizedProvisioningAction) action;
			buffer.append("{action=" + parameterizedAction.getAction().getClass().getName()); //$NON-NLS-1$
			buffer.append(", actionText=" + parameterizedAction.getActionText() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		buffer.append(DebugHelper.LINE_SEPARATOR);
		buffer.append("parameters=" + formatParameters(parameters)); //$NON-NLS-1$
		return buffer.toString();
	}

	public static String formatParameters(Map<String, ? extends Object> parameters) {
		Iterator<? extends Entry<String, ? extends Object>> it = parameters.entrySet().iterator();
		if (!it.hasNext())
			return "{}"; //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer();
		buffer.append('{');
		for (;;) {
			Entry<String, ? extends Object> e = it.next();
			String key = e.getKey();
			buffer.append(key);
			buffer.append('=');
			Object value = e.getValue();
			if (value instanceof String || value instanceof File || value instanceof Operand || value instanceof IArtifactKey || value instanceof IInstallableUnit)
				buffer.append(value);
			else if (value instanceof IProfile)
				buffer.append(((IProfile) value).getProfileId());
			else
				buffer.append(value.getClass().getName());
			if (!it.hasNext()) {
				buffer.append('}');
				break;
			}
			buffer.append(',');
			buffer.append(DebugHelper.LINE_SEPARATOR);
			buffer.append(' ');
		}
		return buffer.toString();
	}
}

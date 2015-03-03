/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Landmark Graphics Corporation - bug 397183
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.spi.*;

public class ParameterizedProvisioningAction extends ProvisioningAction {
	private ProvisioningAction action;
	private Map<String, String> actionParameters;
	//ActualParameter is used to keep values to which variables have been resolved.
	//This is especially useful when undoing in the presence of variables that change (e.g. lastResult) 
	private Map<String, Object> actualParameters;
	private String actionText;

	public ParameterizedProvisioningAction(ProvisioningAction action, Map<String, String> actionParameters, String actionText) {
		if (action == null || actionParameters == null)
			throw new IllegalArgumentException(Messages.ParameterizedProvisioningAction_action_or_parameters_null);
		this.action = action;
		this.actionParameters = actionParameters;
		this.actualParameters = new HashMap<String, Object>(actionParameters.size());
		this.actionText = actionText;
	}

	public IStatus execute(Map<String, Object> parameters) {
		parameters = processActionParameters(parameters);
		return action.execute(parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		parameters = processActionParameters(parameters);
		return action.undo(parameters);
	}

	private Map<String, Object> processActionParameters(Map<String, Object> parameters) {
		Map<String, Object> result = new HashMap<String, Object>(parameters);
		for (Entry<String, String> entry : actionParameters.entrySet()) {
			String name = entry.getKey();
			Object value = processVariables(entry.getValue(), parameters, false);
			result.put(name, value);
		}
		return Collections.unmodifiableMap(result);
	}

	//allowInfixReplacement triggers the replacement of the variables found in the middle of a string (e.g. abc${var}def) 
	private Object processVariables(String parameterValue, Map<String, Object> parameters, boolean allowInfixReplacement) {
		int variableBeginIndex = parameterValue.indexOf("${"); //$NON-NLS-1$
		if (variableBeginIndex == -1)
			return parameterValue;

		int variableEndIndex = parameterValue.indexOf('}', variableBeginIndex + 2);
		if (variableEndIndex == -1)
			return parameterValue;

		String preVariable = parameterValue.substring(0, variableBeginIndex);
		String variableName = parameterValue.substring(variableBeginIndex + 2, variableEndIndex);

		//replace the internal name by the user visible name
		if (Phase.LAST_RESULT_PUBLIC_NAME.equals(variableName)) {
			variableName = Phase.LAST_RESULT_INTERNAL_NAME;
		}
		Object valueUsed = actualParameters.get(variableName);
		Object value = valueUsed == null ? parameters.get(variableName) : valueUsed;
		actualParameters.put(variableName, value);

		if (value instanceof Value) {
			if (allowInfixReplacement == false && variableBeginIndex == 0 && variableEndIndex == parameterValue.length() - 1) {
				return ((Value<?>) value).getValue();
			}

			Value<?> result = (Value<?>) value;
			if (result.getClazz() == String.class) {
				value = result.getValue();
			} else
				throw new RuntimeException("The type of the variable is expected to be a String"); //$NON-NLS-1$
		}

		// try to replace this parameter with a character
		if (value == null && variableName.charAt(0) == '#') {
			try {
				int code = Integer.parseInt(variableName.substring(1));
				if (code >= 0 && code < 65536)
					value = Character.toString((char) code);
			} catch (Throwable t) {
				// ignore and leave value as null
			}
		}

		String variableValue = value == null ? "" : value.toString(); //$NON-NLS-1$			//TODO This is where we replace the values
		String postVariable = (String) processVariables(parameterValue.substring(variableEndIndex + 1), parameters, true);
		return preVariable + variableValue + postVariable;
	}

	public ProvisioningAction getAction() {
		return action;
	}

	public Map<String, String> getParameters() {
		return actionParameters;
	}

	public String getActionText() {
		return actionText;
	}

	public Touchpoint getTouchpoint() {
		return action.getTouchpoint();
	}

	public void setTouchpoint(Touchpoint touchpoint) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Value<?> getResult() {
		return action.getResult();
	}

}

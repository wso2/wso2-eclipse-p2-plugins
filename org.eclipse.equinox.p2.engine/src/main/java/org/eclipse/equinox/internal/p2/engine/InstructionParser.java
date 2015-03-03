/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public class InstructionParser {

	public class ActionEntry {

		protected final VersionRange versionRange;
		protected final String actionId;

		public ActionEntry(String actionId, VersionRange versionRange) {
			this.actionId = actionId;
			this.versionRange = versionRange;
		}
	}

	private static final String VERSION_EQUALS = "version="; //$NON-NLS-1$
	private ActionManager actionManager;

	public InstructionParser(ActionManager actionManager) {
		Assert.isNotNull(actionManager);
		this.actionManager = actionManager;
	}

	public List<ProvisioningAction> parseActions(ITouchpointInstruction instruction, ITouchpointType touchpointType) {
		List<ProvisioningAction> actions = new ArrayList<ProvisioningAction>();
		Map<String, ActionEntry> importMap = parseImportAttribute(instruction.getImportAttribute());
		StringTokenizer tokenizer = new StringTokenizer(instruction.getBody(), ";"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			actions.add(parseAction(tokenizer.nextToken(), importMap, touchpointType));
		}
		return actions;
	}

	private Map<String, ActionEntry> parseImportAttribute(String importAttribute) {
		if (importAttribute == null)
			return CollectionUtils.emptyMap();

		Map<String, ActionEntry> result = new HashMap<String, ActionEntry>();
		StringTokenizer tokenizer = new StringTokenizer(importAttribute, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			StringTokenizer actionTokenizer = new StringTokenizer(tokenizer.nextToken(), ";"); //$NON-NLS-1$
			String actionId = actionTokenizer.nextToken().trim();
			int lastDot = actionId.lastIndexOf('.');
			String actionKey = (lastDot == -1) ? actionId : actionId.substring(lastDot + 1);
			VersionRange actionVersionRange = null;
			while (actionTokenizer.hasMoreTokens()) {
				String actionAttribute = actionTokenizer.nextToken().trim();
				if (actionAttribute.startsWith(VERSION_EQUALS))
					actionVersionRange = new VersionRange(actionAttribute.substring(VERSION_EQUALS.length() + 1));
			}
			result.put(actionKey, new ActionEntry(actionId, actionVersionRange));
			result.put(actionId, new ActionEntry(actionId, actionVersionRange));
		}
		return result;
	}

	private ProvisioningAction parseAction(String statement, Map<String, ActionEntry> qualifier, ITouchpointType touchpointType) {
		int openBracket = statement.indexOf('(');
		int closeBracket = statement.lastIndexOf(')');
		if (openBracket == -1 || closeBracket == -1 || openBracket > closeBracket)
			throw new IllegalArgumentException(NLS.bind(Messages.action_syntax_error, statement));
		String actionName = statement.substring(0, openBracket).trim();
		ProvisioningAction action = lookupAction(actionName, qualifier, touchpointType);
		if (action instanceof MissingAction)
			return action;

		String nameValuePairs = statement.substring(openBracket + 1, closeBracket);
		if (nameValuePairs.length() == 0)
			return new ParameterizedProvisioningAction(action, CollectionUtils.<String, String> emptyMap(), statement);

		StringTokenizer tokenizer = new StringTokenizer(nameValuePairs, ","); //$NON-NLS-1$
		Map<String, String> parameters = new HashMap<String, String>();
		while (tokenizer.hasMoreTokens()) {
			String nameValuePair = tokenizer.nextToken();
			int colonIndex = nameValuePair.indexOf(":"); //$NON-NLS-1$
			if (colonIndex == -1)
				throw new IllegalArgumentException(NLS.bind(Messages.action_syntax_error, statement));
			String name = nameValuePair.substring(0, colonIndex).trim();
			String value = nameValuePair.substring(colonIndex + 1).trim();
			parameters.put(name, value);
		}
		return new ParameterizedProvisioningAction(action, parameters, statement);
	}

	private ProvisioningAction lookupAction(String actionId, Map<String, ActionEntry> importMap, ITouchpointType touchpointType) {
		VersionRange versionRange = null;
		ActionEntry actionEntry = importMap.get(actionId);
		if (actionEntry != null) {
			actionId = actionEntry.actionId;
			versionRange = actionEntry.versionRange;
		}

		actionId = actionManager.getTouchpointQualifiedActionId(actionId, touchpointType);
		ProvisioningAction action = actionManager.getAction(actionId, versionRange);
		if (action == null)
			action = new MissingAction(actionId, versionRange);

		return action;
	}
}

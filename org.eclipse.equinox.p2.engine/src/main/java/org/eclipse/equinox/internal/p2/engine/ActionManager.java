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
package org.eclipse.equinox.internal.p2.engine;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.osgi.util.NLS;

public class ActionManager implements IRegistryChangeListener {

	private static final String PT_ACTIONS = "actions"; //$NON-NLS-1$
	private static final String ELEMENT_ACTION = "action"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String TOUCHPOINT_TYPE = "touchpointType"; //$NON-NLS-1$
	private static final String TOUCHPOINT_VERSION = "touchpointVersion"; //$NON-NLS-1$
	/**
	 * Service name constant for the action manager service. This service is used internally
	 * by the engine implementation and should not be referenced directly by clients.
	 */
	public static final String SERVICE_NAME = ActionManager.class.getName();

	private HashMap<String, IConfigurationElement> actionMap;
	private TouchpointManager touchpointManager;

	public ActionManager() {
		this.touchpointManager = new TouchpointManager();
		RegistryFactory.getRegistry().addRegistryChangeListener(this, EngineActivator.ID);
	}

	public Touchpoint getTouchpointPoint(ITouchpointType type) {
		if (type == null || type == ITouchpointType.NONE)
			return null;
		return touchpointManager.getTouchpoint(type);
	}

	public String getTouchpointQualifiedActionId(String actionId, ITouchpointType type) {
		if (actionId.indexOf('.') == -1) {
			if (type == null || type == ITouchpointType.NONE)
				return actionId;

			Touchpoint touchpoint = touchpointManager.getTouchpoint(type);
			if (touchpoint == null)
				throw new IllegalArgumentException(NLS.bind(Messages.ActionManager_Required_Touchpoint_Not_Found, type.toString(), actionId));
			actionId = touchpoint.qualifyAction(actionId);
		}
		return actionId;
	}

	public ProvisioningAction getAction(String actionId, VersionRange versionRange) {
		IConfigurationElement actionElement = getActionMap().get(actionId);
		if (actionElement != null && actionElement.isValid()) {
			try {
				ProvisioningAction action = (ProvisioningAction) actionElement.createExecutableExtension(ATTRIBUTE_CLASS);

				String touchpointType = actionElement.getAttribute(TOUCHPOINT_TYPE);
				if (touchpointType != null) {
					String touchpointVersion = actionElement.getAttribute(TOUCHPOINT_VERSION);
					Touchpoint touchpoint = touchpointManager.getTouchpoint(touchpointType, touchpointVersion);
					if (touchpoint == null)
						throw new IllegalArgumentException(NLS.bind(Messages.ActionManager_Required_Touchpoint_Not_Found, touchpointType, actionId));
					action.setTouchpoint(touchpoint);
				}
				return action;
			} catch (InvalidRegistryObjectException e) {
				// skip
			} catch (CoreException e) {
				throw new IllegalArgumentException(NLS.bind(Messages.ActionManager_Exception_Creating_Action_Extension, actionId));
			}
		}
		return null;
	}

	private synchronized Map<String, IConfigurationElement> getActionMap() {
		if (actionMap != null)
			return actionMap;
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(EngineActivator.ID, PT_ACTIONS);
		IExtension[] extensions = point.getExtensions();
		actionMap = new HashMap<String, IConfigurationElement>(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			try {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					IConfigurationElement actionElement = elements[j];
					if (!actionElement.getName().equals(ELEMENT_ACTION))
						continue;

					String actionId = actionElement.getAttribute(ATTRIBUTE_NAME);
					if (actionId == null)
						continue;

					if (actionId.indexOf('.') == -1)
						actionId = actionElement.getNamespaceIdentifier() + "." + actionId; //$NON-NLS-1$

					actionMap.put(actionId, actionElement);
				}
			} catch (InvalidRegistryObjectException e) {
				// skip
			}
		}
		return actionMap;
	}

	public synchronized void registryChanged(IRegistryChangeEvent event) {
		actionMap = null;
	}

	static void reportError(String errorMsg) {
		Status errorStatus = new Status(IStatus.ERROR, EngineActivator.ID, 1, errorMsg, null);
		LogHelper.log(errorStatus);
	}

}

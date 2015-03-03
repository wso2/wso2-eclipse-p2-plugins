/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class Property extends Phase {

	public class ProfilePropertyAction extends ProvisioningAction {

		public IStatus execute(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			PropertyOperand propertyOperand = (PropertyOperand) parameters.get(PARM_OPERAND);

			if (propertyOperand.second() == null)
				removeProfileProperty(profile, propertyOperand);
			else
				setProfileProperty(profile, propertyOperand, false);

			return null;
		}

		public IStatus undo(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			PropertyOperand propertyOperand = (PropertyOperand) parameters.get(PARM_OPERAND);

			if (propertyOperand.first() == null)
				removeProfileProperty(profile, propertyOperand);
			else
				setProfileProperty(profile, propertyOperand, true);

			return null;
		}

		private void setProfileProperty(Profile profile, PropertyOperand propertyOperand, boolean undo) {

			String value = (String) (undo ? propertyOperand.first() : propertyOperand.second());

			if (propertyOperand instanceof InstallableUnitPropertyOperand) {
				InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
				profile.setInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), value);
			} else {
				profile.setProperty(propertyOperand.getKey(), value);
			}
		}

		private void removeProfileProperty(Profile profile, PropertyOperand propertyOperand) {
			if (propertyOperand instanceof InstallableUnitPropertyOperand) {
				InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
				profile.removeInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey());
			} else {
				profile.removeProperty(propertyOperand.getKey());
			}
		}
	}

	public class UpdateInstallableUnitProfilePropertiesAction extends ProvisioningAction {

		// we do not need to use a memento here since the profile is not persisted unless the operation is successful
		Map<String, String> originalSourceProperties;
		Map<String, String> originalTargetProperties;

		public IStatus execute(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			originalSourceProperties = profile.getInstallableUnitProperties(source);

			IInstallableUnit target = iuOperand.second();
			originalTargetProperties = profile.getInstallableUnitProperties(target);

			profile.addInstallableUnitProperties(target, originalSourceProperties);
			profile.clearInstallableUnitProperties(source);

			return null;
		}

		public IStatus undo(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			profile.clearInstallableUnitProperties(source);
			profile.addInstallableUnitProperties(source, originalSourceProperties);

			IInstallableUnit target = iuOperand.second();
			profile.clearInstallableUnitProperties(target);
			profile.addInstallableUnitProperties(target, originalTargetProperties);

			return null;
		}
	}

	public class RemoveInstallableUnitProfilePropertiesAction extends ProvisioningAction {

		// we do not need to use a memento here since the profile is not persisted unless the operation is successful
		Map<String, String> originalSourceProperties;
		Map<String, String> originalTargetProperties;

		public IStatus execute(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			originalSourceProperties = profile.getInstallableUnitProperties(source);
			profile.clearInstallableUnitProperties(source);

			return null;
		}

		public IStatus undo(Map<String, Object> parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			profile.clearInstallableUnitProperties(source);
			profile.addInstallableUnitProperties(source, originalSourceProperties);

			return null;
		}
	}

	public Property(int weight) {
		super(PhaseSetFactory.PHASE_PROPERTY, weight);
	}

	protected List<ProvisioningAction> getActions(Operand operand) {
		if (operand instanceof PropertyOperand)
			return Collections.<ProvisioningAction> singletonList(new ProfilePropertyAction());

		if (operand instanceof InstallableUnitOperand) {
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
			if (iuOperand.first() != null) {
				if (iuOperand.second() != null) {
					return Collections.<ProvisioningAction> singletonList(new UpdateInstallableUnitProfilePropertiesAction());
				}
				return Collections.<ProvisioningAction> singletonList(new RemoveInstallableUnitProfilePropertiesAction());
			}
		}
		return null;
	}
}

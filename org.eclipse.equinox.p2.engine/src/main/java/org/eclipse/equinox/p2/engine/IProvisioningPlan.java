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
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A provisioning plan describes a proposed set of changes to a profile. The
 * proposed changes may represent a valid and consistent set of changes, or it
 * may represent a set of changes that would cause errors if executed. In this
 * case the plan contains information about the severity and explanation for the
 * problems.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IProvisioningPlan {

	/**
	 * Returns the proposed set of installable units to be added to the profile.
	 * 
	 * @return The proposed profile additions
	 */
	public IQueryable<IInstallableUnit> getAdditions();

	/**
	 * Returns the provisioning context in which this plan was created.
	 * 
	 * @return The plan's provisioning context
	 */
	public ProvisioningContext getContext();

	/**
	 * Returns a plan describing the proposed set of changes to the provisioning infrastructure
	 * required by this plan.  The installer changes must be performed before this plan 
	 * can be successfully executed.
	 * 
	 * @return The installer plan.
	 */
	public IProvisioningPlan getInstallerPlan();

	/**
	 * Returns the profile that this plan will operate on.
	 * 
	 * @return The target profile for this plan
	 */
	public IProfile getProfile();

	/**
	 * Returns the set of IUs that will constitute the profile if the plan is executed successfully.
	 * 
	 * @return The set of the IUs that will constitute the profile after the plan is executed successfully, or @null if the 
	 * plan is in error or the value has not been set.
	 * @since 2.2
	 */
	public IQueryable<IInstallableUnit> getFutureState();

	/**
	 * Returns the proposed set of installable units to be removed from this profile.
	 * 
	 * @return The proposed profile removals.
	 */
	public IQueryable<IInstallableUnit> getRemovals();

	/**
	 * Returns the overall plan status. The severity of this status indicates
	 * whether the plan can be successfully executed or not:
	 * <ul>
	 * <li>A status of {@link IStatus#OK} indicates that the plan can be executed successfully.</li>
	 * <li>A status of {@link IStatus#INFO} or {@link IStatus#WARNING} indicates
	 * that the plan can be executed but may cause problems.</li>
	 * <li>A status of {@link IStatus#ERROR} indicates that the plan cannot be executed
	 * successfully.</li>
	 * <li>A status of {@link IStatus#CANCEL} indicates that the plan computation was
	 * canceled and is incomplete. A canceled plan cannot be executed.</li>
	 * </ul>
	 * 
	 * @return The overall plan status.
	 */
	public IStatus getStatus();

	/**
	 * Adds an installable unit to the plan. This will cause the given installable unit
	 * to be installed into the profile when this plan is executed by the engine. 
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param iu the installable unit to add
	 */
	public void addInstallableUnit(IInstallableUnit iu);

	/**
	 * Removes an installable unit from the plan. This will cause the given installable unit
	 * to be remove from the profile when this plan is executed by the engine. 
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param iu the installable unit to add
	 */
	public void removeInstallableUnit(IInstallableUnit iu);

	/**
	 * Adds a profile property corresponding to the given installable unit to the plan. 
	 * This will cause the given installable unit property to be installed into the profile 
	 * when this plan is executed by the engine. 
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param iu the installable unit to set a property for
	 * @param name the property name
	 * @param value the property value
	 */
	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String name, String value);

	/**
	 * Sets the installer plan for this plan. The installer plan describes the set of changes
	 * that must be made to the provisioning agent in order for this plan to execute
	 * successfully.
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param installerPlan the plan describing changes to the provisioning agent
	 */
	public void setInstallerPlan(IProvisioningPlan installerPlan);

	/**
	 * Sets a profile property in the plan. This will cause the given property
	 * to be added to the profile when this plan is executed by the engine. 
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param name the profile property name
	 * @param value the profile property value
	 */
	public void setProfileProperty(String name, String value);

	/**
	 * Sets the overall plan status, describing whether the planner constructing
	 * this plan believes it will install successfully, or whether it contains errors
	 * or the plan computation has been canceled.
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param status the plan status
	 */
	public void setStatus(IStatus status);

	/**
	 * Adds an instruction to replace one installable unit in the profile with another.
	 * This will cause the 'from' installable unit property to be uninstalled from the profile 
	 * and the 'to' installable unit to be added to the profile when this plan is executed 
	 * by the engine. 
	 * <p>
	 * This is an advanced operation that should only be performed by clients crafting
	 * their own custom plan. Most clients should instead use a planner service
	 * to construct a valid plan based on a profile change request.
	 * </p>
	 * @param from the installable unit to remove
	 * @param to the installable unit to add
	 */
	public void updateInstallableUnit(IInstallableUnit from, IInstallableUnit to);

	/**
	 * Sets the value that is returned by the method getFutureState.
	 * Note that this method is a simple setter and will not cause any update to the other fields of this object.
	 * This field can be set to @null.   
	 * @param futureState A set of IU representing the future plan if the plan is executed successfully.
	 * @since 2.2 
	 */
	public void setFuturePlan(IQueryable<IInstallableUnit> futureState);
}
/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Ericsson AB - Bug 400011 - [shared] Cleanup the SurrogateProfileHandler code
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

public class SurrogateProfileHandler implements ISurrogateProfileHandler {

	private static final String NATIVE_TOUCHPOINT_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	private static final String P2_ENGINE_DIR = "p2/" + EngineActivator.ID + "/"; //$NON-NLS-1$//$NON-NLS-2$
	private static final String OSGI_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	private static final String ECLIPSE_INI_IGNORED = "eclipse.ini.ignored"; //$NON-NLS-1$
	private static final String IU_LOCKED = Integer.toString(IProfile.LOCK_UNINSTALL | IProfile.LOCK_UPDATE);
	private static final String PROP_SURROGATE = "org.eclipse.equinox.p2.surrogate"; //$NON-NLS-1$
	private static final String PROP_BASE = "org.eclipse.equinox.p2.base"; //$NON-NLS-1$
	private static final String STRICT = "STRICT"; //$NON-NLS-1$
	private static final String PROP_INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$

	private final IProvisioningAgent agent;

	private SimpleProfileRegistry profileRegistry;

	private SoftReference<IProfile> cachedProfile;

	private static void addSharedProfileBaseIUs(final IProfile sharedProfile, final Profile userProfile) {
		IQuery<IInstallableUnit> rootIUQuery = QueryUtil.createMatchQuery( //
				"profileProperties[$0] == 'true' || (touchpointType != null && touchpointType.id == $1)",//$NON-NLS-1$
				IProfile.PROP_PROFILE_ROOT_IU, NATIVE_TOUCHPOINT_TYPE);
		IQueryResult<IInstallableUnit> rootIUs = sharedProfile.query(rootIUQuery, null);
		for (Iterator<IInstallableUnit> iterator = rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			userProfile.addInstallableUnit(iu);
			userProfile.addInstallableUnitProperties(iu, sharedProfile.getInstallableUnitProperties(iu));
			userProfile.setInstallableUnitProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU, IU_LOCKED);
			userProfile.setInstallableUnitProperty(iu, PROP_BASE, Boolean.TRUE.toString());
		}

		IInstallableUnit sharedProfileIU = createSharedProfileIU(sharedProfile);
		userProfile.addInstallableUnit(sharedProfileIU);
		userProfile.setInstallableUnitProperty(sharedProfileIU, PROP_INCLUSION_RULES, STRICT);
		userProfile.setInstallableUnitProperty(sharedProfileIU, PROP_BASE, Boolean.TRUE.toString());
	}

	private static IInstallableUnit createSharedProfileIU(final IProfile sharedProfile) {
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("SharedProfile_" + sharedProfile.getProfileId()); //$NON-NLS-1$
		iuDescription.setVersion(Version.createOSGi(1, 0, 0, Long.toString(sharedProfile.getTimestamp())));

		ArrayList<IProvidedCapability> iuCapabilities = new ArrayList<IProvidedCapability>();
		IProvidedCapability selfCapability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iuDescription.getId(), iuDescription.getVersion());
		iuCapabilities.add(selfCapability);
		iuDescription.addProvidedCapabilities(iuCapabilities);

		ArrayList<IRequirement> iuRequirements = new ArrayList<IRequirement>();
		IQueryResult<IInstallableUnit> allIUs = sharedProfile.query(QueryUtil.createIUAnyQuery(), null);
		for (Iterator<IInstallableUnit> iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			IMatchExpression<IInstallableUnit> iuMatcher = ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(ExpressionUtil.parse("id == $0 && version == $1"), iu.getId(), iu.getVersion()); //$NON-NLS-1$
			iuRequirements.add(MetadataFactory.createRequirement(iuMatcher, null, 0, 1, true));
		}
		iuDescription.addRequirements(iuRequirements);
		iuDescription.setProperty(IInstallableUnit.PROP_NAME, NLS.bind(Messages.Shared_Profile, null));

		IInstallableUnit sharedProfileIU = MetadataFactory.createInstallableUnit(iuDescription);
		return sharedProfileIU;
	}

	private static void updateProperties(final IProfile sharedProfile, Profile userProfile) {
		Location installLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		File installFolder = new File(installLocation.getURL().getPath());

		if (Boolean.valueOf(sharedProfile.getProperty(IProfile.PROP_ROAMING)).booleanValue()) {
			userProfile.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.getAbsolutePath());
			userProfile.setProperty(IProfile.PROP_SHARED_CACHE, installFolder.getAbsolutePath());
			userProfile.setProperty(IProfile.PROP_ROAMING, Boolean.FALSE.toString());
		} else {
			String cache = sharedProfile.getProperty(IProfile.PROP_CACHE);
			if (cache != null)
				userProfile.setProperty(IProfile.PROP_SHARED_CACHE, cache);
		}

		Location configurationLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.CONFIGURATION_FILTER);
		File configurationFolder = new File(configurationLocation.getURL().getPath());
		userProfile.setProperty(IProfile.PROP_CONFIGURATION_FOLDER, configurationFolder.getAbsolutePath());

		// We need to check that the configuration folder is not a file system root. 
		// some of the profiles resources are stored as siblings to the configuration folder.
		// also see bug 230384
		if (configurationFolder.getParentFile() == null)
			throw new IllegalArgumentException("Configuration folder must not be a file system root."); //$NON-NLS-1$

		userProfile.setProperty(IProfile.PROP_CACHE, configurationFolder.getParentFile().getAbsolutePath());

		File launcherConfigFile = new File(configurationFolder, ECLIPSE_INI_IGNORED);
		userProfile.setProperty(IProfile.PROP_LAUNCHER_CONFIGURATION, launcherConfigFile.getAbsolutePath());
	}

	public SurrogateProfileHandler(IProvisioningAgent agent) {
		this.agent = agent;
	}

	private synchronized SimpleProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			String installArea = EngineActivator.getContext().getProperty(OSGI_INSTALL_AREA);
			try {
				URL registryURL = new URL(installArea + P2_ENGINE_DIR + SimpleProfileRegistry.DEFAULT_STORAGE_DIR);
				File sharedRegistryDirectory = URIUtil.toFile(URIUtil.toURI(registryURL));
				profileRegistry = new SimpleProfileRegistry(agent, sharedRegistryDirectory, null, false);
			} catch (MalformedURLException e) {
				//this is not possible because we know the above URL is valid
			} catch (URISyntaxException e) {
				//this is not possible because we know the above URL is valid
			}
		}
		return profileRegistry;
	}

	// this method must not try to lock the profile registry
	private IProfile getSharedProfile(String id) {
		SimpleProfileRegistry registry = getProfileRegistry();
		long[] timestamps = registry.listProfileTimestamps(id);
		if (timestamps.length == 0)
			return null;

		long currentTimestamp = timestamps[timestamps.length - 1];

		//see if we have a cached profile
		if (cachedProfile != null) {
			IProfile profile = cachedProfile.get();
			if (profile != null && profile.getProfileId().equals(id) && profile.getTimestamp() == currentTimestamp)
				return profile;
		}

		final IProfile profile = registry.getProfile(id, currentTimestamp);
		if (profile != null)
			cachedProfile = new SoftReference<IProfile>(profile);
		return profile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#createProfile(java.lang.String)
	 */
	public IProfile createProfile(String id) {
		final IProfile sharedProfile = getSharedProfile(id);
		if (sharedProfile == null)
			return null;

		Profile userProfile = new Profile(agent, id, null, sharedProfile.getProperties());
		userProfile.setProperty(PROP_SURROGATE, Boolean.TRUE.toString());
		userProfile.setSurrogateProfileHandler(this);
		updateProperties(sharedProfile, userProfile);
		addSharedProfileBaseIUs(sharedProfile, userProfile);
		return userProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#isSurrogate(org.eclipse.equinox.internal.provisional.p2.engine.IProfile)
	 */
	public boolean isSurrogate(IProfile profile) {
		return Boolean.valueOf(profile.getProperty(PROP_SURROGATE)).booleanValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#queryProfile(org.eclipse.equinox.internal.provisional.p2.engine.IProfile, org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.equinox.internal.provisional.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> queryProfile(IProfile profile, IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		IProfile sharedProfile = getSharedProfile(profile.getProfileId());
		if (sharedProfile == null)
			return profile.query(query, monitor);

		// TODO: Should consider using a sequenced iterator here instead of collecting
		Collector<IInstallableUnit> result = new Collector<IInstallableUnit>();
		result.addAll(sharedProfile.query(query, monitor));
		result.addAll(profile.query(query, monitor));
		return result;
	}
}

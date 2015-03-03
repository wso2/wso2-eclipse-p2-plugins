/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.osgi.util.NLS;

public class ProfileMetadataRepository extends AbstractMetadataRepository {

	private static final String DEFAULT_ARTIFACT_REPO_DIRECTORY = "org.eclipse.equinox.p2.core/cache"; //$NON-NLS-1$
	private static final String ARTIFACTS_XML = "artifacts.xml"; //$NON-NLS-1$
	private static final String FILE_SCHEME = "file"; //$NON-NLS-1$
	private static final String DOT_PROFILE = ".profile"; //$NON-NLS-1$
	public static final String TYPE = "org.eclipse.equinox.p2.engine.repo.metadataRepository"; //$NON-NLS-1$
	public static final Integer VERSION = new Integer(1);
	private IProfile profile;
	private HashSet<IRepositoryReference> repositories = new HashSet<IRepositoryReference>();

	public ProfileMetadataRepository(IProvisioningAgent agent, URI location, IProgressMonitor monitor) throws ProvisionException {
		super(agent, location.toString(), TYPE, VERSION.toString(), location, null, null, null);

		try {
			profile = getProfile(agent, location);
		} catch (RuntimeException e) {
			throw new ProvisionException(new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.REPOSITORY_FAILED_READ, e.getMessage(), e));
		}
		publishArtifactRepos();
	}

	private void publishArtifactRepos() {
		List<URI> artifactRepos = findArtifactRepos();

		IProvisioningEventBus bus = (IProvisioningEventBus) getProvisioningAgent().getService(IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		for (URI repo : artifactRepos) {
			repositories.add(new RepositoryReference(repo, null, IRepository.TYPE_ARTIFACT, IRepository.ENABLED));
			bus.publishEvent(new RepositoryEvent(repo, IRepository.TYPE_ARTIFACT, RepositoryEvent.DISCOVERED, true));
		}
	}

	private List<URI> findArtifactRepos() {
		List<URI> artifactRepos = new ArrayList<URI>();
		File p2Directory = findP2Directory();

		// Add the profile registry's default agent artifact repository.
		// Currently this is used by the Native Touchpoint to store artifacts however
		// other touchpoints might use this as well.
		File agentArtifactRepository = findAgentArtifactRepositoryDirectory(p2Directory);
		if (agentArtifactRepository != null)
			artifactRepos.add(agentArtifactRepository.toURI());

		// bundle pool
		String bundlePool = profile.getProperty(IProfile.PROP_CACHE);
		if (bundlePool != null) {
			File bundlePoolFile = new File(bundlePool);
			if (bundlePoolFile.exists())
				artifactRepos.add(bundlePoolFile.toURI());
			else if (Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue()) {
				// the profile has not been used yet but is a roaming profile
				// best effort to add "just" the default bundle pool
				bundlePoolFile = findDefaultBundlePool(p2Directory);
				if (bundlePoolFile != null)
					artifactRepos.add(bundlePoolFile.toURI());
				return artifactRepos;
			}
		}

		// shared bundle pool
		String sharedBundlePool = profile.getProperty(IProfile.PROP_SHARED_CACHE);
		if (sharedBundlePool != null)
			artifactRepos.add(new File(sharedBundlePool).toURI());

		// cache extensions
		// Currently set exclusively by dropins
		String dropinRepositories = profile.getProperty("org.eclipse.equinox.p2.cache.extensions"); //$NON-NLS-1$
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, "|"); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String repoLocation = ""; //$NON-NLS-1$
				try {
					repoLocation = tokenizer.nextToken();
					artifactRepos.add(new URI(repoLocation));
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "invalid repo reference with location: " + repoLocation, e)); //$NON-NLS-1$
				}
			}
		}
		return artifactRepos;
	}

	private File findAgentArtifactRepositoryDirectory(File p2Directory) {
		if (p2Directory == null)
			return null;

		File agentArtifactRepositoryDirectory = new File(p2Directory, DEFAULT_ARTIFACT_REPO_DIRECTORY);
		if (!agentArtifactRepositoryDirectory.isDirectory())
			return null;

		return agentArtifactRepositoryDirectory;
	}

	private File findDefaultBundlePool(File p2Directory) {
		if (p2Directory == null)
			return null;

		File productDirectory = p2Directory.getParentFile();
		if (productDirectory == null || !(new File(productDirectory, ARTIFACTS_XML).exists()))
			return null;

		return productDirectory;
	}

	private File findP2Directory() {
		File target = new File(getLocation());
		if (target.isFile())
			target = target.getParentFile();

		// by default the profile registry is in {product}/p2/org.eclipse.equinox.p2.engine/profileRegistry
		// the default bundle pool is in the {product} folder
		File profileRegistryDirectory = target.getParentFile();
		if (profileRegistryDirectory == null)
			return null;

		File p2EngineDirectory = profileRegistryDirectory.getParentFile();
		if (p2EngineDirectory == null)
			return null;

		return p2EngineDirectory.getParentFile();
	}

	public Collection<IRepositoryReference> getReferences() {
		return Collections.unmodifiableCollection(repositories);
	}

	public void initialize(RepositoryState state) {
		// nothing to do
	}

	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return profile.query(query, monitor);
	}

	private static IProfile getProfile(IProvisioningAgent agent, URI location) throws ProvisionException {
		if (!FILE_SCHEME.equalsIgnoreCase(location.getScheme()))
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		File target = new File(location);
		if (!target.exists())
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		long timestamp = -1;
		int index = target.getName().lastIndexOf(DOT_PROFILE);
		if (index == -1)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		String profileId = target.getName().substring(0, index);
		if (target.isFile()) {
			try {
				timestamp = Long.parseLong(profileId);
			} catch (NumberFormatException e) {
				fail(location, ProvisionException.REPOSITORY_FAILED_READ);
			}
			target = target.getParentFile();
			if (target == null)
				fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
			index = target.getName().lastIndexOf(DOT_PROFILE);
			profileId = target.getName().substring(0, index);
		}
		profileId = SimpleProfileRegistry.unescape(profileId);

		File registryDirectory = target.getParentFile();
		if (registryDirectory == null)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);
		SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(agent, registryDirectory, null, false);
		if (timestamp == -1) {
			long[] timestamps = profileRegistry.listProfileTimestamps(profileId);
			timestamp = timestamps[timestamps.length - 1];
		}
		IProfile profile = profileRegistry.getProfile(profileId, timestamp);
		if (profile == null)
			fail(location, ProvisionException.REPOSITORY_NOT_FOUND);

		return profile;
	}

	private static void fail(URI location, int code) throws ProvisionException {
		switch (code) {
			case ProvisionException.REPOSITORY_NOT_FOUND :
				String msg = NLS.bind(Messages.io_NotFound, location);
				throw new ProvisionException(new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
			case ProvisionException.REPOSITORY_FAILED_READ :
				msg = NLS.bind(Messages.io_FailedRead, location);
				throw new ProvisionException(new Status(IStatus.ERROR, EngineActivator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, null));
		}
	}
}

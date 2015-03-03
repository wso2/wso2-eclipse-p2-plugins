/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 * 	IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *	Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.DebugHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * A provisioning context defines the scope in which a provisioning operation
 * occurs. A context can be used to specify the set of repositories available
 * to the planner and engine as they perform provisioning work.
 * @since 2.0
 */
public class ProvisioningContext {
	private IProvisioningAgent agent;
	private URI[] artifactRepositories; //artifact repositories to consult
	private final List<IInstallableUnit> extraIUs = Collections.synchronizedList(new ArrayList<IInstallableUnit>());
	private URI[] metadataRepositories; //metadata repositories to consult
	private final Map<String, String> properties = new HashMap<String, String>();
	private Map<String, URI> referencedArtifactRepositories = null;

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

	class ArtifactRepositoryQueryable implements IQueryable<IArtifactRepository> {
		List<IArtifactRepository> repositories;

		ArtifactRepositoryQueryable(List<IArtifactRepository> repositories) {
			this.repositories = repositories;
		}

		public IQueryResult<IArtifactRepository> query(IQuery<IArtifactRepository> query, IProgressMonitor mon) {
			return query.perform(repositories.listIterator());
		}
	}

	/**
	 * This Comparator sorts the repositories such that local repositories are first
	 */
	private static final Comparator<URI> LOCAL_FIRST_COMPARATOR = new Comparator<URI>() {

		public int compare(URI arg0, URI arg1) {
			String protocol0 = arg0.getScheme();
			String protocol1 = arg1.getScheme();

			if (FILE_PROTOCOL.equals(protocol0) && !FILE_PROTOCOL.equals(protocol1))
				return -1;
			if (!FILE_PROTOCOL.equals(protocol0) && FILE_PROTOCOL.equals(protocol1))
				return 1;
			return 0;
		}
	};

	/**
	 * Instructs the provisioning context to follow metadata repository references when 
	 * providing queryables for obtaining metadata and artifacts.  When this property is set to
	 * "true", then metadata repository references that are encountered while loading the 
	 * specified metadata repositories will be included in the provisioning
	 * context.  
	 *
	 * @see #getMetadata(IProgressMonitor)
	 * @see #setMetadataRepositories(URI[])
	 */
	public static final String FOLLOW_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followRepositoryReferences"; //$NON-NLS-1$

	private static final String FOLLOW_ARTIFACT_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followArtifactRepositoryReferences"; //$NON-NLS-1$

	/**
	 * Creates a new provisioning context that includes all available metadata and
	 * artifact repositories available to the specified provisioning agent.
	 *
	 * @param agent the provisioning agent from which to obtain any necessary services.
	 */
	public ProvisioningContext(IProvisioningAgent agent) {
		this.agent = agent;
		// null repos means look at them all
		metadataRepositories = null;
		artifactRepositories = null;
		setProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, Boolean.TRUE.toString());
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact keys that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact keys.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactKey> getArtifactKeys(IProgressMonitor monitor) {
		return QueryUtil.compoundQueryable(getLoadedArtifactRepositories(monitor));
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact descriptors that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact descriptors.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactDescriptor> getArtifactDescriptors(IProgressMonitor monitor) {
		List<IArtifactRepository> repos = getLoadedArtifactRepositories(monitor);
		List<IQueryable<IArtifactDescriptor>> descriptorQueryables = new ArrayList<IQueryable<IArtifactDescriptor>>();
		for (IArtifactRepository repo : repos) {
			descriptorQueryables.add(repo.descriptorQueryable());
		}
		return QueryUtil.compoundQueryable(descriptorQueryables);
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact repositories that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact repositories.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactRepository> getArtifactRepositories(IProgressMonitor monitor) {
		return new ArtifactRepositoryQueryable(getLoadedArtifactRepositories(monitor));
	}

	/**
	 * Return an array of loaded artifact repositories.
	 */
	private List<IArtifactRepository> getLoadedArtifactRepositories(IProgressMonitor monitor) {
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = artifactRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : artifactRepositories;
		Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);

		List<IArtifactRepository> repos = new ArrayList<IArtifactRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, (repositories.length + 1) * 100);
		for (int i = 0; i < repositories.length; i++) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			try {
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			} catch (ProvisionException e) {
				//skip unreadable repositories
			}
			// Remove this URI from the list of extra references if it is there.
			if (referencedArtifactRepositories != null)
				referencedArtifactRepositories.remove(repositories[i]);
		}
		// Are there any extra artifact repository references to consider?
		if (referencedArtifactRepositories != null && referencedArtifactRepositories.size() > 0 && shouldFollowArtifactReferences()) {
			SubMonitor innerSub = SubMonitor.convert(sub.newChild(100), referencedArtifactRepositories.size() * 100);
			for (URI referencedURI : referencedArtifactRepositories.values()) {
				try {
					repos.add(repoManager.loadRepository(referencedURI, innerSub.newChild(100)));
				} catch (ProvisionException e) {
					// skip unreadable repositories
				}
			}
		}
		return repos;
	}

	private Set<IMetadataRepository> getLoadedMetadataRepositories(IProgressMonitor monitor) {
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repositories = metadataRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : metadataRepositories;

		HashMap<String, IMetadataRepository> repos = new HashMap<String, IMetadataRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);

		// Clear out the list of remembered artifact repositories
		referencedArtifactRepositories = new HashMap<String, URI>();
		for (int i = 0; i < repositories.length; i++) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			loadMetadataRepository(repoManager, repositories[i], repos, shouldFollowReferences(), sub.newChild(100));
		}
		Set<IMetadataRepository> set = new HashSet<IMetadataRepository>();
		set.addAll(repos.values());
		return set;
	}

	private void loadMetadataRepository(IMetadataRepositoryManager manager, URI location, HashMap<String, IMetadataRepository> repos, boolean followMetadataRepoReferences, IProgressMonitor monitor) {
		// if we've already processed this repo, don't do it again.  This keeps us from getting
		// caught up in circular references.
		if (repos.containsKey(location.toString()))
			return;

		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		// First load the repository itself.
		IMetadataRepository repository;
		try {
			repository = manager.loadRepository(location, sub.newChild(500));
		} catch (ProvisionException e) {
			// nothing more to do
			return;
		}
		repos.put(location.toString(), repository);
		Collection<IRepositoryReference> references = repository.getReferences();
		// We always load artifact repositories referenced by this repository.  We might load
		// metadata repositories
		if (references.size() > 0) {
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			SubMonitor repoSubMon = SubMonitor.convert(sub.newChild(500), 100 * references.size());
			for (IRepositoryReference ref : references) {
				try {
					if (ref.getType() == IRepository.TYPE_METADATA && followMetadataRepoReferences && isEnabled(manager, ref)) {
						loadMetadataRepository(manager, ref.getLocation(), repos, followMetadataRepoReferences, repoSubMon.newChild(100));
					} else if (ref.getType() == IRepository.TYPE_ARTIFACT) {
						// We want to remember all enabled artifact repository locations.
						if (isEnabled(artifactManager, ref))
							referencedArtifactRepositories.put(ref.getLocation().toString(), ref.getLocation());
					}
				} catch (IllegalArgumentException e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=311338
					// ignore invalid location and keep going
				}
			}
		} else {
			sub.done();
		}

	}

	// If the manager knows about the repo, consider its enablement state in the manager.
	// If the manager does not know about the repo, consider the reference enablement state
	@SuppressWarnings("rawtypes")
	private boolean isEnabled(IRepositoryManager manager, IRepositoryReference reference) {
		return (manager.contains(reference.getLocation()) && manager.isEnabled(reference.getLocation())) || ((!manager.contains(reference.getLocation())) && ((reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED));
	}

	private boolean shouldFollowReferences() {
		return Boolean.valueOf(getProperty(FOLLOW_REPOSITORY_REFERENCES)).booleanValue();
	}

	private boolean shouldFollowArtifactReferences() {
		return Boolean.valueOf(getProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES)).booleanValue();
	}

	/**
	 * Returns a queryable that can be used to obtain any metadata (installable units)
	 * that are needed for the provisioning operation.
	 * 
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client retrieves
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * metadata repository locations or artifact repository locations once the
	 * metadata queryable has been retrieved.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available metadata.
	 *
	 * @see #setMetadataRepositories(URI[])
	 * @see #FOLLOW_REPOSITORY_REFERENCES
	 */
	public IQueryable<IInstallableUnit> getMetadata(IProgressMonitor monitor) {
		return QueryUtil.compoundQueryable(getLoadedMetadataRepositories(monitor));
	}

	/**
	 * Returns the list of additional installable units that should be considered as
	 * available for installation by the planner. Returns an empty list if
	 * there are no extra installable units to consider. This method has no effect on the
	 * execution of the engine.
	 *
	 * @return The extra installable units that are available
	 */
	public List<IInstallableUnit> getExtraInstallableUnits() {
		return extraIUs;
	}

	/**
	 * Returns the properties that are defined in this context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 *
	 * @return the defined provisioning context properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Returns the value of the property with the given key, or <code>null</code>
	 * if no such property is defined
	 * @param key the property key
	 * @return the property value, or <code>null</code>
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Sets the artifact repositories to consult when performing an operation.
	 * <p>
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client 
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * artifact repository locations once the metadata queryable has been retrieved.
	 *
	 * @param artifactRepositories the artifact repository locations
	*/
	public void setArtifactRepositories(URI[] artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	/**
	 * Sets the metadata repositories to consult when performing an operation.
	 * <p>
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client 
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * metadata repository locations once the metadata queryable has been retrieved.

	 * @param metadataRepositories the metadata repository locations
	*/
	public void setMetadataRepositories(URI[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/**
	 * Sets the list of additional installable units that should be considered as
	 * available for installation by the planner. This method has no effect on the
	 * execution of the engine.
	 * @param extraIUs the extra installable units
	 */
	public void setExtraInstallableUnits(List<IInstallableUnit> extraIUs) {
		this.extraIUs.clear();
		//copy the list to prevent future client tampering
		if (extraIUs != null)
			this.extraIUs.addAll(extraIUs);
	}

	/**
	 * Sets a property on this provisioning context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 * @param key the property key
	 * @param value the property value
	 */
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{artifactRepos=" + DebugHelper.formatArray(null != artifactRepositories ? Arrays.asList(artifactRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", metadataRepos=" + DebugHelper.formatArray(null != metadataRepositories ? Arrays.asList(metadataRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", properties=" + getProperties() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}
}

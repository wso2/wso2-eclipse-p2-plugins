/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.EventObject;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

public class CollectEvent extends EventObject {

	private static final long serialVersionUID = 5782796765127875200L;
	/**
	 * It means the overall collecting requests are started.
	 */
	public static final int TYPE_OVERALL_START = 1;
	/**
	 * It means the overall collecting requests are finished. 
	 */
	public static final int TYPE_OVERALL_END = 2;
	/**
	 * It means the collecting requests related to a special repository are started.
	 * See {@link CollectEvent#getRepository()}
	 */
	public static final int TYPE_REPOSITORY_START = 3;
	/**
	 * It means the collecting requests related to a special repository are end.
	 * See {@link CollectEvent#getRepository()} 
	 */
	public static final int TYPE_REPOSITORY_END = 4;

	private IArtifactRepository artifactRepo;
	private IArtifactRequest[] requests;

	private ProvisioningContext context;

	private int type;

	public CollectEvent(int type, IArtifactRepository artifactRepo, ProvisioningContext context, IArtifactRequest[] requests) {
		super(requests);
		this.type = type;
		this.artifactRepo = artifactRepo;
		this.context = context;
		this.requests = requests;
	}

	public int getType() {
		return type;
	}

	/**
	 * Return the associated repository if the downloading requests are related to a special repository
	 * @return the associated repository or <code>null</code> if the repository is unknown
	 */
	public IArtifactRepository getRepository() {
		return artifactRepo;
	}

	public IArtifactRequest[] getDownloadRequests() {
		return requests;
	}

	public ProvisioningContext getContext() {
		return context;
	}
}

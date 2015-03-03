/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Checks the certificates on a set of files or artifacts and reports back any problems
 * with unsigned artifacts, untrusted certificates, or tampered content.
 */
public class CertificateChecker {
	private static final String DEBUG_PREFIX = "certificate checker"; //$NON-NLS-1$

	private ArrayList<File> artifacts;
	private final IProvisioningAgent agent;

	public CertificateChecker() {
		this(null);
	}

	public CertificateChecker(IProvisioningAgent agent) {
		this.agent = agent;
		artifacts = new ArrayList<File>();
	}

	public IStatus start() {
		final BundleContext context = EngineActivator.getContext();
		ServiceReference<SignedContentFactory> contentFactoryRef = context.getServiceReference(SignedContentFactory.class);
		SignedContentFactory verifierFactory = context.getService(contentFactoryRef);
		try {
			return checkCertificates(verifierFactory);
		} finally {
			context.ungetService(contentFactoryRef);
		}
	}

	private IStatus checkCertificates(SignedContentFactory verifierFactory) {
		UIServices serviceUI = (UIServices) agent.getService(UIServices.SERVICE_NAME);
		SignedContent content = null;
		SignerInfo[] signerInfo = null;
		ArrayList<Certificate> untrusted = new ArrayList<Certificate>();
		ArrayList<File> unsigned = new ArrayList<File>();
		ArrayList<Certificate[]> untrustedChain = new ArrayList<Certificate[]>();
		Map<Certificate, Collection<File>> untrustedArtifacts = new HashMap<Certificate, Collection<File>>();
		IStatus status = Status.OK_STATUS;
		if (artifacts.size() == 0 || serviceUI == null)
			return status;
		for (File artifact : artifacts) {
			try {
				content = verifierFactory.getSignedContent(artifact);
				if (!content.isSigned()) {
					unsigned.add(artifact);
					continue;
				}
				signerInfo = content.getSignerInfos();
			} catch (GeneralSecurityException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentError, e);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.CertificateChecker_SignedContentIOError, e);
			}
			for (int i = 0; i < signerInfo.length; i++) {
				if (!signerInfo[i].isTrusted()) {
					Certificate[] certificateChain = signerInfo[i].getCertificateChain();
					if (!untrusted.contains(certificateChain[0])) {
						untrusted.add(certificateChain[0]);
						untrustedChain.add(certificateChain);
					}
					if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED) {
						if (untrustedArtifacts.containsKey(certificateChain[0])) {
							untrustedArtifacts.get(certificateChain[0]).add(artifact);
						} else {
							untrustedArtifacts.put(certificateChain[0], new ArrayList<File>(Arrays.asList(artifact)));
						}
					}
				}

				// log the unsigned artifacts if requested
				if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNSIGNED && !unsigned.isEmpty()) {
					StringBuffer message = new StringBuffer("The following artifacts are unsigned:\n"); //$NON-NLS-1$
					for (File file : unsigned) {
						message.append(NLS.bind("  {0}\n", file.getPath())); //$NON-NLS-1$
					}
					DebugHelper.debug(DEBUG_PREFIX, message.toString());
				}

				// log the untrusted certificates if requested
				if (DebugHelper.DEBUG_CERTIFICATE_CHECKER_UNTRUSTED && !untrusted.isEmpty()) {
					StringBuffer message = new StringBuffer("The following certificates are untrusted:\n"); //$NON-NLS-1$
					for (Certificate cert : untrustedArtifacts.keySet()) {
						message.append(cert.toString() + "\n"); //$NON-NLS-1$
						message.append("  used by the following artifacts:\n"); //$NON-NLS-1$
						for (File file : untrustedArtifacts.get(cert)) {
							message.append(NLS.bind("    {0}\n", file.getPath())); //$NON-NLS-1$
						}
					}
					DebugHelper.debug(DEBUG_PREFIX, message.toString());
				}
			}
		}
		String policy = getUnsignedContentPolicy();
		//if there is unsigned content and we should never allow it, then fail without further checking certificates
		if (!unsigned.isEmpty() && EngineActivator.UNSIGNED_FAIL.equals(policy))
			return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.CertificateChecker_UnsignedNotAllowed, unsigned));

		String[] details;
		// If we always allow unsigned content, or we don't have any, we don't prompt the user about it
		if (EngineActivator.UNSIGNED_ALLOW.equals(policy) || unsigned.isEmpty())
			details = null;
		else {
			details = new String[unsigned.size()];
			for (int i = 0; i < details.length; i++) {
				details[i] = unsigned.get(i).toString();
			}
		}
		Certificate[][] unTrustedCertificateChains;
		if (untrusted.isEmpty()) {
			unTrustedCertificateChains = null;
		} else {
			unTrustedCertificateChains = new Certificate[untrustedChain.size()][];
			for (int i = 0; i < untrustedChain.size(); i++) {
				unTrustedCertificateChains[i] = untrustedChain.get(i);
			}
		}

		// If there was no unsigned content, and nothing untrusted, no need to prompt.
		if (details == null && unTrustedCertificateChains == null)
			return status;

		TrustInfo trustInfo = serviceUI.getTrustInfo(unTrustedCertificateChains, details);

		// If user doesn't trust unsigned content, cancel the operation
		if (!trustInfo.trustUnsignedContent())
			return Status.CANCEL_STATUS;

		Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
		// If we had untrusted chains and nothing was trusted, cancel the operation
		if (unTrustedCertificateChains != null && trustedCertificates == null) {
			return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		}
		// Anything that was trusted should be removed from the untrusted list
		if (trustedCertificates != null) {
			for (int i = 0; i < trustedCertificates.length; i++) {
				untrusted.remove(trustedCertificates[i]);
			}
		}

		// If there is still untrusted content, cancel the operation
		if (untrusted.size() > 0)
			return new Status(IStatus.CANCEL, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		// If we should persist the trusted certificates, add them to the trust engine
		if (trustInfo.persistTrust())
			return persistTrustedCertificates(trustedCertificates);

		return status;
	}

	private IStatus persistTrustedCertificates(Certificate[] trustedCertificates) {
		if (trustedCertificates == null)
			// I'm pretty sure this would be a bug; trustedCertificates should never be null here.
			return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateRejected);
		ServiceTracker<TrustEngine, TrustEngine> trustEngineTracker = new ServiceTracker<TrustEngine, TrustEngine>(EngineActivator.getContext(), TrustEngine.class, null);
		trustEngineTracker.open();
		Object[] trustEngines = trustEngineTracker.getServices();
		try {
			if (trustEngines == null)
				return null;
			for (Certificate trustedCertificate : trustedCertificates) {
				for (Object engine : trustEngines) {
					TrustEngine trustEngine = (TrustEngine) engine;
					if (trustEngine.isReadOnly())
						continue;
					try {
						trustEngine.addTrustAnchor(trustedCertificate, trustedCertificate.toString());
						// this should mean we added an anchor successfully; continue to next certificate
						break;
					} catch (IOException e) {
						//just return an INFO so the user can proceed with the install
						return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_KeystoreConnectionError, e);
					} catch (GeneralSecurityException e) {
						return new Status(IStatus.INFO, EngineActivator.ID, Messages.CertificateChecker_CertificateError, e);
					}
				}
			}
		} finally {
			trustEngineTracker.close();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Return the policy on unsigned content.
	 */
	private String getUnsignedContentPolicy() {
		String policy = EngineActivator.getContext().getProperty(EngineActivator.PROP_UNSIGNED_POLICY);
		if (policy == null)
			policy = EngineActivator.UNSIGNED_PROMPT;
		return policy;

	}

	public void add(File toAdd) {
		artifacts.add(toAdd);
	}

	public void add(Object[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (toAdd[i] instanceof File)
				add((File) toAdd[i]);
		}
	}
}

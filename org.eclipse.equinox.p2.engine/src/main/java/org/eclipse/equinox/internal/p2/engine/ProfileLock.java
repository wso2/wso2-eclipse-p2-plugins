/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

/**
 * The purpose of this class is to enable cross process locking.
 * See 257654 for more details.
 */
public class ProfileLock {
	private static final String LOCK_FILENAME = ".lock"; //$NON-NLS-1$

	private final Location location;
	private final Object lock;
	private Thread lockHolder;
	private int waiting;

	public ProfileLock(Object lock, File profileDirectory) {
		this.lock = lock;
		location = createLockLocation(profileDirectory);
	}

	private static Location createLockLocation(File parent) {
		Location anyLoc = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName());
		try {
			final URL url = parent.toURL();
			Location location = anyLoc.createLocation(null, url, false);
			location.set(url, false, LOCK_FILENAME);
			return location;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.SimpleProfileRegistry_Bad_profile_location, e.getLocalizedMessage()));
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw new IllegalStateException(e.getLocalizedMessage());
		}
	}

	/**
	 * Asserts that this thread currently holds the profile lock.
	 * @throws IllegalStateException If this thread does not currently hold the profile lock
	 */
	public void checkLocked() {
		synchronized (lock) {
			if (lockHolder == null)
				throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);

			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);
		}
	}

	/**
	 * Attempts to obtain an exclusive write lock on a profile. The profile lock must be
	 * owned by any process and thread that wants to modify a profile. If the lock
	 * is currently held by another thread in this process, this method will  block until
	 * the lock becomes available. If the lock is currently held by another process,
	 * this method returns <code>false</code>. Re-entrant attempts to acquire the
	 * same profile lock multiple times in the same thread is not allowed.
	 * 
	 * @return <code>true</code> if the lock was successfully obtained by this thread,
	 * and <code>false</code> if another process is currently holding the lock.
	 */
	public boolean lock() {
		synchronized (lock) {
			Thread current = Thread.currentThread();
			if (lockHolder == current)
				throw new IllegalStateException(Messages.profile_lock_not_reentrant);

			boolean locationLocked = (waiting != 0);
			while (lockHolder != null) {
				locationLocked = true;
				waiting++;
				boolean interrupted = false;
				try {
					lock.wait();
				} catch (InterruptedException e) {
					interrupted = true;
				} finally {
					waiting--;
					// if interrupted restore interrupt to thread state
					if (interrupted)
						current.interrupt();
				}
			}
			try {
				if (!locationLocked && !location.lock())
					return false;

				lockHolder = current;
			} catch (IOException e) {
				throw new IllegalStateException(NLS.bind(Messages.SimpleProfileRegistry_Profile_not_locked_due_to_exception, e.getLocalizedMessage()));
			}
			return true;
		}
	}

	/**
	 * Releases the exclusive write lock on a profile. This method must only be called
	 * by a thread that currently owns the lock.
	 */
	public void unlock() {
		synchronized (lock) {
			if (lockHolder == null)
				throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);

			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);

			lockHolder = null;
			if (waiting == 0)
				location.release();
			else
				lock.notify();
		}
	}

	/**
	 * Returns whether a thread in this process currently holds the profile lock.
	 * 
	 * @return <code>true</code> if a thread in this process owns the profile lock,
	 * and <code>false</code> otherwise
	 */
	public boolean processHoldsLock() {
		synchronized (lock) {
			return lockHolder != null;
		}
	}
}
/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.query.QueryUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class ProfileWriter extends MetadataWriter implements ProfileXMLConstants {

	public ProfileWriter(OutputStream output, ProcessingInstruction[] processingInstructions) throws IOException {
		super(output, processingInstructions);
	}

	public void writeProfile(IProfile profile) {
		start(PROFILE_ELEMENT);
		attribute(ID_ATTRIBUTE, profile.getProfileId());
		attribute(TIMESTAMP_ATTRIBUTE, Long.toString(profile.getTimestamp()));
		writeProperties(profile.getProperties());
		ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>(profile.query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet());
		Collections.sort(ius, new Comparator<IInstallableUnit>() {
			public int compare(IInstallableUnit iu1, IInstallableUnit iu2) {
				int IdCompare = iu1.getId().compareTo(iu2.getId());
				if (IdCompare != 0)
					return IdCompare;

				return iu1.getVersion().compareTo(iu2.getVersion());
			}
		});
		writeInstallableUnits(ius.iterator(), ius.size());
		writeInstallableUnitsProperties(ius.iterator(), ius.size(), profile);
		end(PROFILE_ELEMENT);
		flush();
	}

	private void writeInstallableUnitsProperties(Iterator<IInstallableUnit> it, int size, IProfile profile) {
		if (size == 0)
			return;
		start(IUS_PROPERTIES_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, size);
		while (it.hasNext()) {
			IInstallableUnit iu = it.next();
			Map<String, String> properties = profile.getInstallableUnitProperties(iu);
			if (properties.isEmpty())
				continue;

			start(IU_PROPERTIES_ELEMENT);
			attribute(ID_ATTRIBUTE, iu.getId());
			attribute(VERSION_ATTRIBUTE, iu.getVersion().toString());
			writeProperties(properties);
			end(IU_PROPERTIES_ELEMENT);
		}
		end(IUS_PROPERTIES_ELEMENT);
	}
}

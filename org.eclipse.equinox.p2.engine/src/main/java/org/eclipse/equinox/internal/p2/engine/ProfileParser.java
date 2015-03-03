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

import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;

/**
 *	An abstract XML parser class for parsing profiles as written by the ProfileWriter.
 */
public abstract class ProfileParser extends MetadataParser implements ProfileXMLConstants {

	public ProfileParser(BundleContext context, String bundleId) {
		super(context, bundleId);
	}

	protected class ProfileHandler extends RootHandler {

		private final String[] required = new String[] {ID_ATTRIBUTE};

		private String profileId;
		private String parentId;
		private String timestamp;
		private PropertiesHandler propertiesHandler;
		private InstallableUnitsHandler unitsHandler;
		private IUsPropertiesHandler iusPropertiesHandler;

		public ProfileHandler() {
			// default
		}

		protected ProfileHandler(String profileId) {
			this.profileId = profileId;
		}

		protected void handleRootAttributes(Attributes attributes) {
			profileId = parseRequiredAttributes(attributes, required)[0];
			parentId = parseOptionalAttribute(attributes, PARENT_ID_ATTRIBUTE);
			timestamp = parseOptionalAttribute(attributes, TIMESTAMP_ATTRIBUTE);

		}

		public void startElement(String name, Attributes attributes) {
			if (PROPERTIES_ELEMENT.equals(name)) {
				if (propertiesHandler == null) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
				if (unitsHandler == null) {
					unitsHandler = new InstallableUnitsHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (IUS_PROPERTIES_ELEMENT.equals(name)) {
				if (iusPropertiesHandler == null) {
					iusPropertiesHandler = new IUsPropertiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else {
				invalidElement(name, attributes);
			}
		}

		public String getProfileId() {
			return profileId;
		}

		public String getParentId() {
			return parentId;
		}

		public long getTimestamp() {
			if (timestamp != null) {
				try {
					return Long.parseLong(timestamp);
				} catch (NumberFormatException e) {
					// TODO: log
				}
			}
			return 0;
		}

		public Map<String, String> getProperties() {
			if (propertiesHandler == null)
				return null;
			return propertiesHandler.getProperties();
		}

		public IInstallableUnit[] getInstallableUnits() {
			if (unitsHandler == null)
				return null;
			return unitsHandler.getUnits();
		}

		public Map<String, String> getIUProperties(IInstallableUnit iu) {
			if (iusPropertiesHandler == null)
				return null;

			Map<String, Map<String, String>> iusPropertiesMap = iusPropertiesHandler.getIUsPropertiesMap();
			if (iusPropertiesMap == null)
				return null;

			String iuIdentity = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
			return iusPropertiesMap.get(iuIdentity);
		}
	}

	protected class IUPropertiesHandler extends AbstractHandler {

		private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};

		private String iuIdentity;
		private Map<String, Map<String, String>> iusPropertiesMap;
		private PropertiesHandler propertiesHandler;

		public IUPropertiesHandler(AbstractHandler parentHandler, Attributes attributes, Map<String, Map<String, String>> iusPropertiesMap) {
			super(parentHandler, IU_PROPERTIES_ELEMENT);
			this.iusPropertiesMap = iusPropertiesMap;

			String values[] = parseRequiredAttributes(attributes, required);
			String id = values[0];
			Version version = checkVersion(IU_PROPERTIES_ELEMENT, VERSION_ATTRIBUTE, values[1]);
			iuIdentity = id + "_" + version.toString(); //$NON-NLS-1$
		}

		protected void finished() {
			if (isValidXML() && iuIdentity != null && propertiesHandler != null) {
				iusPropertiesMap.put(iuIdentity, propertiesHandler.getProperties());
			}
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(PROPERTIES_ELEMENT)) {
				propertiesHandler = new PropertiesHandler(this, attributes);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class IUsPropertiesHandler extends AbstractHandler {

		private Map<String, Map<String, String>> iusPropertiesMap;

		public IUsPropertiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, IUS_PROPERTIES_ELEMENT);
			String sizeStr = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			int size = (sizeStr != null ? new Integer(sizeStr).intValue() : 4);
			iusPropertiesMap = new LinkedHashMap<String, Map<String, String>>(size);
		}

		public Map<String, Map<String, String>> getIUsPropertiesMap() {
			return iusPropertiesMap;
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(IU_PROPERTIES_ELEMENT)) {
				new IUPropertiesHandler(this, attributes, iusPropertiesMap);
			} else {
				invalidElement(name, attributes);
			}
		}
	}
}

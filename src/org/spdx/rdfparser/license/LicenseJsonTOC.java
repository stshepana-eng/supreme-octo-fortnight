/**
 * Copyright (c) 2018 Source Auditor Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.spdx.rdfparser.license;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Table of Contents for the listed license list as represented as a JSON index file
 * at spdx.org/licenses/licenses.json
 * 
 * @author Gary O'Neall
 *
 */
public class LicenseJsonTOC {
	
	class LicenseJson {
		private String reference;
		private boolean isDeprecatedLicenseId;
		private String detailsUrl;
		private int referenceNumber;
		private String name;
		private String licenseId;
		private String[] seeAlso;
		private boolean isOsiApproved;
		/**
		 * @return the reference
		 */
		public String getReference() {
			return reference;
		}
		/**
		 * @return the isDeprecatedLicenseId
		 */
		public boolean isDeprecatedLicenseId() {
			return isDeprecatedLicenseId;
		}
		/**
		 * @return the detailsUrl
		 */
		public String getDetailsUrl() {
			return detailsUrl;
		}
		/**
		 * @return the referenceNumber
		 */
		public int getReferenceNumber() {
			return referenceNumber;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the licenseId
		 */
		public String getLicenseId() {
			return licenseId;
		}
		/**
		 * @return the seeAlso
		 */
		public String[] getSeeAlso() {
			return seeAlso;
		}
		/**
		 * @return the isOsiApproved
		 */
		public boolean isOsiApproved() {
			return isOsiApproved;
		}
	}
	

	private String licenseListVersion;
	private LicenseJson[] licenses;
	private String releaseDate;

	/**
	 * @return the licenseListVersion
	 */
	public String getLicenseListVersion() {
		return licenseListVersion;
	}

	/**
	 * @return the licenses
	 */
	public LicenseJson[] getLicenses() {
		return licenses;
	}
	
	public Set<String> getLicenseIds() {
		Set<String> retval = Sets.newHashSet();
		if (licenses == null) {
			return retval;
		}
		for (LicenseJson license:licenses) {
			retval.add(license.licenseId);
		}
		return retval;
	}

	/**
	 * @return the releaseDate
	 */
	public String getReleaseDate() {
		return releaseDate;
	}
	

}

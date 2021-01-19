/**
 * Copyright (c) 2011 Source Auditor Inc.
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
 *
*/
package org.spdx.html;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.model.Annotation;
import org.spdx.rdfparser.model.Checksum;
import org.spdx.rdfparser.model.ExternalRef;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;

import com.google.common.collect.Lists;

/**
 * Context for SPDX Package
 * @author Gary O'Neall
 *
 */
public class PackageContext {

	private SpdxPackage pkg = null;
	private Map<String, String> spdxIdToUrl;

	/**
	 * @param doc
	 * @throws InvalidSPDXAnalysisException
	 */
	public PackageContext(SpdxPackage pkg, Map<String, String> spdxIdToUrl) throws InvalidSPDXAnalysisException {
		this.pkg = pkg;
		this.spdxIdToUrl = spdxIdToUrl;
	}

	public String name() {
		if (this.pkg != null) {
			return pkg.getName();
		} else {
			return null;
		}
	}

	public String versionInfo() {
		if (pkg != null) {
			return pkg.getVersionInfo();
		} else {
			return null;
		}
	}

	public String downloadLocation() {
		if (pkg != null) {
			return pkg.getDownloadLocation();
		} else {
			return null;
		}
	}

	public String summary() {
		if (pkg != null) {
			return pkg.getSummary();
		} else {
			return null;
		}
	}

	public List<String> attributionText() {
		if (pkg == null || pkg.getAttributionText() == null) {
			return null;
		}
		return Arrays.asList(pkg.getAttributionText());
	}

	public String sourceInfo() {
		if (pkg != null) {
			return pkg.getSourceInfo();
		} else {
			return null;
		}
	}

	public String packageFileName() {
		if (pkg != null) {
			return pkg.getPackageFileName();
		} else {
			return null;
		}
	}

	public String supplier() {
		if (pkg != null) {
			return pkg.getSupplier();
		} else {
			return null;
		}
	}

	public String originator() {
		if (pkg != null) {
			return pkg.getOriginator();
		} else {
			return null;
		}
	}

	public String description() {
		if (pkg != null) {
			return pkg.getDescription();
		} else {
			return null;
		}
	}

	public VerificationCodeContext packageVerificationCode() {
		if (pkg == null) {
			return null;
		}
		SpdxPackageVerificationCode verificationCode;
		try {
			verificationCode = pkg.getPackageVerificationCode();
			if (verificationCode == null) {
				return null;
			}
			return new VerificationCodeContext(verificationCode);
		} catch (InvalidSPDXAnalysisException e) {
			return new VerificationCodeContext(e);
		}

	}

	public List<String> checksum() {
		if (pkg != null) {
			List<String> retval = Lists.newArrayList();

			try {
				Checksum[] checksums = pkg.getChecksums();
				if (checksums == null || checksums.length > 0) {
					return null;
				}
				for (int i = 0; i < checksums.length; i++) {
					retval.add(Checksum.CHECKSUM_ALGORITHM_TO_TAG.get(checksums[i].getAlgorithm())+
							" "+checksums[i].getValue());
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Error getting SPDX Package checksum: "+e.getMessage());
			}
			Collections.sort(retval);
			return retval;
		} else {
			return null;
		}
	}

	public String copyrightText() {
		if (pkg != null) {
			return pkg.getCopyrightText();
		} else {
			return null;
		}
	}

	public String licenseDeclared() {
		if (pkg != null) {
			try {
				AnyLicenseInfo info = pkg.getLicenseDeclared();
				if (info != null) {
					return info.toString();
				} else {
					return null;
				}
			} catch (InvalidSPDXAnalysisException e) {
				return "Error getting SPDX Package copyright: "+e.getMessage();
			}
		} else {
			return null;
		}
	}

	public String licenseConcluded() {
		if (pkg != null) {
			AnyLicenseInfo info = pkg.getLicenseConcluded();
			if (info != null) {
				return info.toString();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public String licenseComments() {
		if (pkg != null) {
			return pkg.getLicenseComments();
		} else {
			return null;
		}
	}

	public String homePage() {
		if (pkg != null) {
			return pkg.getHomepage();
		} else {
			return  null;
		}
	}

	public List<String> licenseInfoFromFiles() {
		List<String> retval = Lists.newArrayList();
		if (pkg != null) {
			AnyLicenseInfo[] licenseInfos = null;
			licenseInfos = pkg.getLicenseInfoFromFiles();
			if (licenseInfos != null) {
				for (int i = 0; i < licenseInfos.length; i++) {
					retval.add(licenseInfos[i].toString());
				}
			}
		}
		Collections.sort(retval);
		return retval;
	}

	public String spdxId() {
		if (pkg == null) {
			return null;
		}
		return pkg.getId();
	}

	public List<ElementContext> hasFile() {
		List<ElementContext> retval = Lists.newArrayList();
		if (pkg != null) {
			SpdxFile[] files;
			try {
				files = pkg.getFiles();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						retval.add(new ElementContext(files[i], this.spdxIdToUrl));
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add(new ElementContext(e));
			}
		}
		Collections.sort(retval, new Comparator<ElementContext>() {
            @Override
            public int compare(ElementContext o1, ElementContext o2) {
                if (o1 == null || o1.getId() == null) {
                	if (o2 != null && o2.getId() != null) {
                		return 1;
                	}
                }
                if (o2 == null || o2.getId() == null) {
                	return -1;
                }
                return o1.getId().compareTo(o2.getId());
            }} );
		return retval;
	}

	public boolean isFilesAnalyzed() {
		if (this.pkg == null) {
			return true;
		}
		try {
			return this.pkg.isFilesAnalyzed();
		} catch (InvalidSPDXAnalysisException e) {
			return true;
		}
	}

	public List<RelationshipContext> packageRelationships() {
		    List<RelationshipContext> retval = Lists.newArrayList();
		    Relationship[] relationships = pkg.getRelationships();
		    if (relationships != null) {
		        Arrays.sort(relationships);

	    		for (Relationship relationship : relationships) {
	    		    retval.add(new RelationshipContext(relationship, spdxIdToUrl));
	    		}
		    }

			return retval;
	}

	public List<AnnotationContext> packageAnnotations() {
		List<AnnotationContext> retval  = Lists.newArrayList();
		Annotation[] annotations = pkg.getAnnotations();
		if (annotations != null) {
			Arrays.sort(annotations);
			for (Annotation annotation : annotations) {
				retval.add(new AnnotationContext(annotation));
			}
		}
		return retval;
	}

	public List<ExternalRefContext> externalRefs() {
		List<ExternalRefContext> retval  = Lists.newArrayList();
		if (this.pkg != null) {
			try {
				ExternalRef[] externalRefs = pkg.getExternalRefs();
				if (externalRefs != null) {
					Arrays.sort(externalRefs);
					for (ExternalRef externalRef:externalRefs) {
						retval.add(new ExternalRefContext(externalRef));
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				// Return an empty list
			}
		}
		return retval;
	}
}

/**
 * Copyright (c) 2015 Source Auditor Inc.
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
package org.spdx.rdfparser.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.RdfModelHelper;
import org.spdx.rdfparser.RdfParserHelper;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.SpdxVerificationHelper;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.OrLaterOperator;
import org.spdx.rdfparser.license.SimpleLicensingInfo;
import org.spdx.rdfparser.license.SpdxNoAssertionLicense;
import org.spdx.rdfparser.license.SpdxNoneLicense;
import org.spdx.rdfparser.license.WithExceptionOperator;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;

import com.google.common.collect.Maps;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * A Package represents a collection of software files that are
 * delivered as a single functional component.
 * @author Gary O'Neall
 *
 */
public class SpdxPackage extends SpdxItem implements SpdxRdfConstants, Comparable<SpdxPackage> {

	AnyLicenseInfo licenseDeclared;
	Checksum[] checksums;
	String description;
	String downloadLocation;
	String homepage;
	String originator;
	String packageFileName;
	SpdxPackageVerificationCode packageVerificationCode;
	String sourceInfo;
	String summary;
	String supplier;
	String versionInfo;
	SpdxFile[] files;
	boolean filesAnalyzed = true;
	ExternalRef[] externalRefs;

	/**
	 * @param name
	 * @param comment
	 * @param annotations
	 * @param relationships
	 * @param licenseConcluded
	 * @param licenseInfosFromFiles
	 * @param copyrightText
	 * @param licenseComment
	 * @param licenseDeclared
	 * @param checksums
	 * @param description
	 * @param downloadLocation
	 * @param files
	 * @param homepage
	 * @param originator
	 * @param packageFileName
	 * @param packageVerificationCode
	 * @param sourceInfo
	 * @param summary
	 * @param supplier
	 * @param versionInfo
	 * @param filesAnalyzed
	 * @param externalRefs
	 */
	public SpdxPackage(String name, String comment, Annotation[] annotations,
			Relationship[] relationships, AnyLicenseInfo licenseConcluded,
			AnyLicenseInfo[] licenseInfosFromFiles, String copyrightText,
			String licenseComment, AnyLicenseInfo licenseDeclared,
			Checksum[] checksums, String description, String downloadLocation,
			SpdxFile[] files, String homepage, String originator, String packageFileName,
			SpdxPackageVerificationCode packageVerificationCode,
			String sourceInfo, String summary, String supplier,
			String versionInfo, boolean filesAnalyzed, ExternalRef[] externalRefs) {
		super(name, comment, annotations, relationships, licenseConcluded,
				licenseInfosFromFiles, copyrightText, licenseComment);
		this.licenseDeclared = licenseDeclared;
		this.checksums = checksums;
		if (this.checksums == null) {
			this.checksums = new Checksum[0];
		}
		this.description = description;
		this.downloadLocation = downloadLocation;
		this.files = files;
		if (this.files == null) {
			this.files = new SpdxFile[0];
		}
		this.homepage = homepage;
		this.originator = originator;
		this.packageFileName = packageFileName;
		this.packageVerificationCode = packageVerificationCode;
		this.sourceInfo = sourceInfo;
		this.summary = summary;
		this.supplier = supplier;
		this.versionInfo = versionInfo;
		this.filesAnalyzed = filesAnalyzed;
		this.externalRefs = externalRefs;
	}

	public SpdxPackage(String name, String comment, Annotation[] annotations,
			Relationship[] relationships, AnyLicenseInfo licenseConcluded,
			AnyLicenseInfo[] licenseInfosFromFiles, String copyrightText,
			String licenseComment, AnyLicenseInfo licenseDeclared,
			Checksum[] checksums, String description, String downloadLocation,
			SpdxFile[] files, String homepage, String originator, String packageFileName,
			SpdxPackageVerificationCode packageVerificationCode,
			String sourceInfo, String summary, String supplier,
			String versionInfo) {
		this(name, comment, annotations, relationships, licenseConcluded,
				licenseInfosFromFiles, copyrightText, licenseComment, licenseDeclared,
				checksums, description, downloadLocation, files, homepage,
				originator, packageFileName, packageVerificationCode,
				sourceInfo, summary, supplier, versionInfo, true, new ExternalRef[0]);
	}

	public SpdxPackage(String name, AnyLicenseInfo licenseConcluded,
			AnyLicenseInfo[] licenseInfosFromFiles, String copyrightText,
			AnyLicenseInfo licenseDeclared, String downloadLocation, SpdxFile[] files,
			SpdxPackageVerificationCode packageVerificationCode) {
		this(name, null, null, null, licenseConcluded,
				licenseInfosFromFiles, copyrightText, null, licenseDeclared,
				null, null, downloadLocation, files, null, null, null, packageVerificationCode,
				null, null, null, null, true, new ExternalRef[0]);
	}

	/**
	 * @param modelContainer
	 * @param node
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxPackage(IModelContainer modelContainer, Node node)
			throws InvalidSPDXAnalysisException {
		super(modelContainer, node);
		getMyPropertiesFromModel();
	}

	/**
	 * @param name
	 * @param comment
	 * @param annotations
	 * @param relationships
	 * @param licenseConcluded
	 * @param licenseInfosFromFiles
	 * @param copyrightText
	 * @param licenseComment
	 * @param licenseDeclared
	 * @param checksums
	 * @param description
	 * @param downloadLocation
	 * @param files
	 * @param homepage
	 * @param originator
	 * @param packageFileName
	 * @param packageVerificationCode
	 * @param sourceInfo
	 * @param summary
	 * @param supplier
	 * @param versionInfo
	 * @param filesAnalyzed
	 * @param externalRefs
	 * @param attributionText
	 */
	public SpdxPackage(String name, String comment, Annotation[] annotations,
			Relationship[] relationships, AnyLicenseInfo licenseConcluded,
			AnyLicenseInfo[] licenseInfosFromFiles, String copyrightText,
			String licenseComment, AnyLicenseInfo licenseDeclared,
			Checksum[] checksums, String description, String downloadLocation,
			SpdxFile[] files, String homepage, String originator, String packageFileName,
			SpdxPackageVerificationCode packageVerificationCode,
			String sourceInfo, String summary, String supplier,
			String versionInfo, boolean filesAnalyzed, ExternalRef[] externalRefs,
			String[] attributionText) {
		this(name, comment, annotations, relationships, licenseConcluded,
				licenseInfosFromFiles, copyrightText, licenseComment, licenseDeclared,
				checksums, description, downloadLocation, files, homepage, originator,
				packageFileName, packageVerificationCode, sourceInfo, summary, supplier,
				versionInfo, filesAnalyzed, externalRefs);
		this.attributionText = attributionText;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getPropertiesFromModel()
	 */
	@Override
	public void getPropertiesFromModel() throws InvalidSPDXAnalysisException {
		super.getPropertiesFromModel();
		getMyPropertiesFromModel();
	}

	void getMyPropertiesFromModel() throws InvalidSPDXAnalysisException {
		this.licenseDeclared = findAnyLicenseInfoPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DECLARED_LICENSE);
		this.checksums = findMultipleChecksumPropertyValues(SPDX_NAMESPACE,
				PROP_PACKAGE_CHECKSUM);
		this.description = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DESCRIPTION);
		this.downloadLocation = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DOWNLOAD_URL);
		this.homepage = findSinglePropertyValue(DOAP_NAMESPACE,
				PROP_PROJECT_HOMEPAGE);
		this.originator = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_ORIGINATOR);
		this.packageFileName = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE_NAME);
		this.packageVerificationCode = findVerificationCodePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_VERIFICATION_CODE);
		this.sourceInfo = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SOURCE_INFO);
		this.summary = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SHORT_DESC);
		this.supplier = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SUPPLIER);
		this.versionInfo = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_VERSION_INFO);
		SpdxElement[] filesE = findMultipleElementPropertyValues(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE);
		this.files = new SpdxFile[filesE.length];
		for (int i = 0; i < filesE.length; i++) {
			if (!(filesE[i] instanceof SpdxFile)) {
				throw(new InvalidSPDXAnalysisException("Incorrect type for a file belonging to a package: "+filesE[i].getName()));
			}
			this.files[i] = (SpdxFile)filesE[i];
		}
		String filesAnalyzedString = findSinglePropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILES_ANALYZED);
		if (filesAnalyzedString != null) {
			filesAnalyzedString = filesAnalyzedString.trim();
			if (filesAnalyzedString.equals("true") || filesAnalyzedString.equals("1")) {
				this.filesAnalyzed = true;
			} else if (filesAnalyzedString.equals("false") || filesAnalyzedString.equals("0")){
				this.filesAnalyzed = false;
			} else {
				throw(new InvalidSPDXAnalysisException("Invalid value for files analyzed - must be {true, false, 0, 1}"));
			}
		} else {
			this.filesAnalyzed = true;	// Default value is true per the 2.1 specification
		}
		this.externalRefs = findExternalRefPropertyValues(SPDX_NAMESPACE,
				PROP_EXTERNAL_REF);
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#findDuplicateResource(org.spdx.rdfparser.IModelContainer, java.lang.String)
	 */
	@Override
	public Resource findDuplicateResource(IModelContainer modelContainer, String uri) throws InvalidSPDXAnalysisException {
		// A duplicate package has the same name and verification code
		if (this.name == null || this.name.isEmpty()) {
			return null;
		}
		if (this.packageVerificationCode == null) {
			return null;
		}
		if (this.packageVerificationCode.getValue() == null || this.packageVerificationCode.getValue().isEmpty()) {
			return null;
		}
		Model localModel = modelContainer.getModel();
		// look for a matching name
		Node nameProperty = localModel.getProperty(SPDX_NAMESPACE, this.getNamePropertyName()).asNode();
		Node verificationCodeProperty = localModel.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_VERIFICATION_CODE).asNode();
		Node verificationCodeValueProperty = localModel.getProperty(SPDX_NAMESPACE, PROP_VERIFICATIONCODE_VALUE).asNode();

		Triple nameMatch = Triple.createMatch(null, nameProperty, NodeFactory.createLiteral(this.name));
		ExtendedIterator<Triple> nameleIter = localModel.getGraph().find(nameMatch);
		while (nameleIter.hasNext()) {
			Triple t = nameleIter.next();
			// Check for the package verification code
			Node packageNode = t.getSubject();
			Triple verifcationMatch = Triple.createMatch(packageNode, verificationCodeProperty, null);
			ExtendedIterator<Triple> verificationIter = localModel.getGraph().find(verifcationMatch);
			while (verificationIter.hasNext()) {
				Triple vt = verificationIter.next();
				Triple valueMatch = Triple.createMatch(vt.getObject(), verificationCodeValueProperty, null);
				ExtendedIterator<Triple> valueIter = localModel.getGraph().find(valueMatch);
				while (valueIter.hasNext()) {
					Triple valuetrip = valueIter.next();
					String verificationCodeValue = valuetrip.getObject().toString(false);
					if (this.packageVerificationCode.getValue().equals(verificationCodeValue)) {
						return RdfParserHelper.convertToResource(localModel, packageNode);
					}
				}
			}
		}
		return null;	// if we got here, we didn't find a duplicate
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.SpdxItem#populateModel()
	 */
	@Override
	public void populateModel() throws InvalidSPDXAnalysisException {
		super.populateModel();
		 setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DECLARED_LICENSE, this.licenseDeclared);
		setPropertyValues(SPDX_NAMESPACE,
				PROP_PACKAGE_CHECKSUM, this.checksums);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DESCRIPTION, this.description);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_DOWNLOAD_URL, this.downloadLocation);
		setPropertyValue(DOAP_NAMESPACE,
				PROP_PROJECT_HOMEPAGE, this.homepage);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_ORIGINATOR, this.originator);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE_NAME, this.packageFileName);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_VERIFICATION_CODE, this.packageVerificationCode);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SOURCE_INFO, this.sourceInfo);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SHORT_DESC, this.summary);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_SUPPLIER, this.supplier);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_VERSION_INFO, this.versionInfo);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE, this.files);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILES_ANALYZED, this.filesAnalyzed);
		setPropertyValue(SPDX_NAMESPACE,
				PROP_EXTERNAL_REF, this.externalRefs);
	}

	@Override
	protected String getNamePropertyName() {
		return SpdxRdfConstants.PROP_PROJECT_NAME;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getType(org.apache.jena.rdf.model.Model)
	 */
	@Override
	public Resource getType(Model model) {
		return model.createResource(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.CLASS_SPDX_PACKAGE);
	}

	/**
	 * @return true if filesAnalyzed flag is set
	 * @throws InvalidSPDXAnalysisException
	 */
	public boolean isFilesAnalyzed() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			String filesAnalyzedString = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_FILES_ANALYZED);
			if (filesAnalyzedString != null) {
				filesAnalyzedString = filesAnalyzedString.trim();
				if (filesAnalyzedString.equals("true") || filesAnalyzedString.equals("1")) {
					this.filesAnalyzed = true;
				} else if (filesAnalyzedString.equals("false") || filesAnalyzedString.equals("0")){
					this.filesAnalyzed = false;
				} else {
					throw(new InvalidSPDXAnalysisException("Invalid value for files analyzed - must be {true, false, 0, 1}"));
				}
			} else {
				this.filesAnalyzed = true;	// Default value is true per the 2.1 specification
			}
		}
		return this.filesAnalyzed;
	}

	/**
	 * Set files Analyzed for the package
	 * @param filesAnalyzed
	 */
	public void setFilesAnalyzed(boolean filesAnalyzed) {
		this.filesAnalyzed = filesAnalyzed;
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILES_ANALYZED, filesAnalyzed);
	}

	/**
	 * @return the licenseDeclared
	 * @throws InvalidSPDXAnalysisException
	 */
	public AnyLicenseInfo getLicenseDeclared() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			AnyLicenseInfo refresh = findAnyLicenseInfoPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DECLARED_LICENSE);
			if (refresh == null || !refresh.equals(this.licenseDeclared)) {
				this.licenseDeclared = refresh;
			}
		}
		return licenseDeclared;
	}

	/**
	 * @param licenseDeclared the licenseDeclared to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setLicenseDeclared(AnyLicenseInfo licenseDeclared) throws InvalidSPDXAnalysisException {
		this.licenseDeclared = licenseDeclared;
		 setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DECLARED_LICENSE, this.licenseDeclared);
	}

	/**
	 * @return the checksums
	 * @throws InvalidSPDXAnalysisException
	 */
	public Checksum[] getChecksums() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			Checksum[] refresh = findMultipleChecksumPropertyValues(SPDX_NAMESPACE,
					PROP_PACKAGE_CHECKSUM);
			if (!arraysEquivalent(refresh, this.checksums, true)) {
				this.checksums = refresh;
			}
		}
		return checksums;
	}

	/**
	 * @param checksums the checksums to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setChecksums(Checksum[] checksums) throws InvalidSPDXAnalysisException {
		this.checksums = checksums;
		setPropertyValues(SPDX_NAMESPACE,
					PROP_PACKAGE_CHECKSUM, this.checksums);
	}

	/**
	 * Add a checksum to the list of checksums for this package
	 * @param checksum
	 * @throws InvalidSPDXAnalysisException
	 */
	public void addChecksum(Checksum checksum) throws InvalidSPDXAnalysisException {
		if (checksum == null) {
			return;
		}
		this.checksums = Arrays.copyOf(this.checksums, this.checksums.length + 1);
		this.checksums[this.checksums.length-1] = checksum;
		addPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_CHECKSUM, checksum);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		if (this.resource != null && refreshOnGet) {
			this.description = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DESCRIPTION);
		}
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DESCRIPTION, this.description);
	}

	/**
	 * @return the downloadLocation
	 */
	public String getDownloadLocation() {
		if (this.resource != null && refreshOnGet) {
			this.downloadLocation = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DOWNLOAD_URL);
		}
		return downloadLocation;
	}

	/**
	 * @param downloadLocation the downloadLocation to set
	 */
	public void setDownloadLocation(String downloadLocation) {
		this.downloadLocation = downloadLocation;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_DOWNLOAD_URL, this.downloadLocation);
	}

	/**
	 * @return the homepage
	 */
	public String getHomepage() {
		if (this.resource != null && refreshOnGet) {
			this.homepage = findSinglePropertyValue(DOAP_NAMESPACE,
					PROP_PROJECT_HOMEPAGE);
		}
		return homepage;
	}

	/**
	 * @param homepage the homepage to set
	 */
	public void setHomepage(String homepage) {
		this.homepage = homepage;
		setPropertyValue(DOAP_NAMESPACE,
					PROP_PROJECT_HOMEPAGE, this.homepage);
	}

	/**
	 * @return the originator
	 */
	public String getOriginator() {
		if (this.resource != null && refreshOnGet) {
			this.originator = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_ORIGINATOR);
		}
		return originator;
	}

	/**
	 * @param originator the originator to set
	 */
	public void setOriginator(String originator) {
		this.originator = originator;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_ORIGINATOR, this.originator);
	}

	/**
	 * @return the packageFileName
	 */
	public String getPackageFileName() {
		if (this.resource != null && refreshOnGet) {
			this.packageFileName = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_FILE_NAME);
		}
		return packageFileName;
	}

	/**
	 * @param packageFileName the packageFileName to set
	 */
	public void setPackageFileName(String packageFileName) {
		this.packageFileName = packageFileName;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_FILE_NAME, this.packageFileName);
	}

	/**
	 * @return the packageVerificationCode
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxPackageVerificationCode getPackageVerificationCode() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			SpdxPackageVerificationCode refresh = findVerificationCodePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_VERIFICATION_CODE);
			if (refresh == null || !refresh.equivalent(this.packageVerificationCode)) {
				this.packageVerificationCode = refresh;
			}
		}
		return packageVerificationCode;
	}

	/**
	 * @param packageVerificationCode the packageVerificationCode to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setPackageVerificationCode(
			SpdxPackageVerificationCode packageVerificationCode) throws InvalidSPDXAnalysisException {
		this.packageVerificationCode = packageVerificationCode;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_VERIFICATION_CODE, this.packageVerificationCode);
	}

	/**
	 * @return the sourceInfo
	 */
	public String getSourceInfo() {
		if (this.resource != null && refreshOnGet) {
			this.sourceInfo = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SOURCE_INFO);
		}
		return sourceInfo;
	}

	/**
	 * @param sourceInfo the sourceInfo to set
	 */
	public void setSourceInfo(String sourceInfo) {
		this.sourceInfo = sourceInfo;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SOURCE_INFO, this.sourceInfo);
	}

	/**
	 * @return the summary
	 */
	public String getSummary() {
		if (this.resource != null && refreshOnGet) {
			this.summary = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SHORT_DESC);
		}
		return summary;
	}

	/**
	 * @param summary the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SHORT_DESC, this.summary);
	}

	/**
	 * @return the supplier
	 */
	public String getSupplier() {
		if (this.resource != null && refreshOnGet) {
			this.supplier = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SUPPLIER);
		}
		return supplier;
	}

	/**
	 * @param supplier the supplier to set
	 */
	public void setSupplier(String supplier) {
		this.supplier = supplier;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_SUPPLIER, this.supplier);
	}

	/**
	 * @return the versionInfo
	 */
	public String getVersionInfo() {
		if (this.resource != null && refreshOnGet) {
			this.versionInfo = findSinglePropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_VERSION_INFO);
		}
		return versionInfo;
	}

	/**
	 * @param versionInfo the versionInfo to set
	 */
	public void setVersionInfo(String versionInfo) {
		this.versionInfo = versionInfo;
		setPropertyValue(SPDX_NAMESPACE,
					PROP_PACKAGE_VERSION_INFO, this.versionInfo);
	}

	/**
	 * @return the externalRefs
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExternalRef[] getExternalRefs() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			this.externalRefs = findExternalRefPropertyValues(SPDX_NAMESPACE,
					PROP_EXTERNAL_REF);
		}
		return externalRefs;
	}

	/**
	 * @param externalRefs the externalRefs to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setExternalRefs(ExternalRef[] externalRefs) throws InvalidSPDXAnalysisException {
		setPropertyValue(SPDX_NAMESPACE,
				PROP_EXTERNAL_REF, externalRefs);
		this.externalRefs = externalRefs;
	}

	/**
	 * @return the files
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxFile[] getFiles() throws InvalidSPDXAnalysisException {
		return this.getFiles(true);
	}
	/**
	 * @return the files
	 * @param checkRelationships - if true, compare relationships when determining if the files need to be loaded
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxFile[] getFiles(boolean checkRelationships) throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			SpdxElement[] filesE = findMultipleElementPropertyValues(SPDX_NAMESPACE,
					PROP_PACKAGE_FILE);
			if (!arraysEquivalent(filesE, this.files, checkRelationships)) {
				this.files = new SpdxFile[filesE.length];
				for (int i = 0; i < filesE.length; i++) {
					if (!(filesE[i] instanceof SpdxFile)) {
						throw(new InvalidSPDXAnalysisException("Incorrect type for a file belonging to a package: "+filesE[i].getName()));
					}
					this.files[i] = (SpdxFile)filesE[i];
				}
			}
		}
		return files;
	}

	/**
	 * @param files the files to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setFiles(SpdxFile[] files) throws InvalidSPDXAnalysisException {
		this.files = files;
		setPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE, this.files);
	}

	/**
	 * Add a file to the list of files attached to this package
	 * @param file
	 * @throws InvalidSPDXAnalysisException
	 */
	public void addFile(SpdxFile file) throws InvalidSPDXAnalysisException {
		if (file == null) {
			return;
		}
		if (this.files == null) {
			this.files = new SpdxFile[] {file};
		} else {
			this.files = Arrays.copyOf(this.files, this.files.length+1);
			this.files[this.files.length-1] = file;
		}
		addPropertyValue(SPDX_NAMESPACE,
				PROP_PACKAGE_FILE, file);
	}

	@Override
	public boolean equivalent(IRdfModel o) {
		return this.equivalent(o, true);
	}

	@Override
	public boolean equivalent(IRdfModel o, boolean testRelationships) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof SpdxPackage)) {
			return false;
		}
		if (!super.equivalent(o, testRelationships)) {
			return false;
		}
		SpdxPackage comp = (SpdxPackage)o;
		try {
			SpdxPackageVerificationCode myVerificationCode = getPackageVerificationCode();
			if (myVerificationCode == null) {
				if (comp.getPackageVerificationCode() != null) {
					return false;
				}
			} else {
				if (!(myVerificationCode.equivalent(comp.getPackageVerificationCode()))) {
					return false;
				}
			}
			if (this.isFilesAnalyzed() != comp.isFilesAnalyzed()) {
				return false;
			}
		return (equivalentConsideringNull(this.getLicenseDeclared(), comp.getLicenseDeclared()) &&
				arraysEquivalent(this.getChecksums(), comp.getChecksums(), testRelationships) &&
				RdfModelHelper.stringsEquivalent(this.getDescription(), comp.getDescription()) &&
				RdfModelHelper.stringsEquivalent(this.getDownloadLocation(), comp.getDownloadLocation()) &&
				arraysEquivalent(this.getFiles(false), comp.getFiles(false), testRelationships) &&
				RdfModelHelper.stringsEquivalent(this.getHomepage(), comp.getHomepage()) &&
				RdfModelHelper.stringsEquivalent(this.getOriginator(), comp.getOriginator()) &&
				RdfModelHelper.stringsEquivalent(this.getPackageFileName(), comp.getPackageFileName()) &&
				RdfModelHelper.stringsEquivalent(this.getSourceInfo(), comp.getSourceInfo()) &&
				RdfModelHelper.stringsEquivalent(this.getSummary(), comp.getSummary()) &&
				RdfModelHelper.stringsEquivalent(this.getSupplier(), comp.getSupplier()) &&
				RdfModelHelper.stringsEquivalent(this.getVersionInfo(), comp.getVersionInfo()) &&
				arraysEquivalent(this.getExternalRefs(), comp.getExternalRefs()));
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Invalid analysis exception on comparing equivalent: "+e.getMessage(),e);
			return false;
		}
	}

	@Override
    public SpdxPackage clone(Map<String, SpdxElement> clonedElementIds) {
		if (clonedElementIds.containsKey(this.getId())) {
			return (SpdxPackage)clonedElementIds.get(this.getId());
		}

		SpdxPackage retval = new SpdxPackage(this.name, this.comment, this.cloneAnnotations(),
				null, this.cloneLicenseConcluded(),
				this.cloneLicenseInfosFromFiles(), this.copyrightText,
				this.licenseComments, this.cloneLicenseDeclared(), this.cloneCheckums(),
				this.description, this.downloadLocation, null,
				this.homepage, this.originator, this.packageFileName,
				this.clonePackageVerificationCode(), this.sourceInfo,
				this.summary, this.supplier, this.versionInfo, this.filesAnalyzed,
				this.cloneExternalRefs());
		clonedElementIds.put(this.getId(), retval);
		try {
			retval.setRelationships(cloneRelationships(clonedElementIds));
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Unexected error setting relationships during clone",e);
		}
		try {
			retval.setFiles(this.cloneFiles(clonedElementIds));
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Unexected error setting relationships during clone",e);
		}
		return retval;
	}

	/**
	 * @return
	 */
	private ExternalRef[] cloneExternalRefs() {
		if (this.externalRefs == null) {
			return null;
		}
		ExternalRef[] retval = new ExternalRef[this.externalRefs.length];
		for (int i = 0; i < this.externalRefs.length; i++) {
			retval[i] = this.externalRefs[i].clone();
		}
		return retval;
	}

	@Override
	public SpdxPackage clone() {
		return clone(Maps.<String, SpdxElement>newHashMap());
	}

	/**
	 * @return
	 */
	private SpdxPackageVerificationCode clonePackageVerificationCode() {
		if (this.packageVerificationCode == null) {
			return null;
		}
		return new SpdxPackageVerificationCode(this.packageVerificationCode.getValue(),
				this.packageVerificationCode.getExcludedFileNames());
	}

	/**
	 * @return
	 */
	private SpdxFile[] cloneFiles(Map<String, SpdxElement> clonedElementIds) {
		if (this.files == null) {
			return new SpdxFile[0];
		}
		SpdxFile[] retval = new SpdxFile[this.files.length];
		for (int i = 0; i < files.length; i++) {
			retval[i] = this.files[i].clone(clonedElementIds);
		}
		return retval;
	}

	/**
	 * @return
	 */
	private AnyLicenseInfo cloneLicenseDeclared() {
		if (this.licenseDeclared == null) {
			return null;
		} else {
			return this.licenseDeclared.clone();
		}
	}

	/**
	 * @return
	 */
	private Checksum[] cloneCheckums() {
		if (this.checksums == null) {
			return new Checksum[0];
		}
		Checksum[] retval = new Checksum[this.checksums.length];
		for (int i = 0; i < this.checksums.length; i++) {
			retval[i] = this.checksums[i].clone();
		}
		return retval;
	}

	@Override
	public List<String> verify() {
		String pkgName = name;
		if (pkgName == null ) {
			pkgName = "UNKNOWN PACKAGE";
		}
		List<String> retval = super.verify();
		// summary - nothing really to check

		// description - nothing really to check

		// download location
		String downloadLocation = this.getDownloadLocation();
		if (downloadLocation == null || downloadLocation.isEmpty()) {
			retval.add("Missing required download location for package "+pkgName);
		}
		//TODO: Add a check for the download location format per SPDX 2.0

		// checksum
		for (int i = 0; i < checksums.length; i++) {
			List<String> checksumVerify = checksums[i].verify();
			addNameToWarnings(checksumVerify);
			retval.addAll(checksumVerify);
		}

		// sourceinfo - nothing really to check

		// license declared - mandatory - 1 (need to change return values)
		try {
			AnyLicenseInfo declaredLicense = this.getLicenseDeclared();
			if (declaredLicense == null) {
				retval.add("Missing required declared license for package "+pkgName);
			} else {
				List<String> verify = declaredLicense.verify();
				addNameToWarnings(verify);
				retval.addAll(verify);
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid package declared license: "+e.getMessage());
		}
		if ((this.licenseInfoFromFiles == null || this.licenseInfoFromFiles.length == 0)
				&& filesAnalyzed) {
			retval.add("Missing required license information from files for "+name);
		} else {
			boolean foundNonSimpleLic = false;
			for (int i = 0; i < this.licenseInfoFromFiles.length; i++) {
				AnyLicenseInfo lic = this.licenseInfoFromFiles[i];
				if (!(lic instanceof SimpleLicensingInfo ||
						lic instanceof SpdxNoAssertionLicense ||
						lic instanceof SpdxNoneLicense ||
						lic instanceof OrLaterOperator ||
						lic instanceof WithExceptionOperator)) {
					foundNonSimpleLic = true;
					break;
				}
			}
			if (foundNonSimpleLic) {
				retval.add("license info from files contains complex licenses for "+name);
			}
		}
		// files depends on if the filesAnalyzed flag
		try {
			SpdxFile[] files = this.getFiles();
			if (files == null || files.length == 0) {
				if (filesAnalyzed) {
					retval.add("Missing required package files for "+pkgName);
				}
			} else {
				if (!filesAnalyzed) {
					retval.add("Warning: Found analyzed files for package "+pkgName+" when analyzedFiles is set to false.");
				}
				for (int i = 0; i < files.length; i++) {
					List<String> verify = files[i].verify();
					addNameToWarnings(verify);
					retval.addAll(verify);
				}
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid package files: "+e.getMessage());
		}

		// verification code
		SpdxPackageVerificationCode verificationCode = null;
		try {
			verificationCode = this.getPackageVerificationCode();
			if (verificationCode == null && filesAnalyzed) {
				retval.add("Missing required package verification code for package " + pkgName);
			} else if (verificationCode != null && verificationCode.getValue() != null && !verificationCode.getValue().isEmpty() && !filesAnalyzed) {
				retval.add("Verification code must not be included when files not analyzed.");
			} else if (filesAnalyzed) {
				List<String> verify = verificationCode.verify();
				addNameToWarnings(verify);
				retval.addAll(verify);
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid package verification code: " + e.getMessage());
		}

		// supplier
		String supplier = null;
		supplier = this.getSupplier();
		if (supplier != null && !supplier.isEmpty()) {
			String error = SpdxVerificationHelper.verifySupplier(supplier);
			if (error != null && !error.isEmpty()) {
				retval.add("Supplier error - "+error+ " for package "+pkgName);
			}
		}
		// originator
		String originator = this.getOriginator();
		if (originator != null && !originator.isEmpty()) {
			String error = SpdxVerificationHelper.verifyOriginator(originator);
			if (error != null && !error.isEmpty()) {
				retval.add("Originator error - "+error+ " for package "+pkgName);
			}
		}
		// External refs
		if (this.externalRefs != null) {
			for (ExternalRef externalRef:this.externalRefs) {
				retval.addAll(externalRef.verify());
			}
		}
		return retval;
	}
	// the following methods are provided to ease the migration to the SPDX 2.0 version

	/**
	 * This method has been replaced by getName() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public String getDeclaredName() {
		return this.getName();
	}

	/**
	 * This method has been replaced by getDownloadLocation() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public String getDownloadUrl() {
		return this.getDownloadLocation();
	}

	/**
	 * This method has been replaced by getSummary() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public String getShortDescription() {
		return this.getSummary();
	}

	/**
	 * This method has been replaced by getPackageFileName() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public String getFileName() {
		return this.getPackageFileName();
	}

	/**
	 * This method has been replaced by getPackageVerificationCode() to be consistent with the spec property name
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	@Deprecated
	public SpdxPackageVerificationCode getVerificationCode() throws InvalidSPDXAnalysisException {
		return this.getPackageVerificationCode();
	}

	/**
	 * This method has been replaced by getDeclaredCopyright() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public String getDeclaredCopyright() {
		return this.getCopyrightText();
	}

	/**
	 * This method has been replaced by getLicenseDeclared() to be consistent with the spec property name
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	@Deprecated
	public AnyLicenseInfo getDeclaredLicense() throws InvalidSPDXAnalysisException {
		return this.getLicenseDeclared();
	}

	/**
	 * This method has been replaced by getLicenseConcluded() to be consistent with the spec property name
	 * @return
	 */
	@Deprecated
	public AnyLicenseInfo getConcludedLicenses() {
		return this.getLicenseConcluded();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SpdxPackage pkg) {
		// sort order is determined by the name and the version
		String myNameVersion = this.getName();
		String compNameVersion = pkg.getName();
		if (myNameVersion == null) {
			myNameVersion = "";
		}
		if (compNameVersion == null) {
			compNameVersion = "";
		}
		String myVersion = this.getVersionInfo();
		if (myVersion != null) {
			myNameVersion = myNameVersion + myVersion;
		}
		String compVersion = pkg.getVersionInfo();
		if (compVersion != null) {
			compNameVersion = compNameVersion + compVersion;
		}
		return myNameVersion.compareToIgnoreCase(compNameVersion);
	}

	/**
	 * @return the Sha1 checksum value for this package, or a blank string if no
	 * sha1 checksum has been set
	 */
	public String getSha1() {
		if (this.checksums != null) {
			for (int i = 0;i < this.checksums.length; i++) {
				if (this.checksums[i].getAlgorithm().equals(ChecksumAlgorithm.checksumAlgorithm_sha1)) {
					return this.checksums[i].getValue();
				}
			}
		}
		// No sha1 found, return an empty string
		return "";
	}

	/**
	 * @param externalRef
	 * @throws InvalidSPDXAnalysisException
	 */
	public void addExternalRef(ExternalRef externalRef) throws InvalidSPDXAnalysisException {
		if (externalRef == null) {
			return;
		}
		if (this.externalRefs == null) {
			this.externalRefs = new ExternalRef[] {externalRef};
		} else {
			this.externalRefs = Arrays.copyOf(this.externalRefs, this.externalRefs.length+1);
			this.externalRefs[this.externalRefs.length-1] = externalRef;
		}
		addPropertyValue(SPDX_NAMESPACE,
				PROP_EXTERNAL_REF, externalRef);
	}
}

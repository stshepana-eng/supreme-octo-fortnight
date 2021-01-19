/**
 * Copyright (c) 2010, 2011 Source Auditor Inc.
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
package org.spdx.rdfparser;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxListedLicense;
import org.spdx.rdfparser.model.IRdfModel;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;


/**
 *
 * Simple model for the SPDX Analysis document.  The document is stored in a Jena RDF
 * model which can be accessed through the model property.
 *
 * The class should be constructed using the SPDXDocumentFactory class
 *
 * The createSpdxDocument(uri) must be called first for a blank model
 *
 * The license, file, and package objects can be constructed then added to the model
 * by using the set functions.
 *
 * The non-standard licenses must contain a unique ID of the form LicenseRef-NN where NN is a
 * unique number.  The method <code>addNonStandardLicense(licenseText)</code> can be called to
 * create a new unique Non-Standard License.
 *
 * This class is provided for compatibility with version 1.2 of the libraries - it has been
 * replaced with SpdxDocumentContainer in version 2.0 and above.
 *
 * @author Gary O'Neall
 *
 */
@Deprecated
public class SPDXDocument implements SpdxRdfConstants, IModelContainer {

	public static final String POINT_EIGHT_SPDX_VERSION = "SPDX-0.8";
	public static final String POINT_NINE_SPDX_VERSION = "SPDX-0.9";
	public static final String ONE_DOT_ZERO_SPDX_VERSION = "SPDX-1.0";
	public static final String ONE_DOT_ONE_SPDX_VERSION = "SPDX-1.1";
	public static final String ONE_DOT_TWO_SPDX_VERSION = "SPDX-1.2";
	public static final String TWO_DOT_ZERO_DOT_ZERO_VERSION = "2.0.0";

	public static final String CURRENT_SPDX_VERSION = "SPDX-2.0";

	public static final String CURRENT_IMPLEMENTATION_VERSION = "2.0.2";

	static Set<String> SUPPORTED_SPDX_VERSIONS = Sets.newHashSet();

	static {
		SUPPORTED_SPDX_VERSIONS.add(CURRENT_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(POINT_EIGHT_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(POINT_NINE_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(ONE_DOT_ZERO_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(ONE_DOT_ONE_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(ONE_DOT_TWO_SPDX_VERSION);
		SUPPORTED_SPDX_VERSIONS.add(TWO_DOT_ZERO_DOT_ZERO_VERSION);
	}

	/**
	 * Keeps tract of the next license reference number when generating the license ID's for
	 * non-standard licenses
	 */
	private AtomicInteger nextLicenseRef = new AtomicInteger(1);

	/**
	 * Simple class representing an SPDX Package.  This is stored in an RDF
	 * model.
	 *
	 * This package is initialized using an existing SPDXPackage in an
	 * RDF document by constructing the package with the node representing the
	 * SPDX package.
	 *
	 * This class is provided for compatibility with version 1.2 of the library.
	 * This class is replaced by model.SpdxPackage in version 2.0 and above.
	 *
	 * @author Gary O'Neall
	 *
	 */
	@Deprecated
	public class SPDXPackage {
		private SPDXDocument enclosingSpdxDocument;
		private Node node = null;
		/**
		 * Construct a new SPDX package and populate the properties from the node
		 * @param pkgNode Node in the RDF graph representing the SPDX package
		 */
		public SPDXPackage(Node pkgNode, SPDXDocument enclosingSpdxDocument) {
			this.node = pkgNode;
			this.enclosingSpdxDocument = enclosingSpdxDocument;
		}
		/**
		 * @return the declaredName
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getDeclaredName() throws InvalidSPDXAnalysisException {
			String[] declaredNames = findDocPropertieStringValues(this.node, PROP_PACKAGE_DECLARED_NAME);
			if (declaredNames == null || declaredNames.length == 0) {
				return null;
			}
			if (declaredNames.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one declared name for a package"));
			}
			return(declaredNames[0]);
		}

		/**
		 * @param declaredName the declaredName to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setDeclaredName(String declaredName) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_DECLARED_NAME);
			if (declaredName != null) {
				addProperty(node, PROP_PACKAGE_DECLARED_NAME, new String[] {declaredName});
			}
		}
		/**
		 * @return the fileName
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getFileName() throws InvalidSPDXAnalysisException {
			String[] fileNames = findDocPropertieStringValues(this.node, PROP_PACKAGE_FILE_NAME);
			if (fileNames == null || fileNames.length == 0) {
				return null;
			}
			if (fileNames.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one machine name for a package"));
			}
			return fileNames[0];
		}
		/**
		 * @param fileName the fileName to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setFileName(String fileName) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_FILE_NAME);
			if (fileName != null) {
				addProperty(node, PROP_PACKAGE_FILE_NAME, new String[] {fileName});
			}
		}
		/**
		 * @return the sha1
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getSha1() throws InvalidSPDXAnalysisException {

			String retval = null;
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_CHECKSUM).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				SPDXChecksum cksum = new SPDXChecksum(model, t.getObject());
				if (cksum.getAlgorithm().equals(SpdxRdfConstants.ALGORITHM_SHA1)) {
					retval = cksum.getValue();
				}
			}
			return retval;
		}
		/**
		 * @param sha1 the sha1 to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setSha1(String sha1) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_CHECKSUM);
			if (sha1 != null) {
				SPDXChecksum cksum = new SPDXChecksum(SpdxRdfConstants.ALGORITHM_SHA1, sha1);
				Resource cksumResource = cksum.createResource(model);
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_CHECKSUM);
				s.addProperty(p, cksumResource);
			}
		}
		/**
		 * @return the sourceInfo
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getSourceInfo() throws InvalidSPDXAnalysisException {
			String[] sourceInfos = findDocPropertieStringValues(this.node, PROP_PACKAGE_SOURCE_INFO);
			if (sourceInfos == null || sourceInfos.length == 0) {
				return null;
			}
			if (sourceInfos.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one source info for an SPDX package"));
			}
			return sourceInfos[0];
		}
		/**
		 * @param sourceInfo the sourceInfo to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setSourceInfo(String sourceInfo) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_SOURCE_INFO);
			if (sourceInfo != null) {
				addProperty(node, PROP_PACKAGE_SOURCE_INFO, new String[] {sourceInfo});
			}
		}

		/**
		 * @return Version information of the package
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getVersionInfo() throws InvalidSPDXAnalysisException {
			String[] versionInfos = findDocPropertieStringValues(this.node, PROP_PACKAGE_VERSION_INFO);
			if (versionInfos == null || versionInfos.length == 0) {
				return null;
			}
			if (versionInfos.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one version info for an SPDX package"));
			}
			return versionInfos[0];
		}

		/**
		 * Set the version information of the package
		 * @param versionInfo
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setVersionInfo(String versionInfo) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_VERSION_INFO);
			if (versionInfo != null) {
				addProperty(node, PROP_PACKAGE_VERSION_INFO, new String[] {versionInfo});
			}
		}

		/**
		 * @return the declaredLicenses
		 * @throws InvalidSPDXAnalysisException
		 */
		public AnyLicenseInfo getDeclaredLicense() throws InvalidSPDXAnalysisException {
			List<AnyLicenseInfo> alLic = Lists.newArrayList();
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_DECLARED_LICENSE).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				alLic.add(LicenseInfoFactory.getLicenseInfoFromModel(enclosingSpdxDocument, t.getObject()));
			}
			if (alLic.size() > 1) {
				throw(new InvalidSPDXAnalysisException("Too many declared licenses"));
			}
			if (alLic.size() == 0) {
				return null;
			}
			return alLic.get(0);
		}
		/**
		 * @param declaredLicenses the declaredLicenses to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setDeclaredLicense(AnyLicenseInfo declaredLicense) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_DECLARED_LICENSE);
			if (declaredLicense != null) {
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_DECLARED_LICENSE);

				Resource lic = declaredLicense.createResource(this.enclosingSpdxDocument);
				s.addProperty(p, lic);
			}
		}


		/**
		 * @return the detectedLicenses
		 * @throws InvalidSPDXAnalysisException
		 */
		public AnyLicenseInfo getConcludedLicenses() throws InvalidSPDXAnalysisException {
			List<AnyLicenseInfo> alLic = Lists.newArrayList();
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_CONCLUDED_LICENSE).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				alLic.add(LicenseInfoFactory.getLicenseInfoFromModel(enclosingSpdxDocument, t.getObject()));
			}
			if (alLic.size() > 1) {
				throw(new InvalidSPDXAnalysisException("Too many concluded licenses"));
			}
			if (alLic.size() == 0) {
				return null;
			}
			return alLic.get(0);
		}
		/**
		 * @param detectedLicenses the detectedLicenses to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setConcludedLicenses(AnyLicenseInfo detectedLicenses) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_CONCLUDED_LICENSE);
			if (detectedLicenses != null) {
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_CONCLUDED_LICENSE);
				Resource lic = detectedLicenses.createResource(this.enclosingSpdxDocument);
				s.addProperty(p, lic);
			}
		}
		/**
		 * @return the licenseComment
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getLicenseComment() throws InvalidSPDXAnalysisException {
			String[] comments = findDocPropertieStringValues(this.node, PROP_PACKAGE_LICENSE_COMMENT);
			if (comments == null || comments.length == 0) {
				return null;
			}
			if (comments.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one license comment for a package"));
			}
			return(comments[0]);
		}
		/**
		 * @param comments the license comments to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setLicenseComment(String comments) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_LICENSE_COMMENT);
			if (comments != null) {
				addProperty(node, PROP_PACKAGE_LICENSE_COMMENT, new String[] {comments});
			}
		}
		/**
		 * @return the declaredCopyright
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getDeclaredCopyright() throws InvalidSPDXAnalysisException {
			String[] copyrights = findDocPropertieStringValues(this.node, PROP_PACKAGE_DECLARED_COPYRIGHT);
			if (copyrights == null || copyrights.length == 0) {
				return null;
			}
			if (copyrights.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one declared copyright for a package"));
			}
			return(copyrights[0]);
		}
		/**
		 * @param declaredCopyright the declaredCopyright to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setDeclaredCopyright(String declaredCopyright) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_DECLARED_COPYRIGHT);
			if (declaredCopyright != null) {
				addProperty(node, PROP_PACKAGE_DECLARED_COPYRIGHT, new String[] {declaredCopyright});
			}
		}
		/**
		 * @return the shortDescription
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getShortDescription() throws InvalidSPDXAnalysisException {
			String[] shortDescs = findDocPropertieStringValues(this.node, PROP_PACKAGE_SHORT_DESC);
			if (shortDescs == null || shortDescs.length == 0) {
				return null;
			}
			if (shortDescs.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one short description for a package"));
			}
			return(shortDescs[0]);
		}
		/**
		 * @param shortDescription the shortDescription to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setShortDescription(String shortDescription) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_SHORT_DESC);
			if (shortDescription != null) {
				addProperty(node, PROP_PACKAGE_SHORT_DESC, new String[] {shortDescription});
			}
		}
		/**
		 * @return the description
		 * @throws InvalidSPDXAnalysisException
		 */
		public String getDescription() throws InvalidSPDXAnalysisException {
			String[] desc = findDocPropertieStringValues(this.node, PROP_PACKAGE_DESCRIPTION);
			if (desc == null || desc.length == 0) {
				return null;
			}
			if (desc.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one description for a package"));
			}
			return(desc[0]);
		}
		/**
		 * @param description the description to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setDescription(String description) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_DESCRIPTION);
			if (description != null) {
				addProperty(node, PROP_PACKAGE_DESCRIPTION, new String[] {description});
			}
		}

		/**
		 * Set the originator
		 * @param originator Either a valid originator string or NOASSERTION
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setOriginator(String originator) throws InvalidSPDXAnalysisException {
			if (originator == null) {
				removeProperties(node, PROP_PACKAGE_ORIGINATOR);
				return;
			}
			String error = SpdxVerificationHelper.verifyOriginator(originator);
			if (error != null && !error.isEmpty()) {
				throw(new InvalidSPDXAnalysisException(error));
			}
			removeProperties(node, PROP_PACKAGE_ORIGINATOR);
			addProperty(node, PROP_PACKAGE_ORIGINATOR, new String[] {originator});
		}

		/**
		 * Set the Supplier
		 * @param supplier Either a valid originator string or NOASSERTION
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setSupplier(String supplier) throws InvalidSPDXAnalysisException {
			if (supplier == null) {
				removeProperties(node, PROP_PACKAGE_SUPPLIER);
				return;
			}
			String error = SpdxVerificationHelper.verifySupplier(supplier);
			if (error != null && !error.isEmpty()) {
				throw(new InvalidSPDXAnalysisException(error));
			}
			removeProperties(node, PROP_PACKAGE_SUPPLIER);
			addProperty(node, PROP_PACKAGE_SUPPLIER, new String[] {supplier});
		}

		public String getOriginator() throws InvalidSPDXAnalysisException {
			String[] originators = findDocPropertieStringValues(this.node, PROP_PACKAGE_ORIGINATOR);
			if (originators == null || originators.length == 0) {
				return null;
			}
			if (originators.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one originator for a package"));
			}
			return(originators[0]);
		}

		public String getSupplier() throws InvalidSPDXAnalysisException {
			String[] suppliers = findDocPropertieStringValues(this.node, PROP_PACKAGE_SUPPLIER);
			if (suppliers == null || suppliers.length == 0) {
				return null;
			}
			if (suppliers.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one supplier for a package"));
			}
			return(suppliers[0]);
		}

		/**
		 * @return the files
		 * @throws InvalidSPDXAnalysisException
		 */
		public SPDXFile[] getFiles() throws InvalidSPDXAnalysisException {
			// files
			List<SPDXFile> alFiles = Lists.newArrayList();
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_FILE).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				alFiles.add(new SPDXFile(enclosingSpdxDocument, t.getObject()));
			}
			SPDXFile[] retval = new SPDXFile[alFiles.size()];
			return alFiles.toArray(retval);
		}
		/**
		 * @param files the files to set
		 * @throws InvalidSPDXAnalysisException
		 */
		public void setFiles(SPDXFile[] files) throws InvalidSPDXAnalysisException {
			// Delete all existing files
			List<Node> alFileNodes = Lists.newArrayList();
			Node n = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_FILE).asNode();
			Triple m = Triple.createMatch(this.node, n, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				alFileNodes.add(t.getObject());
			}
			removeProperties(node, PROP_PACKAGE_FILE);
			removeProperties(getSpdxDocNode(), PROP_SPDX_FILE_REFERENCE);	// NOTE: In version 2.0, we will need to remove just the files which were in the package

			for (Node fileNode : alFileNodes) {
				model.removeAll(getResource(fileNode), null, null);
			}

			if (files != null) {
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_FILE);
				Resource docResource = getResource(getSpdxDocNode());
				Property docP = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_FILE_REFERENCE);
				for (int i = 0; i < files.length; i++) {
					Resource file = files[i].createResource(getDocument(), getDocumentNamespace() + getNextSpdxElementRef());
					s.addProperty(p, file);
					docResource.addProperty(docP, file);
				}
			}
		}
		/**
		 * Add a file to the package
		 * @param file
		 */
		public void addFile(SPDXFile file) throws InvalidSPDXAnalysisException {
			Resource s = getResource(this.node);
			Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_FILE);
			Resource docResource = getResource(getSpdxDocNode());
			Property docP = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_FILE_REFERENCE);
			Resource fileResource = file.createResource(getDocument(), getDocumentNamespace() + getNextSpdxElementRef());
			s.addProperty(p, fileResource);
			docResource.addProperty(docP, fileResource);
		}

		/**
		 * Removes all SPDX files by the given name
		 * @param fileName Name of SPDX file to be removed
		 * @throws InvalidSPDXAnalysisException
		 */
		public void removeFile(String fileName) throws InvalidSPDXAnalysisException {
			List<Node> filesToRemove = Lists.newArrayList();
			Node fileNameProperty = model.getProperty(SPDX_NAMESPACE, PROP_FILE_NAME).asNode();
			Property docFileProperty = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_FILE_REFERENCE);
			Property pkgFileProperty = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_FILE);
			Resource docResource = getResource(getSpdxDocNode());
			Resource pkgResource = getResource(this.node);
			Triple m = Triple.createMatch(getSpdxDocNode(), docFileProperty.asNode(), null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			//TODO: See if there is a more efficient search method for files
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				Node fileObject = t.getObject();
				Triple fileNameMatch = Triple.createMatch(fileObject, fileNameProperty, null);
				ExtendedIterator<Triple> fileNameIterator = model.getGraph().find(fileNameMatch);
				while (fileNameIterator.hasNext()) {
					Triple fileNameTriple = fileNameIterator.next();
					String searchFileName = fileNameTriple.getObject().toString(false);
					if (searchFileName.equals(fileName)) {
						filesToRemove.add(fileObject);
					}
				}
			}
			for (int i = 0; i < filesToRemove.size(); i++) {
				// remove the references files
				RDFNode o = model.getRDFNode(filesToRemove.get(i));
				model.removeAll(docResource, docFileProperty, o);
				// remove the package files
				model.removeAll(pkgResource, pkgFileProperty, o);
			}
		}
		public String getDownloadUrl() throws InvalidSPDXAnalysisException {
			String[] urls = findDocPropertieStringValues(this.node, PROP_PACKAGE_DOWNLOAD_URL);
			if (urls == null || urls.length == 0) {
				return null;
			}
			if (urls.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one URL for a package"));
			}
			return(urls[0]);
		}

		public void setDownloadUrl(String url) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_DOWNLOAD_URL);
			if (url != null) {
				addProperty(node, PROP_PACKAGE_DOWNLOAD_URL, new String[] {url});
			}
		}

		public String getHomePage() throws InvalidSPDXAnalysisException {
			String[] urls = findDocPropertieStringValues(this.node, DOAP_NAMESPACE, PROP_PROJECT_HOMEPAGE);
			if (urls == null || urls.length == 0) {
				return null;
			}
			if (urls.length > 1) {
				throw(new InvalidSPDXAnalysisException("More than one home page for a package"));
			}
			return(urls[0]);
		}

		public void setHomePage(String url) throws InvalidSPDXAnalysisException {
			removeProperties(node, DOAP_NAMESPACE, PROP_PROJECT_HOMEPAGE);
			if (url != null) {
				addProperty(node, DOAP_NAMESPACE, PROP_PROJECT_HOMEPAGE, new String[] {url});
			}
		}

		public SpdxPackageVerificationCode getVerificationCode() throws InvalidSPDXAnalysisException {
			SpdxPackageVerificationCode retval = null;
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_VERIFICATION_CODE).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				retval = new SpdxPackageVerificationCode(model, t.getObject());
			}
			return retval;
		}

		public void setVerificationCode(SpdxPackageVerificationCode verificationCode) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_VERIFICATION_CODE);
			if (verificationCode != null) {
				Resource verificationCodeResource = verificationCode.createResource(model);
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_VERIFICATION_CODE);
				s.addProperty(p, verificationCodeResource);
			}
		}
		public SPDXPackageInfo getPackageInfo() throws InvalidSPDXAnalysisException {
			return new SPDXPackageInfo(this.getDeclaredName(), this.getVersionInfo(), this.getFileName(),
					this.getSha1(), this.getSourceInfo(), this.getDeclaredLicense(),
					this.getConcludedLicenses(), this.getLicenseInfoFromFiles(),
					this.getLicenseComment(), this.getDeclaredCopyright(),
					this.getShortDescription(), this.getDescription(), this.getDownloadUrl(),
					this.getVerificationCode(), this.getSupplier(), this.getOriginator(),
					this.getHomePage());
		}

		public void setLicenseInfoFromFiles(AnyLicenseInfo[] licenseInfo) throws InvalidSPDXAnalysisException {
			removeProperties(node, PROP_PACKAGE_LICENSE_INFO_FROM_FILES);
			if (licenseInfo != null) {
				Resource s = getResource(this.node);
				Property p = model.createProperty(SPDX_NAMESPACE, PROP_PACKAGE_LICENSE_INFO_FROM_FILES);
				for (int i = 0; i < licenseInfo.length; i++) {
					Resource lic = licenseInfo[i].createResource(this.enclosingSpdxDocument);
					s.addProperty(p, lic);
				}
			}
		}

		public AnyLicenseInfo[] getLicenseInfoFromFiles() throws InvalidSPDXAnalysisException {
			List<AnyLicenseInfo> alLic = Lists.newArrayList();
			Node p = model.getProperty(SPDX_NAMESPACE, PROP_PACKAGE_LICENSE_INFO_FROM_FILES).asNode();
			Triple m = Triple.createMatch(this.node, p, null);
			ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
			while (tripleIter.hasNext()) {
				Triple t = tripleIter.next();
				alLic.add(LicenseInfoFactory.getLicenseInfoFromModel(enclosingSpdxDocument, t.getObject()));
			}
			AnyLicenseInfo[] retval = new AnyLicenseInfo[alLic.size()];
			retval = alLic.toArray(retval);
			return retval;
		}

		/**
		 * Clones a deep copy of all fields to a new SPDXPackage contained in the docToCloneTo SPDXDocument.
		 * NOTE: This will overwrite any existing SPDXPackages contained within the SPDXDocument and the SPDXDocument must not contain any extracted licenses
		 * @param docToCloneTo SPDX Document to contain the result of the clone
		 * @param packageUri URI for the SPDX Package being created
		 * @return A deep copy of this SPDXPackage contained within docToCloneTo
		 * @throws InvalidSPDXAnalysisException
		 */
		public SPDXPackage clone(SPDXDocument docToCloneTo, String packageUri) throws InvalidSPDXAnalysisException {
			if (docToCloneTo.getExtractedLicenseInfos() != null && docToCloneTo.getExtractedLicenseInfos().length > 0) {
				throw(new InvalidSPDXAnalysisException("Can not clone a package to an SPDX document with existing licenses"));
			}
			if (docToCloneTo.getFileReferences()!= null && docToCloneTo.getFileReferences().length > 0) {
				throw(new InvalidSPDXAnalysisException("Can not clone a package to an SPDX document with existing files"));
			}
			//TODO: Allow cloning of existing licenses and files by merging the extracted license infos and mapping the licenses
			SPDXPackage retval = docToCloneTo.createSpdxPackage(packageUri);
			// need to copy the non-standard licenses in case they are referenced
			ExtractedLicenseInfo[] extractedLicenseInfos = getExtractedLicenseInfos();
			if (extractedLicenseInfos != null) {
				docToCloneTo.setExtractedLicenseInfos(extractedLicenseInfos);
				docToCloneTo.initializeNextLicenseRef(extractedLicenseInfos);
			}
			retval.setConcludedLicenses(this.getConcludedLicenses().clone());
			retval.setDeclaredCopyright(this.getDeclaredCopyright());
			retval.setDeclaredLicense(this.getDeclaredLicense().clone());
			retval.setDeclaredName(this.getDeclaredName());
			retval.setDescription(this.getDescription());
			retval.setDownloadUrl(this.getDownloadUrl());
			retval.setFileName(this.getFileName());
			SPDXFile[] myFiles = this.getFiles();
			if (myFiles != null) {
				SPDXFile[] clonedFiles = new SPDXFile[myFiles.length];
				for (int i = 0; i < clonedFiles.length; i++) {
					// For the refactored code, replace the following by a call to clone, call to setId, then a call to createResource
					clonedFiles[i] = myFiles[i].clone(docToCloneTo, docToCloneTo.getDocumentNamespace() + docToCloneTo.getNextSpdxElementRef());
				}
				retval.setFiles(clonedFiles);
			}
			retval.setHomePage(this.getHomePage());
			retval.setLicenseComment(this.getLicenseComment());
			AnyLicenseInfo[] licenseInfosFromFiles = this.getLicenseInfoFromFiles();
			if (licenseInfosFromFiles != null) {
				AnyLicenseInfo[] clonedLicenseInfosFromFiles = new AnyLicenseInfo[licenseInfosFromFiles.length];
				for (int i = 0; i < clonedLicenseInfosFromFiles.length; i++) {
					clonedLicenseInfosFromFiles[i] = licenseInfosFromFiles[i].clone();
				}
				retval.setLicenseInfoFromFiles(clonedLicenseInfosFromFiles);
			}
			retval.setOriginator(this.getOriginator());
			retval.setSha1(this.getSha1());
			retval.setShortDescription(this.getShortDescription());
			retval.setSourceInfo(this.getSourceInfo());
			retval.setSupplier(this.getSupplier());
			retval.setVerificationCode(this.getVerificationCode());
			retval.setVersionInfo(this.getVersionInfo());
			// need to reset the next reference numbers in the toDocument
			return retval;
		}

		/**
		 * @return Array list of any error messages found in verifying the package model
		 */
		public List<String> verify() {
			List<String> retval = Lists.newArrayList();
			// name
			try {
				String name = this.getDeclaredName();
				if (name == null || name.isEmpty()) {
					retval.add("Missing required name for package");
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid name: "+e.getMessage());
			}
			// summary
			try {
				@SuppressWarnings("unused")
				String summary = this.getShortDescription();
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid summary: "+e.getMessage());
			}
			// description
			try {
				@SuppressWarnings("unused")
				String description = this.getDescription();
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid description: "+e.getMessage());
			}
			// download location
			try {
				String downloadLocation = this.getDownloadUrl();
				if (downloadLocation == null || downloadLocation.isEmpty()) {
					retval.add("Missing required download location for package");
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid download location: "+e.getMessage());
			}
			// checksum
			try {
				String checksum = this.getSha1();
				if (checksum != null && !checksum.isEmpty()) {
					String verify = SpdxVerificationHelper.verifyChecksumString(checksum);
					if (verify != null) {
						retval.add("Package checksum error: "+verify);
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid checksum: "+e.getMessage());
			}
			// source Info - optional
			try {
				@SuppressWarnings("unused")
				String sourceInfo = this.getSourceInfo();
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package source info: "+e.getMessage());
			}

			// copyright text - mandatory
			try {
				String copyrightText = this.getDeclaredCopyright();
				if (copyrightText == null || copyrightText.isEmpty()) {
					retval.add("Missing required package copyright text");
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package copyright: "+e.getMessage());
			}

			// license comments - optional
			try {
				@SuppressWarnings("unused")
				String licenseComments = this.getLicenseComment();
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid license comments: "+e.getMessage());
			}

			// license declared - mandatory - 1 (need to change return values)
			try {
				AnyLicenseInfo declaredLicense = this.getDeclaredLicense();
				if (declaredLicense == null) {
					retval.add("Missing required declared license");
				} else {
					retval.addAll(declaredLicense.verify());
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package declared license: "+e.getMessage());
			}

			// license concluded - mandatory - 1 (need to change return values)
			try {
				AnyLicenseInfo concludedLicense = this.getConcludedLicenses();
				if (concludedLicense == null) {
					retval.add("Missing required concluded license");
				} else {
					retval.addAll(concludedLicense.verify());
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package concluded license: "+e.getMessage());
			}

			// license infos from files - mandatory - 1 or more
			try {
				AnyLicenseInfo[] licenseInfosFromFiles = this.getLicenseInfoFromFiles();
				if (licenseInfosFromFiles == null || licenseInfosFromFiles.length == 0) {
					retval.add("Missing required license information from files");
				} else {
					for (int i = 0; i < licenseInfosFromFiles.length; i++) {
						retval.addAll(licenseInfosFromFiles[i].verify());
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package license information from files: "+e.getMessage());
			}

			// hasFiles mandatory one or more
			try {
				SPDXFile[] files = this.getFiles();
				if (files == null || files.length == 0) {
					retval.add("Missing required package files");
				} else {
					for (int i = 0; i < files.length; i++) {
						retval.addAll(files[i].verify());
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package files: "+e.getMessage());
			}

			// verification code
			SpdxPackageVerificationCode verificationCode = null;
			try {
				verificationCode = this.getVerificationCode();
				if (verificationCode == null) {
					retval.add("Missing required package verification code.");
				} else {
					retval.addAll(verificationCode.verify());
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid package verification code: "+e.getMessage());
			}

			// supplier
			String supplier = null;
			try {
				supplier = this.getSupplier();
				if (supplier != null) {
					String error = SpdxVerificationHelper.verifySupplier(supplier);
					if (error != null && !error.isEmpty()) {
						retval.add("Supplier error - "+error);
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid supplier: "+e.getMessage());
			}

			// originator
			String originator = null;
			try {
				originator = this.getOriginator();
				if (originator != null) {
					String error = SpdxVerificationHelper.verifyOriginator(originator);
					if (error != null && !error.isEmpty()) {
						retval.add("Originator error - "+error);
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid originator: "+e.getMessage());
			}
			return retval;
		}
	}

	Model model;
	SPDXPackage spdxPackage = null;
	/**
	 * Namespace for all SPDX document elements
	 */
	private String documentNamespace;
	private AtomicInteger nextElementRef = new AtomicInteger(0);

	public SPDXDocument(Model model) throws InvalidSPDXAnalysisException {
		this.model = model;
		initialize();

	}

	/**
	 * Initialize the next license ref and next element reference variables
	 * @return any verification errors encountered
	 * @throws InvalidSPDXAnalysisException
	 */
	private List<String> initialize() throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode != null) {	// not empty - we should verify
			if (!spdxDocNode.isURI()) {
				throw(new InvalidSPDXAnalysisException("SPDX Documents must have a unique URI"));
			}
			String docUri = spdxDocNode.getURI();
			this.documentNamespace = this.formDocNamespace(docUri);
			List<String> errors = verify();
			initializeNextLicenseRef();
			initializeNextElementRef();
			return errors;
		} else {
			return Lists.newArrayList();
		}
	}

	/**
	 * Initialize the next SPDX element reference used for creating new SPDX element URIs
	 */
	private void initializeNextElementRef() {
		int highestElementRef = 0;
		Triple m = Triple.createMatch(null, null, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	// find everything
		while (tripleIter.hasNext()) {
			// iterate through everything looking for matches to this SPDX document URI
			Triple trip = tripleIter.next();
			if (trip.getSubject().isURI()) {	// check the subject
				String subjectUri = trip.getSubject().getURI();
				if (subjectUri.startsWith(this.documentNamespace + SPDX_ELEMENT_REF_PRENUM)) {
					String elementRef = subjectUri.substring(this.documentNamespace.length());
					if (SPDX_ELEMENT_REF_PATTERN.matcher(elementRef).matches()) {
						int elementRefNum = getElementRefNumber(elementRef);
						if (elementRefNum > highestElementRef) {
							highestElementRef = elementRefNum;
						}
					}
				}
			}
			if (trip.getObject().isURI()) {		// check the object
				String objectUri = trip.getObject().getURI();
				if (objectUri.startsWith(this.documentNamespace + SPDX_ELEMENT_REF_PRENUM)) {
					String elementRef = objectUri.substring(this.documentNamespace.length());
					if (SPDX_ELEMENT_REF_PATTERN.matcher(elementRef).matches()) {
						int elementRefNum = getElementRefNumber(elementRef);
						if (elementRefNum > highestElementRef) {
							highestElementRef = elementRefNum;
						}
					}
				}
			}
		}

		this.nextElementRef.set(highestElementRef + 1);
	}

	/**
	 * Parses out the reference number for an SPDX element reference
	 * @param elementReference Element reference to parse
	 * @return element reference or -1 if the element reference is not valid
	 */
	public static int getElementRefNumber(String elementReference) {
		String numPart = elementReference.substring(SPDX_ELEMENT_REF_PRENUM.length());
		try {
			return Integer.parseInt(numPart);
		} catch(Exception ex) {
			return -1;
		}
	}

	/**
	 * Initialize the next license reference by scanning all of the existing non-standard licenses
	 * @throws InvalidSPDXAnalysisException
	 */
	protected void initializeNextLicenseRef() throws InvalidSPDXAnalysisException {
		initializeNextLicenseRef(this.getExtractedLicenseInfos());
	}

	protected void initializeNextLicenseRef(ExtractedLicenseInfo[] existingLicenses) throws InvalidSPDXAnalysisException {
		int highestNonStdLicense = 0;
		for (int i = 0; i < existingLicenses.length; i++) {
			try {
			int idNum = getLicenseRefNum(existingLicenses[i].getLicenseId());
			if (idNum > highestNonStdLicense) {
				highestNonStdLicense = idNum;
			}
			} catch (NonNumericLicenseIdException ex) {
				// just continue
			}
		}
		this.nextLicenseRef.set(highestNonStdLicense + 1);
	}

	/**
	 * Parses a license ID and return the integer representing the ID number (e.g. N in LicenseRef-N)
	 * Note that in SPDX 1.2, non-numeric license IDs are allowed. This method will throw a NonNumericException if
	 * a non numeric license ID passed as a licenseID parameter
	 * @param licenseID
	 * @return
	 * @throws NonNumericLicenseIdException If the non-standard license ID is not of the form LicenseRef-NN
	 */
	public int getLicenseRefNum(String licenseID) throws NonNumericLicenseIdException {
		Matcher matcher = LICENSE_ID_PATTERN_NUMERIC.matcher(licenseID);
		if (!matcher.matches()) {
			throw(new NonNumericLicenseIdException("Invalid license ID found in the non-standard licenses: '"+licenseID+"'"));
		}
		int numGroups = matcher.groupCount();
		if (numGroups != 1) {
			throw(new NonNumericLicenseIdException("Invalid license ID found in the non-standard licenses: '"+licenseID+"'"));
		}
		int idNum = Integer.decode(matcher.group(1));
		return idNum;
	}

	public static String formNonStandardLicenseID(int idNum) {
		return NON_STD_LICENSE_ID_PRENUM + String.valueOf(idNum);
	}

	public String verifySpdxVersion(String spdxVersion) {
		if (!spdxVersion.startsWith("SPDX-")) {
			return "Invalid spdx version - must start with 'SPDX-'";
		}
		Matcher docSpecVersionMatcher = SpdxRdfConstants.SPDX_VERSION_PATTERN.matcher(spdxVersion);
		if (!docSpecVersionMatcher.matches()) {
			return "Invalid spdx version format - must match 'SPDX-M.N'";
		}
		return null;	// if we got here, there is no problem
	}
	/**
	 * Verifies the spdx document
	 * @return error messages for any fields which do not match the spec.  Return an empty array list if no issues.
	 */
	public List<String> verify() {
		List<String> retval = Lists.newArrayList();
		// specVersion
		String docSpecVersion = "";	// note - this is used later in verify to verify version specific info
		try {
			docSpecVersion = this.getSpdxVersion();
			if (docSpecVersion == null || docSpecVersion.isEmpty()) {
				retval.add("Missing required SPDX version");
			} else {
				String verify = verifySpdxVersion(docSpecVersion);
				if (verify != null) {
					retval.add(verify);
				} else {
					if (!SUPPORTED_SPDX_VERSIONS.contains(docSpecVersion)) {
						retval.add("Version "+docSpecVersion+" is not supported by this version of the rdf parser");
					}
				}
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid spec version: "+e.getMessage());
		}
		// creationInfo
		try {
			SPDXCreatorInformation creator = this.getCreatorInfo();
			if (creator == null) {
				retval.add("Missing required Creator");
			} else {
				List<String> creatorVerification = creator.verify();
				retval.addAll(creatorVerification);
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid creator information: "+e.getMessage());
		}
		// Package
		try {
			SPDXPackage sPkg = this.getSpdxPackage();
			if (sPkg == null) {
				retval.add("Missing required SPDX Package");
			} else {
				List<String> packageVerification = sPkg.verify();
				retval.addAll(packageVerification);
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid SPDX Package: "+e.getMessage());
		}
		// Reviewers
		try {
			SPDXReview[] reviews = this.getReviewers();
			if (reviews != null) {
				for (int i = 0; i < reviews.length; i++) {
					List<String> reviewerVerification = reviews[i].verify();
					retval.addAll(reviewerVerification);
				}
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid reviewers: "+e.getMessage());
		}
		// Non standard licenses
		try {
			ExtractedLicenseInfo[] extractedLicInfos = this.getExtractedLicenseInfos();
			if (extractedLicInfos != null) {
				for (int i = 0; i < extractedLicInfos.length; i++) {
					List<String> extractedLicInfoVerification = extractedLicInfos[i].verify();
					retval.addAll(extractedLicInfoVerification);
				}
			}
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Invalid extracted licensing info: "+e.getMessage());
		}
		// data license
		if (docSpecVersion != null && !docSpecVersion.equals(POINT_EIGHT_SPDX_VERSION) && !docSpecVersion.equals(POINT_NINE_SPDX_VERSION)) { // added as a mandatory field in 1.0
			try {
				SpdxListedLicense dataLicense = this.getDataLicense();
				if (dataLicense == null) {
					retval.add("Missing required data license");
				}else if (docSpecVersion.equals(ONE_DOT_ZERO_SPDX_VERSION)) {
					if (!dataLicense.getLicenseId().equals(SPDX_DATA_LICENSE_ID_VERSION_1_0)) {
						retval.add("Incorrect data license for SPDX version 1.0 document - found "+dataLicense.getLicenseId()+", expected "+SPDX_DATA_LICENSE_ID_VERSION_1_0);
					}
				} else {
					if (!dataLicense.getLicenseId().equals(SPDX_DATA_LICENSE_ID)) {
						retval.add("Incorrect data license for SPDX document - found "+dataLicense.getLicenseId()+", expected "+SPDX_DATA_LICENSE_ID);
					}
				}
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Invalid data license: "+e.getMessage());
			}
		}
		// document comment
		try {
			String[] comments = findDocPropertieStringValues(getSpdxDocNode(), RDFS_NAMESPACE, RDFS_PROP_COMMENT);
			if (comments.length > 1) {
				retval.add("More than one document comment exists for the SPDX Package");
			}
		}
		catch(Exception e) {
			retval.add("Invalid document comment: "+e.getMessage());
		}
		return retval;
	}

	/**
	 * Find all property string values belonging to the subject
	 * @param subject
	 * @param propertyName
	 * @return string values of the properties or null if the subject or propertyName is null
	 */
	private String[] findDocPropertieStringValues(Node subject, String propertyName) {
		return findDocPropertieStringValues(subject, SPDX_NAMESPACE, propertyName);
	}

	/**
	 * Find all property string values belonging to the subject
	 * @param subject
	 * @param nameSpace
	 * @param propertyName
	 * @return string values of the properties or null if the subject or propertyName is null
	 */
	private String[] findDocPropertieStringValues(Node subject, String nameSpace, String propertyName) {
		if (subject == null || propertyName == null) {
			return null;
		}
		List<String> alResult = Lists.newArrayList();
		Node p = model.getProperty(nameSpace, propertyName).asNode();
		Triple m = Triple.createMatch(subject, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			if (t.getObject().isURI()) {
				if (t.getObject().getURI().equals(SpdxRdfConstants.URI_VALUE_NONE)) {
					alResult.add(SpdxRdfConstants.NONE_VALUE);
				} else if (t.getObject().getURI().equals(SpdxRdfConstants.URI_VALUE_NOASSERTION)) {
					alResult.add(SpdxRdfConstants.NOASSERTION_VALUE);
				} else {
					alResult.add(t.getObject().toString(false));
				}
			} else {
				alResult.add(t.getObject().toString(false));
			}
		}
		String[] retval = new String[alResult.size()];
		return alResult.toArray(retval);
	}

	/**
	 * Remove all properties by the property name from the subject node
	 * @param node
	 * @param propertyName
	 * @throws InvalidSPDXAnalysisException
	 */
	private void removeProperties(Node subject, String propertyName) throws InvalidSPDXAnalysisException {
		removeProperties(subject, SPDX_NAMESPACE, propertyName);
	}

	/**
	 * Remove all properties by the property name within the nameSpace from the subject node
	 * @param node
	 * @param propertyName
	 * @throws InvalidSPDXAnalysisException
	 */
	private void removeProperties(Node subject, String nameSpace, String propertyName) throws InvalidSPDXAnalysisException {
		Property p = model.getProperty(nameSpace, propertyName);
		Resource s = getResource(subject);
		model.removeAll(s, p, null);
	}

	private Resource getResource(Node node) throws InvalidSPDXAnalysisException {
		Resource s;
		if (node.isURI()) {
			s = model.createResource(node.getURI());
		} else if (node.isBlank()) {
			s = model.createResource(new AnonId(node.getBlankNodeId()));
		} else {
			throw(new InvalidSPDXAnalysisException("Node can not be a literal"));
		}
		return s;
	}

	/**
	 * Add a literal string property value
	 * @param subject
	 * @param propertyName
	 * @param propertyValue
	 * @throws InvalidSPDXAnalysisException
	 */
	private void addProperty(Node subject, String propertyName, String[] propertyValue) throws InvalidSPDXAnalysisException {
		addProperty(subject, SPDX_NAMESPACE, propertyName, propertyValue);
	}

	/**
	 * Add a literal string property value
	 * @param subject
	 * @param nameSpace
	 * @param propertyName
	 * @param propertyValue
	 * @throws InvalidSPDXAnalysisException
	 */
	private void addProperty(Node subject, String nameSpace, String propertyName, String[] propertyValue) throws InvalidSPDXAnalysisException {
		Resource s = getResource(subject);
		for (int i = 0; i < propertyValue.length; i++) {
			Property p = model.createProperty(nameSpace, propertyName);
			if (propertyValue[i].equals(SpdxRdfConstants.NONE_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NONE);
				s.addProperty(p, r);
			} else if (propertyValue[i].equals(SpdxRdfConstants.NOASSERTION_VALUE)) {
				Resource r = model.createResource(SpdxRdfConstants.URI_VALUE_NOASSERTION);
				s.addProperty(p, r);
			} else {
				s.addProperty(p, propertyValue[i]);
			}
		}
	}

	/**
	 * @return the spdxVersion or null if there is no SPDX document
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getSpdxVersion() throws InvalidSPDXAnalysisException {
		String[] versions = findDocPropertieStringValues(getSpdxDocNode(), PROP_SPDX_VERSION);
		if (versions == null || versions.length == 0) {
			return null;
		}
		if (versions.length > 1) {
			throw(new InvalidSPDXAnalysisException("More than one version exists for the SPDX Document"));
		}
		return versions[0];
	}

	/**
	 * @param comment the documentComment to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setDocumentComment(String comment) throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must create the SPDX document before setting spdxVersion"));
		}
		removeProperties(spdxDocNode, RDFS_NAMESPACE, RDFS_PROP_COMMENT);
		if (comment != null && !comment.isEmpty()) {
			addProperty(spdxDocNode, RDFS_NAMESPACE, RDFS_PROP_COMMENT, new String[] {comment});
		}
	}

	/**
	 * @return the documentComment or null if there is no SPDX document or SPDX document comment
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getDocumentComment() throws InvalidSPDXAnalysisException {
		String[] comments = findDocPropertieStringValues(getSpdxDocNode(), RDFS_NAMESPACE, RDFS_PROP_COMMENT);
		if (comments == null || comments.length == 0) {
			return null;
		}
		if (comments.length > 1) {
			throw(new InvalidSPDXAnalysisException("More than one document comment exists for the SPDX Document"));
		}
		return comments[0];
	}

	/**
	 * @param spdxVersion the spdxVersion to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setSpdxVersion(String spdxVersion) throws InvalidSPDXAnalysisException {
		String versionVerify = verifySpdxVersion(spdxVersion);
		if (versionVerify != null && !versionVerify.isEmpty()) {
			throw(new InvalidSPDXAnalysisException(versionVerify));
		}
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must create the SPDX document before setting spdxVersion"));
		}
		removeProperties(spdxDocNode, PROP_SPDX_VERSION);
		addProperty(spdxDocNode, PROP_SPDX_VERSION, new String[] {spdxVersion});
	}

	public SpdxListedLicense getDataLicense() throws InvalidSPDXAnalysisException {
		List<AnyLicenseInfo> alLic = Lists.newArrayList();
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_DATA_LICENSE).asNode();
		Triple m = Triple.createMatch(getSpdxDocNode(), p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alLic.add(LicenseInfoFactory.getLicenseInfoFromModel(this, t.getObject()));
		}
		if (alLic.size() > 1) {
			throw(new InvalidSPDXAnalysisException("Too many data licenses"));
		}
		if (alLic.size() == 0) {
			return null;
		}
		if (!(alLic.get(0) instanceof SpdxListedLicense)) {
			throw(new InvalidSPDXAnalysisException("Incorrect license for datalicense - must be a standard SPDX license type"));
		}
		return (SpdxListedLicense)(alLic.get(0));
	}

	public void setDataLicense(SpdxListedLicense dataLicense) throws InvalidSPDXAnalysisException {
		String spdxVersion = this.getSpdxVersion();
		if (spdxVersion == null) {
			throw(new InvalidSPDXAnalysisException("Can not set a data license - document does not contain a version.  Set the SPDX version property before setting the data license."));
		}
		if (spdxVersion.equals(ONE_DOT_ZERO_SPDX_VERSION)) {
			if (!dataLicense.getLicenseId().equals(SPDX_DATA_LICENSE_ID_VERSION_1_0)) {
				throw(new InvalidSPDXAnalysisException("Invalid data license for version 1 SPDX document - license must have ID "+SPDX_DATA_LICENSE_ID_VERSION_1_0));
			}
		} else {
			if (!dataLicense.getLicenseId().equals(SPDX_DATA_LICENSE_ID)) {
				throw(new InvalidSPDXAnalysisException("Invalid data license for SPDX document - license must have ID "+SPDX_DATA_LICENSE_ID));
			}
		}

		removeProperties(getSpdxDocNode(), PROP_SPDX_DATA_LICENSE);
		Resource s = getResource(getSpdxDocNode());
		Property p = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_DATA_LICENSE);

		Resource lic = dataLicense.createResource(this);
		s.addProperty(p, lic);
	}

	public SPDXFile[] getFileReferences() throws InvalidSPDXAnalysisException {
		List<SPDXFile> alFiles = Lists.newArrayList();
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_FILE_REFERENCE).asNode();
		Triple m = Triple.createMatch(getSpdxDocNode(), p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alFiles.add(new SPDXFile(this, t.getObject()));
		}
		SPDXFile[] retval = new SPDXFile[alFiles.size()];
		return alFiles.toArray(retval);
	}

	@Deprecated
	/**
	 * This method id deprecated - please use the getCreator() method for this information
	 * @return the The creators of the Analysis
	 * @throws InvalidSPDXAnalysisException
	 */
	public String[] getCreators() throws InvalidSPDXAnalysisException {
		SPDXCreatorInformation creator = getCreatorInfo();
		if (creator != null && creator.getCreators() != null) {
			return creator.getCreators();
		} else {
			return null;
		}
	}

	public SPDXCreatorInformation getCreatorInfo() throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("No SPDX Document was found.  Can not access the creator information"));
		}
		List<SPDXCreatorInformation> als = Lists.newArrayList();
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_CREATION_INFO).asNode();
		Triple m = Triple.createMatch(spdxDocNode, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			als.add(new SPDXCreatorInformation(model, t.getObject()));
		}
		if (als.size() > 1) {
			throw(new InvalidSPDXAnalysisException("Too many creation information for document.  Only one is allowed."));
		}
		if (als.size() > 0) {
			return als.get(0);
		} else {
			return null;
		}
	}

	/**
	 * @param creators the creators of the analysis
	 * @throws InvalidSPDXDocException
	 */
	public void setCreationInfo(SPDXCreatorInformation creator) throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must have an SPDX document to set creationInfo"));
		}
		// delete any previous created
		Property p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_CREATION_INFO);
		Resource s = getResource(spdxDocNode);
		model.removeAll(s, p, null);
		// add the property
		p = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_CREATION_INFO);
		s.addProperty(p, creator.createResource(model));
	}

	@Deprecated
	/**
	 * This method id deprecated - please use the getCreator() method for this information
	 * @return the creator comments for the analysis
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getCreatorComment() throws InvalidSPDXAnalysisException {
		SPDXCreatorInformation creator = this.getCreatorInfo();
		if (creator != null) {
			return creator.getComment();
		} else {
			return null;
		}
	}

	/**
	 * @return the reviewers
	 * @throws InvalidSPDXAnalysisException
	 */
	public SPDXReview[] getReviewers() throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must have an SPDX document to get reviewers"));
		}
		List<SPDXReview> als = Lists.newArrayList();
		als.clear();
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_REVIEWED_BY).asNode();
		Triple m = Triple.createMatch(spdxDocNode, p, null);
		ExtendedIterator<Triple >tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			als.add(new SPDXReview(model, t.getObject()));
		}
		SPDXReview[] reviewers = new SPDXReview[als.size()];
		reviewers = als.toArray(reviewers);
		return reviewers;
	}

	/**
	 * @param reviewers the reviewers to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setReviewers(SPDXReview[] reviewers) throws InvalidSPDXAnalysisException {
		if (reviewers.length > 0) {
			List<String> errors = Lists.newArrayList();
			for (int i = 0;i < reviewers.length; i++) {
				errors.addAll(reviewers[i].verify());
			}
			if (errors.size() > 0) {
				StringBuilder sb = new StringBuilder("Invalid reviewers due to the following errors in validation:\n");
				for (int i = 0; i < errors.size(); i++) {
					sb.append(errors.get(i));
					sb.append('\n');
				}
				throw(new InvalidSPDXAnalysisException(sb.toString()));
			}
		}
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must have an SPDX document to set reviewers"));
		}
		// delete any previous created
		Property p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_REVIEWED_BY);
		Resource s = getResource(spdxDocNode);
		model.removeAll(s, p, null);
		// add the property
		for (int i = 0; i < reviewers.length; i++) {
			p = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_REVIEWED_BY);
			s.addProperty(p, reviewers[i].createResource(model));
		}
	}

	@Deprecated
	/**
	 * This method id deprecated - please use the getCreator() method for this information
	 * @return the created
	 * @throws InvalidSPDXAnalysisException
	 */
	public String getCreated() throws InvalidSPDXAnalysisException {
		SPDXCreatorInformation creator = this.getCreatorInfo();
		if (creator != null) {
			return creator.getCreated();
		} else {
			return null;
		}
	}

	/**
	 * @return the spdxPackage
	 * @throws InvalidSPDXAnalysisException
	 */
	public SPDXPackage getSpdxPackage() throws InvalidSPDXAnalysisException {
		if (this.spdxPackage != null) {
			return this.spdxPackage;
		}
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must set an SPDX doc before getting an SPDX package"));
		}
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_PACKAGE).asNode();
		Triple m = Triple.createMatch(spdxDocNode, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		SPDXPackage newSpdxPackage = null;
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			newSpdxPackage = new SPDXPackage(t.getObject(), this);
		}
		this.spdxPackage = newSpdxPackage;
		return newSpdxPackage;
	}

	/**
	 * Creates an empty SPDX package
	 * @param uri Unique URI representing the SPDX package
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	public SPDXPackage createSpdxPackage(String uri) throws InvalidSPDXAnalysisException {
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must create the SPDX document before creating an SPDX Package"));
		}
		// delete the previous SPDX package
		Property p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_PACKAGE);
		Resource s = getResource(getSpdxDocNode());
		model.removeAll(s, p, null);
		p = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_PACKAGE);
		Resource pkgType = model.createResource(SPDX_NAMESPACE+CLASS_SPDX_PACKAGE);
		Resource spdxPkg = model.createResource(uri, pkgType);
		s.addProperty(p, spdxPkg);
		return this.spdxPackage = new SPDXPackage(spdxPkg.asNode(), this);
	}

	public void createSpdxPackage() throws InvalidSPDXAnalysisException {
		// generate a unique URI by appending the next available SPDX element ref to the document namespace
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must create the SPDX document before creating an SPDX Package"));
		}
		createSpdxPackage(this.documentNamespace + this.getNextSpdxElementRef());
	}

	/**
	 * @return the nonStandardLicenses
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExtractedLicenseInfo[] getExtractedLicenseInfos() throws InvalidSPDXAnalysisException {
		// nonStandardLicenses
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("No SPDX Document - can not get the Non Standard Licenses"));
		}
		List<ExtractedLicenseInfo> alLic = Lists.newArrayList();
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_EXTRACTED_LICENSES).asNode();
		Triple m = Triple.createMatch(spdxDocNode, p, null);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		while (tripleIter.hasNext()) {
			Triple t = tripleIter.next();
			alLic.add(new ExtractedLicenseInfo(this, t.getObject()));
		}
		ExtractedLicenseInfo[] nonStandardLicenses = new ExtractedLicenseInfo[alLic.size()];
		nonStandardLicenses = alLic.toArray(nonStandardLicenses);
		return nonStandardLicenses;
	}

	/**
	 * Resets all ExtractedLicenseInfos to nonStandardLicenses.  Removes references to any ExtractedLicenseInfos not present in nonStandardLicenses.
	 * If any LicenseID's are already present in the model, the text will be replaced with the text in the new nonStandardLicenses
	 * @param nonStandardLicenses the nonStandardLicenses to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setExtractedLicenseInfos(ExtractedLicenseInfo[] nonStandardLicenses) throws InvalidSPDXAnalysisException {
		List<String> errors = Lists.newArrayList();
		// verify the licenses
		for (int i = 0;i < nonStandardLicenses.length; i++) {
			errors.addAll(nonStandardLicenses[i].verify());
		}
		if (errors.size() > 0) {
			StringBuilder sb = new StringBuilder("Invalid extracted license information due to the following verification failures:\n");
			for (int i = 0; i < errors.size(); i++) {
				sb.append(errors.get(i));
				sb.append('\n');
			}
			throw new InvalidSPDXAnalysisException(sb.toString());
		}
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			throw(new InvalidSPDXAnalysisException("Must create the SPDX document before setting Non-Standard Licenses"));
		}
		// validate the license ID's and update the next license ID property
		initializeNextLicenseRef(nonStandardLicenses);
		// delete the previous createdby's
		Property p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_EXTRACTED_LICENSES);
		Resource s = getResource(getSpdxDocNode());
		model.removeAll(s, p, null);
		for (int i = 0; i < nonStandardLicenses.length; i++) {
			p = model.createProperty(SPDX_NAMESPACE, PROP_SPDX_EXTRACTED_LICENSES);
			s.addProperty(p, nonStandardLicenses[i].createResource(this));
		}
	}

	/**
	 * Adds a new non-standard license containing the text provided.  Forms the license ID
	 * from the next License ID available
	 * @param licenseText
	 * @return the newly created NonStandardLicense
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExtractedLicenseInfo addNewExtractedLicenseInfo(String licenseText) throws InvalidSPDXAnalysisException {
		String licenseID = getNextLicenseRef();
		ExtractedLicenseInfo retval = new ExtractedLicenseInfo(licenseID, licenseText);
		addNewExtractedLicenseInfo(retval);
		return retval;
	}

	/**
	 * Adds the license as a new ExtractedLicenseInfo
	 * @param license
	 * @throws InvalidSPDXAnalysisException
	 */
	public void addNewExtractedLicenseInfo(ExtractedLicenseInfo license) throws InvalidSPDXAnalysisException {
		if (extractedLicenseExists(license.getLicenseId())) {
			throw(new InvalidSPDXAnalysisException("Can not add license - ID "+license.getLicenseId()+" already exists."));
		}
		Property p = model.getProperty(SPDX_NAMESPACE, PROP_SPDX_EXTRACTED_LICENSES);
		Resource s = getResource(getSpdxDocNode());
		s.addProperty(p, license.createResource(this));
	}

	/**
	 * @param id
	 * @return true if the license ID is already in the model as an extracted license info
	 * @throws InvalidSPDXAnalysisException
	 */
	protected boolean extractedLicenseExists(String id) throws InvalidSPDXAnalysisException {
		Node p = model.getProperty(SPDX_NAMESPACE, PROP_LICENSE_ID).asNode();
		Node o = NodeFactory.createLiteral(id);
		Triple m = Triple.createMatch(null, p, o);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);
		return tripleIter.hasNext();
	}

	/**
	 * @return next available license ID for an ExtractedLicenseInfo
	 */
	public synchronized String getNextLicenseRef() {
		int nextLicNum = this.nextLicenseRef.getAndIncrement();
		return formNonStandardLicenseID(nextLicNum);
	}

	/**
	 * @return return the next available SPDX element reference.
	 */
	@Override
	public String getNextSpdxElementRef() {
		int nextSpdxElementNum = this.nextElementRef.getAndIncrement();
		return formSpdxElementRef(nextSpdxElementNum);
	}

	public static String formSpdxElementRef(int refNum) {
		return SPDX_ELEMENT_REF_PRENUM + String.valueOf(refNum);
	}

	/**
	 * @return the URI of the SPDX Document
	 */
	public String getSpdxDocUri() {
		// populate the model
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode == null) {
			return null;
		}
		return spdxDocNode.toString(false);
	}

	/**
	 * @return the model
	 */
	@Override
    public Model getModel() {
		return model;
	}

	/**
	 * @param model the model to set
	 * @throws InvalidSPDXAnalysisException
	 */
	public void setModel(Model model) throws InvalidSPDXAnalysisException {
		Model oldModel = this.model;
		this.model = model;
		try {
			List<String> errors = initialize();
			if (errors != null && errors.size() > 0) {
				this.model = oldModel;
				throw(new InvalidSPDXAnalysisException("New model contains verification errors"));
			}
		} catch (InvalidSPDXAnalysisException e) {
			this.model = oldModel;
			throw(e);
		}
	}
	/**
	 * Creates a new empty SPDX Document with the current SPDX document version.
	 * Note: Any previous SPDX documents will be deleted from the model to
	 * preserve the one and only one constraint.
	 * Note: Follow-up calls MUST be made to add the required properties for this
	 * to be a valid SPDX document
	 * @param uri URI for the SPDX Document
	 * @throws InvalidSPDXAnalysisException
	 */
	public void createSpdxAnalysis(String uri) throws InvalidSPDXAnalysisException {
		createSpdxAnalysis(uri, CURRENT_SPDX_VERSION);
	}

	/**
	 * Creates a new empty SPDX Document.
	 * Note: Any previous SPDX documents will be deleted from the model to
	 * preserve the one and only one constraint.
	 * Note: Follow-up calls MUST be made to add the required properties for this
	 * to be a valid SPDX document
	 * @param uri URI for the SPDX Document
	 * @param spdxVersion The version of SPDX analysis to create (impacts the data license for some versions)
	 * @throws InvalidSPDXAnalysisException
	 */
	public void createSpdxAnalysis(String uri, String spdxVersion) throws InvalidSPDXAnalysisException {
		String v = verifySpdxVersion(spdxVersion);
		if (v != null) {
			throw(new InvalidSPDXAnalysisException("Invalid SPDX Version: "+v));
		}
		Node spdxDocNode = getSpdxDocNode();
		if (spdxDocNode != null) {
			// delete
			model.removeAll();
		}
		model.setNsPrefix("spdx", SPDX_NAMESPACE);
		model.setNsPrefix("doap", DOAP_NAMESPACE);
		model.setNsPrefix("rdfs", RDFS_NAMESPACE);
		model.setNsPrefix("rdf", RDF_NAMESPACE);
		this.documentNamespace = formDocNamespace(uri);
		model.setNsPrefix("", this.documentNamespace);
		// set the default namespace to the document namespace
		Resource spdxAnalysisType = model.createResource(SPDX_NAMESPACE+CLASS_SPDX_DOCUMENT);
		model.createResource(uri, spdxAnalysisType);
		// add the version
		this.setSpdxVersion(spdxVersion);
		// reset the next license number and next spdx element num
		this.nextElementRef.set(1);
		this.nextLicenseRef.set(1);
		// add the default data license
		if (!spdxVersion.equals(POINT_EIGHT_SPDX_VERSION) && !spdxVersion.equals(POINT_NINE_SPDX_VERSION)) { // added as a mandatory field in 1.0
			try {
				SpdxListedLicense dataLicense;
				if (spdxVersion.equals(ONE_DOT_ZERO_SPDX_VERSION))
					{
					dataLicense = (SpdxListedLicense)(LicenseInfoFactory.parseSPDXLicenseString(SPDX_DATA_LICENSE_ID_VERSION_1_0));
				} else {
					dataLicense = (SpdxListedLicense)(LicenseInfoFactory.parseSPDXLicenseString(SPDX_DATA_LICENSE_ID));
				}
				this.setDataLicense(dataLicense);
			} catch (InvalidLicenseStringException e) {
				throw new InvalidSPDXAnalysisException("Unable to create data license", e);
			}
		}
	}

	/**
	 * Form the document namespace URI from the SPDX document URI
	 * @param docUriString String form of the SPDX document URI
	 * @return
	 */
	private String formDocNamespace(String docUriString) {
		// just remove any fragments for the DOC URI
		int fragmentIndex = docUriString.indexOf('#');
		if (fragmentIndex <= 0) {
			return docUriString + "#";
		} else {
			return docUriString.substring(0, fragmentIndex) + "#";
		}
	}

	/**
	 * @return the spdx doc node from the model
	 */
	private Node getSpdxDocNode() {
		Node spdxDocNode = null;
		Node rdfTypePredicate = this.model.getProperty(RDF_NAMESPACE, RDF_PROP_TYPE).asNode();
		Node spdxDocObject = this.model.getProperty(SPDX_NAMESPACE, CLASS_SPDX_DOCUMENT).asNode();
		Triple m = Triple.createMatch(null, rdfTypePredicate, spdxDocObject);
		ExtendedIterator<Triple> tripleIter = model.getGraph().find(m);	// find the document
		while (tripleIter.hasNext()) {
			Triple docTriple = tripleIter.next();
			spdxDocNode = docTriple.getSubject();
		}
		return spdxDocNode;
	}

	/**
	 * @return Document namespace
	 */
	@Override
    public String getDocumentNamespace() {
		return this.documentNamespace;
	}

	protected SPDXDocument getDocument() {
		return this;
	}

	public SPDXDocument getSpdxDocument() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#spdxElementRefExists(java.lang.String)
	 */
	@Override
	public boolean spdxElementRefExists(String elementRef) {
		// TODO Implement
		return false;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#addSpdxElementRef(java.lang.String)
	 */
	@Override
	public void addSpdxElementRef(String elementRef)
			throws InvalidSPDXAnalysisException {
		// TODO Implement

	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#documentNamespaceToId(java.lang.String)
	 */
	@Override
	public String documentNamespaceToId(String externalNamespace) {
		// SPDXDocument does not support external document references
		return null;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#externalDocumentIdToNamespace(java.lang.String)
	 */
	@Override
	public String externalDocumentIdToNamespace(String docId) {
		// SPDXDocument does not support external document references
		return null;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#createResource(org.apache.jena.rdf.model.Resource, java.lang.String, org.apache.jena.rdf.model.Resource, org.spdx.rdfparser.model.IRdfModel)
	 */
	@Override
	public Resource createResource(Resource duplicate, String uri,
			Resource type, IRdfModel modelObject) {
		if (duplicate != null) {
			return duplicate;
		} else if (uri == null) {
			return model.createResource(type);
		} else {
			return model.createResource(uri, type);
		}
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.IModelContainer#addCheckNodeObject(org.apache.jena.graph.Node, org.spdx.rdfparser.model.IRdfModel)
	 */
	@Override
	public boolean addCheckNodeObject(Node node, IRdfModel rdfModelObject) {
		// TODO Auto-generated method stub
		return false;
	}
}

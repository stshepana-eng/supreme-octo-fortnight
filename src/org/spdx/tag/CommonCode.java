/**

 * Copyright (c) 2010 Source Auditor Inc.

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
package org.spdx.tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.spdx.rdfparser.DOAPProject;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXReview;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.SimpleLicensingInfo;
import org.spdx.rdfparser.model.Annotation;
import org.spdx.rdfparser.model.Checksum;
import org.spdx.rdfparser.model.DoapProject;
import org.spdx.rdfparser.model.ExternalDocumentRef;
import org.spdx.rdfparser.model.ExternalRef;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxElement;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxFile.FileType;
import org.spdx.rdfparser.model.SpdxItem;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.rdfparser.model.SpdxSnippet;
import org.spdx.rdfparser.model.pointer.ByteOffsetPointer;
import org.spdx.rdfparser.model.pointer.LineCharPointer;
import org.spdx.rdfparser.model.pointer.StartEndPointer;
import org.spdx.rdfparser.referencetype.ListedReferenceTypes;
import org.spdx.tools.RdfToTag;

import com.google.common.collect.Lists;

/**
 * Define Common methods used by Tag-Value and SPDXViewer to print the SPDX
 * document.
 *
 * @author Rana Rahal, Protecode Inc.
 */
@SuppressWarnings("deprecation")
public class CommonCode {


	/**
	 * @param doc
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException
	 */
	public static void printDoc(SpdxDocument doc, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		if (doc == null) {
			println(out, "Warning: No document to print");
			return;
		}
		// version
		String spdxVersion = "";
		if (doc.getSpecVersion() != null
				&& doc.getCreationInfo().getCreated() != null) {
			spdxVersion = doc.getSpecVersion();
			println(out, constants.getProperty("PROP_SPDX_VERSION") + spdxVersion);
		}
		// Data license
		if (!spdxVersion.equals(SpdxDocumentContainer.POINT_EIGHT_SPDX_VERSION)
				&& !spdxVersion.equals(SpdxDocumentContainer.POINT_NINE_SPDX_VERSION)) {
			AnyLicenseInfo dataLicense = doc.getDataLicense();
			if (dataLicense != null) {
				if (dataLicense instanceof SimpleLicensingInfo) {
					println(out, constants.getProperty("PROP_SPDX_DATA_LICENSE")
							+ ((SimpleLicensingInfo)dataLicense).getLicenseId());
				} else {
					println(out, constants.getProperty("PROP_SPDX_DATA_LICENSE")
							+ dataLicense.toString());
				}
			}
		}
		// Document Uri
		String docNamespace = doc.getDocumentNamespace();
		if (docNamespace != null && !docNamespace.isEmpty()) {
			out.println(constants.getProperty("PROP_DOCUMENT_NAMESPACE") + docNamespace);
		}
		// element properties
		printElementProperties(doc, out, constants, "PROP_DOCUMENT_NAME", "PROP_SPDX_COMMENT");
		println(out, "");
		// External References
		ExternalDocumentRef[] externalRefs = doc.getExternalDocumentRefs();
		if (externalRefs != null && externalRefs.length > 0) {
			String externalDocRefHedr = constants.getProperty("EXTERNAL_DOC_REFS_HEADER");
			if (externalDocRefHedr != null && !externalDocRefHedr.isEmpty()) {
				println(out, externalDocRefHedr);
			}
			for (int i = 0; i < externalRefs.length; i++) {
				printExternalDocumentRef(externalRefs[i], out, constants);
			}
		}
		// Creators
		if (doc.getCreationInfo().getCreators() != null
				&& doc.getCreationInfo().getCreators().length > 0) {
			println(out, constants.getProperty("CREATION_INFO_HEADER"));
			String[] creators = doc.getCreationInfo().getCreators();
			for (int i = 0; i < creators.length; i++) {
				println(out, constants.getProperty("PROP_CREATION_CREATOR")
						+ creators[i]);
			}
		}
		// Creation Date
		if (doc.getCreationInfo().getCreated() != null
				&& !doc.getCreationInfo().getCreated().isEmpty()) {
			println(out, constants.getProperty("PROP_CREATION_CREATED")
					+ doc.getCreationInfo().getCreated());
		}
		// Creator Comment
		if (doc.getCreationInfo().getComment() != null
				&& !doc.getCreationInfo().getComment().isEmpty()) {
			println(out, constants.getProperty("PROP_CREATION_COMMENT")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ doc.getCreationInfo().getComment()
					+ constants.getProperty("PROP_END_TEXT"));
		}
		// License list version
		if (doc.getCreationInfo().getLicenseListVersion() != null &&
				!doc.getCreationInfo().getLicenseListVersion().isEmpty()) {
			println(out, constants.getProperty("PROP_LICENSE_LIST_VERSION") +
					doc.getCreationInfo().getLicenseListVersion());
		}
		printElementAnnotationsRelationships(doc, out, constants, "PROP_DOCUMENT_NAME", "PROP_SPDX_COMMENT");
		println(out, "");
		// Print the actual files
		List<SpdxPackage> allPackages = doc.getDocumentContainer().findAllPackages();
		List<SpdxFile> allFiles = doc.getDocumentContainer().findAllFiles();
		List<SpdxSnippet> allSnippets = doc.getDocumentContainer().findAllSnippets();
		// first print out any described files or snippets
		SpdxItem[] items = doc.getDocumentDescribes();
		if (items.length > 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] instanceof SpdxFile) {
					printFile((SpdxFile)items[i], out, constants);
					allFiles.remove(items[i]);
				} else if (items[i] instanceof SpdxSnippet) {
					printSnippet((SpdxSnippet)items[i], out, constants);
					allSnippets.remove(items[i]);
				}
			}
		}
		// print any described packages
		if (items.length > 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] instanceof SpdxPackage) {
					printPackage((SpdxPackage)items[i], out, constants, allFiles, doc.getDocumentNamespace());
					allPackages.remove(items[i]);
				}
			}
		}
		// print remaining packages
		for (SpdxPackage pkg:allPackages) {
			printPackage(pkg, out, constants, allFiles, doc.getDocumentNamespace());
		}
		// print remaining files
		for (SpdxFile file:allFiles) {
			printFile(file, out, constants);
		}
		// print remainig snippets snippets
		Collections.sort(allSnippets);
		for (SpdxSnippet snippet:allSnippets) {
			printSnippet(snippet, out, constants);
		}
		// Extracted license infos
		println(out, "");
		if (doc.getExtractedLicenseInfos() != null
				&& doc.getExtractedLicenseInfos().length > 0) {
			ExtractedLicenseInfo[] nonStandardLic = doc
					.getExtractedLicenseInfos();
			println(out, constants.getProperty("LICENSE_INFO_HEADER"));
			for (int i = 0; i < nonStandardLic.length; i++) {
				printLicense(nonStandardLic[i], out, constants);
			}
		}
		// Reviewers
		SPDXReview[] reviewedBy = doc.getReviewers();

		if (reviewedBy != null && reviewedBy.length > 0) {
			println(out, constants.getProperty("REVIEW_INFO_HEADER"));
			for (int i = 0; i < reviewedBy.length; i++) {
				println(out, constants.getProperty("PROP_REVIEW_REVIEWER")
						+ reviewedBy[i].getReviewer());
				println(out, constants.getProperty("PROP_REVIEW_DATE")
						+ reviewedBy[i].getReviewDate());
				if (reviewedBy[i].getComment() != null
						&& !reviewedBy[i].getComment().isEmpty()) {
					println(out, constants.getProperty("PROP_REVIEW_COMMENT")
							+ constants.getProperty("PROP_BEGIN_TEXT")
							+ reviewedBy[i].getComment()
							+ constants.getProperty("PROP_END_TEXT"));
				}
				println(out, "");
			}
		}
	}

	/**
	 * @param spdxSnippet
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException
	 */
	private static void printSnippet(SpdxSnippet spdxSnippet, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		println(out, constants.getProperty("SNIPPET_HEADER"));
		// NOTE: We can't call the print element properties since the order for tag/value is different for snippets
		println(out, constants.getProperty("PROP_SNIPPET_SPDX_ID") + spdxSnippet.getId());
		if (spdxSnippet.getSnippetFromFile() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_FROM_FILE_ID") +
					spdxSnippet.getSnippetFromFile().getId());
		}
		if (spdxSnippet.getByteRange() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_BYTE_RANGE") +
					formatPointerRange(spdxSnippet.getByteRange()));
		}
		if (spdxSnippet.getLineRange() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_LINE_RANGE") +
					formatPointerRange(spdxSnippet.getLineRange()));
		}
		if (spdxSnippet.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_CONCLUDED_LICENSE") +
					spdxSnippet.getLicenseConcluded());
		}
		if (spdxSnippet.getLicenseInfoFromFiles() != null) {
			for (AnyLicenseInfo seenLicense:spdxSnippet.getLicenseInfoFromFiles()) {
				println(out, constants.getProperty("PROP_SNIPPET_SEEN_LICENSE") +
						seenLicense);
			}
		}
		if (spdxSnippet.getLicenseComments() != null && !spdxSnippet.getLicenseComments().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_LIC_COMMENTS") +
					spdxSnippet.getLicenseComments());
		}
		if (spdxSnippet.getCopyrightText() != null && !spdxSnippet.getCopyrightText().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_COPYRIGHT") +
					spdxSnippet.getCopyrightText());
		}
		if (spdxSnippet.getComment() != null && !spdxSnippet.getComment().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_COMMENT") +
					spdxSnippet.getComment());
		}
		if (spdxSnippet.getName() != null && !spdxSnippet.getName().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_NAME") +
					spdxSnippet.getName());
		}
		println(out, "");
	}

	/**
	 * Format a start end pointer into a numeric range
	 * @param pointer
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	private static String formatPointerRange(StartEndPointer pointer) throws InvalidSPDXAnalysisException {
		String start = "[MISSING]";
		String end = "[MISSING]";
		if (pointer.getStartPointer() != null) {
			if (pointer.getStartPointer() instanceof ByteOffsetPointer) {
				start = String.valueOf(((ByteOffsetPointer)(pointer.getStartPointer())).getOffset());
			} else if (pointer.getStartPointer() instanceof LineCharPointer) {
				start = String.valueOf(((LineCharPointer)(pointer.getStartPointer())).getLineNumber());
			}
		}
		if (pointer.getEndPointer() != null) {
			if (pointer.getEndPointer() instanceof ByteOffsetPointer) {
				end = String.valueOf(((ByteOffsetPointer)(pointer.getEndPointer())).getOffset());
			} else if (pointer.getStartPointer() instanceof LineCharPointer) {
				end = String.valueOf(((LineCharPointer)(pointer.getEndPointer())).getLineNumber());
			}
		}
		return start + ":" + end;
	}

	/**
	 * @param externalDocumentRef
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException
	 */
	private static void printExternalDocumentRef(
			ExternalDocumentRef externalDocumentRef, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		String uri = externalDocumentRef.getSpdxDocumentNamespace();
		if (uri == null || uri.isEmpty()) {
			uri = "[UNSPECIFIED]";
		}
		String sha1 = "[UNSPECIFIED]";
		Checksum checksum = externalDocumentRef.getChecksum();
		if (checksum != null && checksum.getValue() != null && !checksum.getValue().isEmpty()) {
			sha1 = checksum.getValue();
		}
		String id = externalDocumentRef.getExternalDocumentId();
		if (id == null || id.isEmpty()) {
			id = "[UNSPECIFIED]";
		}
		println(out, constants.getProperty("PROP_EXTERNAL_DOC_URI") +
					id + " " + uri + " SHA1: " + sha1);
	}

	/**
	 * @param doc
	 * @param out
	 * @param constants
	 * @param string
	 */
	private static void printElementProperties(SpdxElement element,
			PrintWriter out, Properties constants, String nameProperty,
			String commentProperty) {
		if (element.getName() != null && !element.getName().isEmpty()) {
			println(out, constants.getProperty(nameProperty) + element.getName());
		}
		if (element.getId() != null && !element.getId().isEmpty()) {
			println(out, constants.getProperty("PROP_ELEMENT_ID") + element.getId());
		}
		if (element.getComment() != null && !element.getComment().isEmpty()) {
			println(out, constants.getProperty(commentProperty)
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ element.getComment()
					+ constants.getProperty("PROP_END_TEXT"));
		}
	}

	private static void printElementAnnotationsRelationships(SpdxElement element,
			PrintWriter out, Properties constants, String nameProperty,
			String commentProperty) {
		Annotation[] annotations = element.getAnnotations();
		if (annotations != null && annotations.length > 0) {
			println(out, constants.getProperty("ANNOTATION_HEADER"));
			for (int i = 0; i < annotations.length; i++) {
				printAnnotation(annotations[i], element.getId(), out, constants);
			}
		}
		Relationship[] relationships = element.getRelationships();
		if (relationships != null && relationships.length > 0) {
			println(out, constants.getProperty("RELATIONSHIP_HEADER"));
			for (int i = 0; i < relationships.length; i++) {
				printRelationship(relationships[i], element.getId(), out, constants);
			}
		}
	}
	/**
	 * @param relationship
	 * @param out
	 * @param constants
	 */
	private static void printRelationship(Relationship relationship,
			String elementId, PrintWriter out, Properties constants) {
		out.println(constants.getProperty("PROP_RELATIONSHIP")+
				elementId+" " +
				relationship.getRelationshipType().toTag()+
				" " + relationship.getRelatedSpdxElement().getId());
	}

	/**
	 * @param annotation
	 * @param out
	 * @param constants
	 */
	private static void printAnnotation(Annotation annotation, String id,
			PrintWriter out, Properties constants) {
		out.println(constants.getProperty("PROP_ANNOTATOR")+annotation.getAnnotator());
		out.println(constants.getProperty("PROP_ANNOTATION_DATE")+annotation.getAnnotationDate());
		out.println(constants.getProperty("PROP_ANNOTATION_COMMENT")
				+ constants.getProperty("PROP_BEGIN_TEXT")
				+ annotation.getComment()
				+ constants.getProperty("PROP_END_TEXT"));
		out.println(constants.getProperty("PROP_ANNOTATION_TYPE")+
				(annotation.getAnnotationType().getTag()));
		out.println(constants.getProperty("PROP_ANNOTATION_ID")+id);
	}

	/**
	 * @param license
	 */
	private static void printLicense(ExtractedLicenseInfo license,
			PrintWriter out, Properties constants) {
		// id
		if (license.getLicenseId() != null && !license.getLicenseId().isEmpty()) {
			println(out,
					constants.getProperty("PROP_LICENSE_ID") + license.getLicenseId());
		}
		if (license.getExtractedText() != null && !license.getExtractedText().isEmpty()) {
			println(out, constants.getProperty("PROP_EXTRACTED_TEXT")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ license.getExtractedText() + constants.getProperty("PROP_END_TEXT"));
		}
		if (license.getName() != null && !license.getName().isEmpty()) {
			println(out, constants.getProperty("PROP_LICENSE_NAME")+license.getName());
		}
		if (license.getSeeAlso() != null && license.getSeeAlso().length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(license.getSeeAlso()[0]);
			for (int i = 1; i < license.getSeeAlso().length; i++) {
				sb.append(", ");
				sb.append(license.getSeeAlso()[i]);
			}
			println(out, constants.getProperty("PROP_SOURCE_URLS")+sb.toString());
		}
		if (license.getSeeAlso() != null) {
            if (license.getComment() != null && !license.getComment().isEmpty()) {
            	println(out, constants.getProperty("PROP_LICENSE_COMMENT")
            			+ constants.getProperty("PROP_BEGIN_TEXT")
            			+ license.getComment()
            			+ constants.getProperty("PROP_END_TEXT"));
            }
        }
		println(out, "");
	}

	/**
	 * @param spdxPackage
	 * @throws InvalidSPDXAnalysisException
	 */
	private static void printPackage(SpdxPackage pkg, PrintWriter out,
			Properties constants, List<SpdxFile> remainingFilesToPrint,
			String documentNamespace) throws InvalidSPDXAnalysisException {
		println(out, constants.getProperty("PACKAGE_INFO_HEADER"));
		printElementProperties(pkg, out, constants,"PROP_PACKAGE_DECLARED_NAME",
				"PROP_PACKAGE_COMMENT");
		// Version
		if (pkg.getVersionInfo() != null && !pkg.getVersionInfo().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_VERSION_INFO")
							+ pkg.getVersionInfo());
		}
		// File name
		if (pkg.getPackageFileName() != null && !pkg.getPackageFileName().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_FILE_NAME")
							+ pkg.getPackageFileName());
		}
		// Supplier
		if (pkg.getSupplier() != null && !pkg.getSupplier().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_SUPPLIER")
							+ pkg.getSupplier());
		}
		// Originator
		if (pkg.getOriginator() != null && !pkg.getOriginator().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_ORIGINATOR")
							+ pkg.getOriginator());
		}
		// Download location
		if (pkg.getDownloadLocation() != null && !pkg.getDownloadLocation().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_DOWNLOAD_URL")
							+ pkg.getDownloadLocation());
		}
		// package verification code
        if (pkg.getPackageVerificationCode() != null
                && pkg.getPackageVerificationCode().getValue() != null
                && !pkg.getPackageVerificationCode().getValue().isEmpty()) {
          String code = constants.getProperty("PROP_PACKAGE_VERIFICATION_CODE") + pkg.getPackageVerificationCode().getValue();
          String[] excludedFiles = pkg.getPackageVerificationCode().getExcludedFileNames();
          if (excludedFiles.length != 0) {
              StringBuilder excludedFilesBuilder = new StringBuilder("(");

              for (String excludedFile : excludedFiles) {
                if(excludedFilesBuilder.length() > 0){
                    excludedFilesBuilder.append(", ");
                }

                excludedFilesBuilder.append(excludedFile);
              }

              excludedFilesBuilder.append(')');
              code += excludedFilesBuilder.toString();
          }
          println(out, code);
        }
		// Checksums
		Checksum[] checksums = pkg.getChecksums();
		if (checksums != null && checksums.length > 0) {
			for (int i = 0; i < checksums.length; i++) {
				printChecksum(checksums[i], out, constants, "PROP_PACKAGE_CHECKSUM");
			}
		}
		// Home page
		if (pkg.getHomepage() != null && !pkg.getHomepage().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_HOMEPAGE_URL") +
					pkg.getHomepage());
		}
		// Source info
		if (pkg.getSourceInfo() != null && !pkg.getSourceInfo().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_SOURCE_INFO")
							+ constants.getProperty("PROP_BEGIN_TEXT")
							+ pkg.getSourceInfo()
							+ constants.getProperty("PROP_END_TEXT"));
		}
		// concluded license
		if (pkg.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_PACKAGE_CONCLUDED_LICENSE")
					+ pkg.getLicenseConcluded());
		}
		// License information from files
		if (pkg.getLicenseInfoFromFiles() != null
				&& pkg.getLicenseInfoFromFiles().length > 0) {
			AnyLicenseInfo[] licenses = pkg.getLicenseInfoFromFiles();
			println(out, constants.getProperty("LICENSE_FROM_FILES_INFO_HEADER"));
			for (int i = 0; i < licenses.length; i++) {
				println(out,
						constants
								.getProperty("PROP_PACKAGE_LICENSE_INFO_FROM_FILES")
								+ licenses[i].toString());
			}
		}
		// Declared licenses
		if (pkg.getLicenseDeclared() != null) {
			println(out, constants.getProperty("PROP_PACKAGE_DECLARED_LICENSE")
					+ pkg.getLicenseDeclared());
		}
		if (pkg.getLicenseComments() != null
				&& !pkg.getLicenseComments().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_LICENSE_COMMENT")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ pkg.getLicenseComments() +
					constants.getProperty("PROP_END_TEXT"));
		}
		// Declared copyright
		if (pkg.getCopyrightText() != null
				&& !pkg.getCopyrightText().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_DECLARED_COPYRIGHT")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ pkg.getCopyrightText() + constants.getProperty("PROP_END_TEXT"));
		}
		// Short description
		if (pkg.getSummary() != null
				&& !pkg.getSummary().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_SHORT_DESC")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ pkg.getSummary() + constants.getProperty("PROP_END_TEXT"));
		}
		// Description
		if (pkg.getDescription() != null && !pkg.getDescription().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_DESCRIPTION")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ pkg.getDescription() + constants.getProperty("PROP_END_TEXT"));
		}
		// Attribution text
		String[] attributionText = pkg.getAttributionText();
		if (attributionText != null) {
			for (String att:attributionText) {
				println(out, constants.getProperty("PROP_PACKAGE_ATTRIBUTION_TEXT")
						+ constants.getProperty("PROP_BEGIN_TEXT")
						+ att + constants.getProperty("PROP_END_TEXT"));
			}
		}
		// External Refs
		ExternalRef[] externalRefs = pkg.getExternalRefs();
		if (externalRefs != null && externalRefs.length > 0) {
			for (ExternalRef externalRef:externalRefs) {
				printExternalRef(out, constants, externalRef, documentNamespace);
			}
		}
		printElementAnnotationsRelationships(pkg, out, constants,"PROP_PACKAGE_DECLARED_NAME",
				"PROP_PACKAGE_COMMENT");
		// Files
		if (!pkg.isFilesAnalyzed()) {
			// Only print if not the default
			println(out, constants.getProperty("PROP_PACKAGE_FILES_ANALYZED") + "false");
		}
		if (pkg.getFiles() != null && pkg.getFiles().length > 0) {
                    /* Add files to a List */
                    List<SpdxFile> sortedFileList = Lists.newArrayList();
                    /* Sort the SPDX files before printout */
                    sortedFileList = Arrays.asList(pkg.getFiles());
                    Collections.sort(sortedFileList);
                    println(out, "");
			println(out, constants.getProperty("FILE_INFO_HEADER"));
                        /* Print out sorted files */
			for (SpdxFile file : sortedFileList) {
				printFile(file, out, constants);
				remainingFilesToPrint.remove(file);
				println(out, "");
			}
		} else {
			println(out, "");
		}
	}

	/**
	 * Print a package ExternalRef to out
	 * @param out
	 * @param constants
	 * @param externalRef
	 * @param docNamespace
	 * @throws InvalidSPDXAnalysisException
	 */
	private static void printExternalRef(PrintWriter out, Properties constants,
			ExternalRef externalRef, String docNamespace) throws InvalidSPDXAnalysisException {
		String category = null;
		if (externalRef.getReferenceCategory() == null) {
			category = "OTHER";
		} else {
			category = externalRef.getReferenceCategory().getTag();
		}
		String referenceType = null;
		if (externalRef.getReferenceType() == null ||
				externalRef.getReferenceType().getReferenceTypeUri() == null) {
			referenceType = "[MISSING]";
		} else {
			try {
				referenceType = ListedReferenceTypes.getListedReferenceTypes().getListedReferenceName(externalRef.getReferenceType().getReferenceTypeUri());
			} catch (InvalidSPDXAnalysisException e) {
				referenceType = null;
			}
			if (referenceType == null) {
				referenceType = externalRef.getReferenceType().getReferenceTypeUri().toString();
				if (referenceType.startsWith(docNamespace + "#")) {
					referenceType = referenceType.substring(docNamespace.length()+1);
				}
			}
		}
		String referenceLocator = externalRef.getReferenceLocator();
		if (referenceLocator == null) {
			referenceLocator = "[MISSING]";
		}
		println(out, constants.getProperty("PROP_EXTERNAL_REFERENCE") +
				category + " " + referenceType + " " + referenceLocator);
		if (externalRef.getComment() != null) {
			println(out, constants.getProperty("PROP_EXTERNAL_REFERENCE_COMMENT") + externalRef.getComment());
		}
	}

	/**
	 * @param checksum
	 * @param out
	 * @param constants
	 * @param checksumProperty
	 */
	private static void printChecksum(Checksum checksum, PrintWriter out,
			Properties constants, String checksumProperty) {
		out.println(constants.getProperty(checksumProperty)
				+ Checksum.CHECKSUM_ALGORITHM_TO_TAG.get(checksum.getAlgorithm())
				+ " " + checksum.getValue());
	}

	/**
	 * @param file
	 */
	private static void printFile(SpdxFile file, PrintWriter out,
			Properties constants) {
		printElementProperties(file, out, constants, "PROP_FILE_NAME",
				"PROP_FILE_COMMENT");
		// type
		FileType[] fileTypes = file.getFileTypes();
		if (fileTypes != null && fileTypes.length > 0) {
			for (int i = 0 ;i < fileTypes.length; i++) {
				println(out, constants.getProperty("PROP_FILE_TYPE") + SpdxFile.FILE_TYPE_TO_TAG.get(fileTypes[i]));
			}
		}
		Checksum[] checksums = file.getChecksums();
		if (checksums != null) {
			for (int i = 0; i < checksums.length; i++) {
				printChecksum(checksums[i], out, constants, "PROP_FILE_CHECKSUM");
			}
		}
		// concluded license
		if (file.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_FILE_LICENSE")
					+ file.getLicenseConcluded().toString());
		}
		// License info in file
		if (file.getLicenseInfoFromFiles() != null && file.getLicenseInfoFromFiles().length > 0) {
			// print(out, "\tLicense information from file: ");
			// print(out, file.getSeenLicenses()[0].toString());
			for (int i = 0; i < file.getLicenseInfoFromFiles().length; i++) {
				println(out, constants.getProperty("PROP_FILE_SEEN_LICENSE")
						+ file.getLicenseInfoFromFiles()[i].toString());
			}
		}
		// license comments
		if (file.getLicenseComments() != null
				&& !file.getLicenseComments().isEmpty()) {
			println(out,
					constants.getProperty("PROP_FILE_LIC_COMMENTS")
							+ file.getLicenseComments());
		}
		// file copyright
		if (file.getCopyrightText() != null && !file.getCopyrightText().isEmpty()) {
			println(out, constants.getProperty("PROP_FILE_COPYRIGHT")
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ file.getCopyrightText() + constants.getProperty("PROP_END_TEXT"));
		}
		// artifact of
		if (file.getArtifactOf() != null && file.getArtifactOf().length > 0) {
			for (int i = 0; i < file.getArtifactOf().length; i++) {
				printProject(file.getArtifactOf()[i], out, constants);
			}
		}
		// File notice
		if (file.getNoticeText() != null && !file.getNoticeText().isEmpty()) {
			println(out, constants.getProperty("PROP_FILE_NOTICE_TEXT") +
					constants.getProperty("PROP_BEGIN_TEXT") +
					file.getNoticeText() +
					constants.getProperty("PROP_END_TEXT"));
		}
		// file attribution text
		String[] attributionText = file.getAttributionText();
		if (attributionText != null) {
			for (String att:attributionText) {
				println(out, constants.getProperty("PROP_FILE_ATTRIBUTION_TEXT")
						+ constants.getProperty("PROP_BEGIN_TEXT")
						+ att + constants.getProperty("PROP_END_TEXT"));
			}
		}
		// file contributors
		if (file.getFileContributors() != null && file.getFileContributors().length > 0) {
			for (int i = 0; i < file.getFileContributors().length; i++) {
				println(out, constants.getProperty("PROP_FILE_CONTRIBUTOR")+
						file.getFileContributors()[i]);
			}
		}
		// file dependencies
		SpdxFile[] fileDependencies = file.getFileDependencies();
		if (fileDependencies != null && fileDependencies.length > 0) {
			for (SpdxFile fileDepdency : fileDependencies) {
				println(out, constants.getProperty("PROP_FILE_DEPENDENCY") + fileDepdency.getName());
			}
		}
		printElementAnnotationsRelationships(file, out, constants, "PROP_FILE_NAME",
				"PROP_FILE_COMMENT");
	}

	/**
	 * @param doapProject
	 */
	private static void printProject(DoapProject doapProject, PrintWriter out,
			Properties constants) {
		// project name
		if (doapProject.getName() != null && !doapProject.getName().isEmpty()) {
			println(out,
					constants.getProperty("PROP_PROJECT_NAME")
							+ doapProject.getName());
		}
		// project homepage
		if (doapProject.getHomePage() != null
				&& !doapProject.getHomePage().isEmpty()) {
			println(out, constants.getProperty("PROP_PROJECT_HOMEPAGE")
					+ doapProject.getHomePage());
		}
		// DOAP file url
		if (doapProject.getProjectUri() != null
				&& !doapProject.getProjectUri().isEmpty()
				&& !doapProject.getProjectUri().equals(DOAPProject.UNKNOWN_URI)) {
			println(out,
					constants.getProperty("PROP_PROJECT_URI")
							+ doapProject.getProjectUri());
		}
	}

	private static void println(PrintWriter out, String output) {
		if (out != null) {
			out.println(output);
		} else {
			System.out.println(output);
		}
	}

	public static Properties getTextFromProperties(final String path)
			throws IOException {
		InputStream is = null;
		Properties prop = new Properties();
		try {
			is = RdfToTag.class.getClassLoader().getResourceAsStream(path);
			prop.load(is);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Throwable e) {
//				logger.warn("Unable to close properties file.");
			}
		}
		return prop;
	}

}
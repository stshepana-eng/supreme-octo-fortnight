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
package org.spdx.tag;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXCreatorInformation;
import org.spdx.rdfparser.SPDXReview;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.model.Annotation;
import org.spdx.rdfparser.model.Annotation.AnnotationType;
import org.spdx.rdfparser.model.Checksum;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;
import org.spdx.rdfparser.model.DoapProject;
import org.spdx.rdfparser.model.ExternalDocumentRef;
import org.spdx.rdfparser.model.ExternalRef;
import org.spdx.rdfparser.model.ExternalRef.ReferenceCategory;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxElement;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxFile.FileType;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.rdfparser.model.SpdxSnippet;
import org.spdx.rdfparser.model.pointer.ByteOffsetPointer;
import org.spdx.rdfparser.model.pointer.LineCharPointer;
import org.spdx.rdfparser.model.pointer.StartEndPointer;
import org.spdx.rdfparser.referencetype.ListedReferenceTypes;
import org.spdx.rdfparser.referencetype.ReferenceType;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Translates an tag-value file to a an SPDX Document.
 *
 * Supports SPDX version 2.0
 *
 * 2.0 changes made by Gary O'Neall
 *
 * @author Rana Rahal, Protecode Inc.
 */
public class BuildDocument implements TagValueBehavior {

	private static class AnnotationWithId {
		private Annotation annotation;
		private String id;
		private AnnotationWithId(String annotator) {
			this.annotation = new Annotation(annotator, null, null, null);
		}
		@SuppressWarnings("unused")
		public void setAnnotator(String annotator) {
			annotation.setAnnotator(annotator);
		}
		public void setDate(String date) throws InvalidSPDXAnalysisException {
			annotation.setAnnotationDate(date);
		}
		public void setAnnotationType(AnnotationType annotationType) throws InvalidSPDXAnalysisException {
			annotation.setAnnotationType(annotationType);
		}
		public void setComment(String comment) {
			annotation.setComment(comment);
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getId() {
			return this.id;
		}
		public Annotation getAnnotation() {
			return this.annotation;
		}
	}
	private static class RelationshipWithId {
		private String id;
		private String relatedId;
		private RelationshipType relationshipType;
		private String comment;
		public RelationshipWithId(String id, String relatedId,
				RelationshipType relationshipType) {
			this.id = id;
			this.relatedId = relatedId;
			this.relationshipType = relationshipType;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
		public String getId() {
			return id;
		}
		public String getRelatedId() {
			return relatedId;
		}
		public RelationshipType getRelationshipType() {
			return relationshipType;
		}
		public String getComment() {
			return comment;
		}
	}
	
	private static Pattern EXTERNAL_DOC_REF_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+SHA1:\\s+(\\S+)");
	private static Pattern RELATIONSHIP_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");
	private static Pattern CHECKSUM_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)");
	private static Pattern NUMBER_RANGE_PATTERN = Pattern.compile("(\\d+):(\\d+)");
	private static Pattern EXTERNAL_REF_PATTERN = Pattern.compile("([^ ]+) ([^ ]+) (.+)");
	
	/**
	 * Tags used in the definition of an annotation
	 */
	private Set<String> ANNOTATION_TAGS = Sets.newHashSet();
	/**
	 * Tags used in the definition of a file
	 */
	private Set<String> FILE_TAGS = Sets.newHashSet();
	/**
	 * Tags used in the definition of a Snippet
	 */
	private Set<String> SNIPPET_TAGS = Sets.newHashSet();
	/**
	 * Tags used in the definition of a package
	 */
	/**
	 * Tags used in the definition of an extracted license
	 */
	private Set<String> EXTRACTED_LICENSE_TAGS = Sets.newHashSet();
	private Set<String> PACKAGE_TAGS = Sets.newHashSet();
	private Properties constants;
	private SpdxDocument analysis;
	private DateFormat format = new SimpleDateFormat(SpdxRdfConstants.SPDX_DATE_FORMAT);

	//When we retrieve a list from the SpdxDocument the order changes, therefore keep track of
	//the last object that we are looking at so that we can fill in all of it's information
	private SPDXReview lastReviewer = null;
	private ExtractedLicenseInfo lastExtractedLicense = null;
	private SpdxFile lastFile = null;
	private SpdxSnippet lastSnippet = null;
	private DoapProject lastProject = null;
	// Keep track of all file dependencies since these need to be added after all of the files
	// have been parsed.  Map of file dependency file name to the SPDX files which depends on it
	private Map<String, List<SpdxFile>> fileDependencyMap = Maps.newHashMap();
	/**
	 * Map of all snippetFileID's collected during parsing so that we can add the files
	 * at the end of the document creation once the files are actually created
	 */
	private Map<String, List<SpdxSnippet>>  snippetDependencyMap = Maps.newHashMap();
	/**
	 * Keep track of the last relationship for any following relationship related tags
	 */
	private RelationshipWithId lastRelationship = null;
	/**
	 * Keep track of all relationships and add them at the end of the parsing
	 */
	private List<RelationshipWithId> relationships = Lists.newArrayList();
	/**
	 * Keep track of the last annotation for any following annotation related tags
	 */
	private AnnotationWithId lastAnnotation;
	/**
	 * Keep track of all annotations and add them at the end of the parsing
	 */
	private List<AnnotationWithId> annotations = Lists.newArrayList();
	private SpdxDocumentContainer[] result = null;

	private String specVersion;

	private AnyLicenseInfo dataLicense;

	private String documentName;

	List<String> warningMessages;

	/**
	 * True if we have started defining a package in the tag/value file
	 */
	private boolean inPackageDefinition = false;;

	/**
	 * True if we have started to define a file AT THE DOCUMENT LEVEL
	 * in the tag/value file.  Note that files defined as part of a package
	 * will have the state flag inPackageDefinition set, and inFileDefinition will be false.
	 */
	private boolean inFileDefinition = false;
	/**
	 * true if we have started to define a Snippet
	 */
	private boolean inSnippetDefinition;
	/**
	 * True if we have started to define an annotation
	 * in the tag/value file.
	 */
	private boolean inAnnotation = false;;
	/**
	 * True if we are building an extracted license definition
	 */
	private boolean inExtractedLicenseDefinition = false;

	/**
	 * The last (or current) package being defined by the tag/value file
	 */
	private SpdxPackage lastPackage = null;

	/**
	 * The last external reference found
	 */
	private ExternalRef lastExternalRef = null;

	public BuildDocument(SpdxDocumentContainer[] result, Properties constants, List<String> warnings) {
		this.constants = constants;
		this.warningMessages = warnings;
		this.result = result;
		this.ANNOTATION_TAGS.add(constants.getProperty("PROP_ANNOTATION_DATE").trim()+" ");
		this.ANNOTATION_TAGS.add(constants.getProperty("PROP_ANNOTATION_COMMENT").trim()+" ");
		this.ANNOTATION_TAGS.add(constants.getProperty("PROP_ANNOTATION_ID").trim()+" ");
		this.ANNOTATION_TAGS.add(constants.getProperty("PROP_ANNOTATION_TYPE").trim()+" ");

		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_TYPE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_LICENSE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_COPYRIGHT").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_CHECKSUM").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_SEEN_LICENSE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_LIC_COMMENTS").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_COMMENT").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_DEPENDENCY").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_CONTRIBUTOR").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_FILE_NOTICE_TEXT").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ELEMENT_ID").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_PROJECT_NAME").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_PROJECT_HOMEPAGE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_PROJECT_URI").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_DOCUMENT_NAMESPACE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_RELATIONSHIP").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_RELATIONSHIP_COMMENT").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ANNOTATOR").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ANNOTATION_DATE").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ANNOTATION_COMMENT").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ANNOTATION_ID").trim()+" ");
		this.FILE_TAGS.add(constants.getProperty("PROP_ANNOTATION_TYPE").trim()+" ");
		
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_SPDX_ID").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_FROM_FILE_ID").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_BYTE_RANGE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_LINE_RANGE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_CONCLUDED_LICENSE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_LIC_COMMENTS").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_COPYRIGHT").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_COMMENT").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_NAME").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_SNIPPET_SEEN_LICENSE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_DOCUMENT_NAMESPACE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_RELATIONSHIP").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_RELATIONSHIP_COMMENT").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_ANNOTATOR").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_ANNOTATION_DATE").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_ANNOTATION_COMMENT").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_ANNOTATION_ID").trim()+" ");
		this.SNIPPET_TAGS.add(constants.getProperty("PROP_ANNOTATION_TYPE").trim()+" ");

		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_COMMENT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_FILE_NAME").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_CHECKSUM").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_DOWNLOAD_URL").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_SOURCE_INFO").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_DECLARED_LICENSE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_CONCLUDED_LICENSE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_DECLARED_COPYRIGHT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_SHORT_DESC").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_DESCRIPTION").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_VERIFICATION_CODE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_LICENSE_INFO_FROM_FILES").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_LICENSE_COMMENT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_VERSION_INFO").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_ORIGINATOR").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_SUPPLIER").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_HOMEPAGE_URL").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ELEMENT_ID").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_FILE_NAME").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_RELATIONSHIP").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_RELATIONSHIP_COMMENT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ANNOTATOR").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ANNOTATION_DATE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ANNOTATION_COMMENT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ANNOTATION_ID").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_ANNOTATION_TYPE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_EXTERNAL_REFERENCE").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_EXTERNAL_REFERENCE_COMMENT").trim()+" ");
		this.PACKAGE_TAGS.add(constants.getProperty("PROP_PACKAGE_FILES_ANALYZED").trim()+" ");

		this.EXTRACTED_LICENSE_TAGS.add(constants.getProperty("PROP_LICENSE_TEXT").trim()+" ");
		this.EXTRACTED_LICENSE_TAGS.add(constants.getProperty("PROP_EXTRACTED_TEXT").trim()+" ");
		this.EXTRACTED_LICENSE_TAGS.add(constants.getProperty("PROP_LICENSE_COMMENT").trim()+" ");
		this.EXTRACTED_LICENSE_TAGS.add(constants.getProperty("PROP_LICENSE_NAME").trim()+" ");
		this.EXTRACTED_LICENSE_TAGS.add(constants.getProperty("PROP_SOURCE_URLS").trim()+" ");
	}

	@Override
	public void enter() throws Exception {
		// do nothing???
	}

	@Override
	public void buildDocument(String tag, String value) throws Exception {
		tag = tag.trim()+" ";
		value = trim(value.trim());
		if (this.inAnnotation && ANNOTATION_TAGS.contains(tag)) {
			buildAnnotation(tag, value, lastAnnotation);
		} else if (this.inFileDefinition && FILE_TAGS.contains(tag)) {
			buildFile(this.lastFile, tag, value);
		} else if (this.inSnippetDefinition && SNIPPET_TAGS.contains(tag)) {
			buildSnippet(this.lastSnippet, tag, value);
		} else if (this.inPackageDefinition && PACKAGE_TAGS.contains(tag)) {
			buildPackage(this.lastPackage, tag, value);
		} else if (this.inExtractedLicenseDefinition && EXTRACTED_LICENSE_TAGS.contains(tag)) {
			buildExtractedLicense(this.lastExtractedLicense, tag, value);
		} else {
			if (inFileDefinition) {
				addLastFile();
			}
			if (inSnippetDefinition) {
				addLastSnippet();
			}
			this.inAnnotation = false;
			this.inFileDefinition = false;
			this.inPackageDefinition = false;
			inSnippetDefinition = false;
			buildDocumentProperties(tag, value);
		}
	}

	/**
	 * Add tag value properties to an existing snippet
	 * @param snippet
	 * @param tag
	 * @param value
	 * @throws InvalidSpdxTagFileException 
	 * @throws InvalidSPDXAnalysisException 
	 * @throws InvalidLicenseStringException 
	 */
	private void buildSnippet(SpdxSnippet snippet, String tag, String value) throws InvalidSpdxTagFileException, InvalidSPDXAnalysisException, InvalidLicenseStringException {
		if (snippet == null) {
			throw(new InvalidSpdxTagFileException("Missing Snippet ID - An SPDX Snippet ID must be specified before the snippet properties"));
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_SPDX_ID"))) {
			snippet.setId(value);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_FROM_FILE_ID"))) {
			// Since the files have not all been parsed, we just keep track of the
			// dependencies in a hashmap until we finish all processing and are building the package
			List<SpdxSnippet> snippetsWithThisAsADependency = this.snippetDependencyMap.get(value);
			if (snippetsWithThisAsADependency == null) {
				snippetsWithThisAsADependency = Lists.newArrayList();
				this.snippetDependencyMap.put(value, snippetsWithThisAsADependency);
			}
			snippetsWithThisAsADependency.add(snippet);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_BYTE_RANGE"))) {
			snippet.setByteRange(parseByteRange(value));
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_LINE_RANGE"))) {
			snippet.setLineRange(parseLineRange(value));
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_CONCLUDED_LICENSE"))) {
			snippet.setLicenseConcluded(LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer()));
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_LIC_COMMENTS"))) {
			snippet.setLicenseComments(value);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_COPYRIGHT"))) {
			snippet.setCopyrightText(value);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_COMMENT"))) {
			snippet.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_NAME"))) {
			snippet.setName(value);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_SEEN_LICENSE"))) {
			snippet.setLicenseInfosFromFiles(new AnyLicenseInfo[] {LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer())});
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATOR"))) {
			if (lastAnnotation != null) {
				annotations.add(lastAnnotation);
			}
			this.inAnnotation = true;
			lastAnnotation = new AnnotationWithId(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP"))) {
			if (lastRelationship != null) {
				relationships.add(lastRelationship);
			}
			lastRelationship = parseRelationship(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP_COMMENT"))) {
			if (lastRelationship == null) {
				throw(new InvalidSpdxTagFileException("Relationship comment found outside of a relationship: "+value));
			}
			lastRelationship.setComment(value);
		} else {
			throw new InvalidSPDXAnalysisException("Error parsing snippet.  Unrecognized tag: "+tag);
		}	
	}

	/**
	 * @param value
	 * @return
	 * @throws InvalidSpdxTagFileException 
	 */
	private StartEndPointer parseLineRange(String value) throws InvalidSpdxTagFileException {
		Matcher matcher = NUMBER_RANGE_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			throw(new InvalidSpdxTagFileException("Invalid snippet line range: "+value));
		}	
		LineCharPointer start = null;
		try {
			start = new LineCharPointer(null, Integer.parseInt(matcher.group(1)));
		} catch (Exception ex) {
			throw new InvalidSpdxTagFileException("Non integer start to snippet line offset: "+value);
		}
		LineCharPointer end = null;
		try {
			end = new LineCharPointer(null, Integer.parseInt(matcher.group(2)));
		} catch (Exception ex) {
			throw new InvalidSpdxTagFileException("Non integer end to snippet line offset: "+value);
		}
		return new StartEndPointer(start, end);
	}

	/**
	 * @param value
	 * @return
	 * @throws InvalidSpdxTagFileException 
	 */
	private StartEndPointer parseByteRange(String value) throws InvalidSpdxTagFileException {
		Matcher matcher = NUMBER_RANGE_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			throw(new InvalidSpdxTagFileException("Invalid snippet byte range: "+value));
		}	
		ByteOffsetPointer start = null;
		try {
			start = new ByteOffsetPointer(null, Integer.parseInt(matcher.group(1)));
		} catch (Exception ex) {
			throw new InvalidSpdxTagFileException("Non integer start to snippet byte offset: "+value);
		}
		ByteOffsetPointer end = null;
		try {
			end = new ByteOffsetPointer(null, Integer.parseInt(matcher.group(2)));
		} catch (Exception ex) {
			throw new InvalidSpdxTagFileException("Non integer end to snippet byte offset: "+value);
		}
		return new StartEndPointer(start, end);
	}

	/**
	 * @param license
	 * @param tag
	 * @param value
	 * @throws InvalidSpdxTagFileException
	 */
	private void buildExtractedLicense(
			ExtractedLicenseInfo license, String tag, String value) throws InvalidSpdxTagFileException {
		if (tag.equals(constants.getProperty("PROP_EXTRACTED_TEXT"))) {
			if (lastExtractedLicense == null) {
				throw(new InvalidSpdxTagFileException("Missing Extracted License - An  extracted license ID must be provided before the license text"));
			}
			license.setExtractedText(value);
		} else if (tag.equals(constants.getProperty("PROP_LICENSE_NAME"))) {
			if (lastExtractedLicense == null) {
				throw(new InvalidSpdxTagFileException("Missing Extracted License - An  extracted license ID must be provided before the license name"));
			}
			license.setName(value);
		} else if (tag.equals(constants.getProperty("PROP_SOURCE_URLS"))) {
			if (lastExtractedLicense == null) {
				throw(new InvalidSpdxTagFileException("Missing Extracted License - An  extracted license ID must be provided before the license URL"));
			}
			String[] values = value.split(",");
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			license.setSeeAlso(values);
		} else if (tag.equals(constants.getProperty("PROP_LICENSE_COMMENT"))) {
			if (lastExtractedLicense == null) {
				throw(new InvalidSpdxTagFileException("Missing Extracted License - An  extracted license ID must be provided before the license comment"));
			}
			license.setComment(value);
		}
	}

	@SuppressWarnings("deprecation")
	private void buildDocumentProperties(String tag, String value) throws Exception {
		if (tag.equals(constants.getProperty("PROP_SPDX_VERSION"))) {
			this.specVersion = value;
			if (analysis != null) {
				analysis.setSpecVersion(value);
			}
		} else if (tag.equals(constants.getProperty("PROP_SPDX_DATA_LICENSE"))) {
			try {
				this.dataLicense = LicenseInfoFactory.getListedLicenseById(value);
			} catch(InvalidSPDXAnalysisException ex) {
				this.dataLicense = null;
			}
			if (this.dataLicense == null) {
				this.dataLicense = new ExtractedLicenseInfo(value, "NO TEXT FOR "+value);
			}
			if (analysis != null) {
				analysis.setDataLicense(this.dataLicense);
			}
		} else if (tag.equals(constants.getProperty("PROP_DOCUMENT_NAME"))) {
			this.documentName = value;
			if (analysis != null) {
				this.analysis.setName(value);
			}
		} else if (tag.equals(constants.getProperty("PROP_DOCUMENT_NAMESPACE"))) {
			if (this.analysis != null) {
				throw(new InvalidSpdxTagFileException("More than one document namespace was specified"));
			}
			if (this.specVersion == null) {
				result[0] = new SpdxDocumentContainer(value);
			} else {
				result[0] = new SpdxDocumentContainer(value, this.specVersion);
			}
			this.analysis = result[0].getSpdxDocument();
			if (this.dataLicense != null) {
				this.analysis.setDataLicense(this.dataLicense);
			}
			if (this.documentName != null) {
				this.analysis.setName(this.documentName);
			}
		} else if (tag.equals(constants.getProperty("PROP_ELEMENT_ID"))) {
			if (!value.equals(SpdxRdfConstants.SPDX_DOCUMENT_ID)) {
				throw(new InvalidSpdxTagFileException("SPDX Document "+value
						+" is invalid.  Document IDs must be "+SpdxRdfConstants.SPDX_DOCUMENT_ID));
			}
		} else if (tag.equals(constants.getProperty("PROP_EXTERNAL_DOC_URI"))) {
			checkAnalysisNull();
			addExternalDocRef(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP"))) {
			if (lastRelationship != null) {
				relationships.add(lastRelationship);
			}
			lastRelationship = parseRelationship(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP_COMMENT"))) {
			if (lastRelationship == null) {
				throw(new InvalidSpdxTagFileException("Relationship comment found outside of a relationship: "+value));
			}
			lastRelationship.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATOR"))) {
			if (lastAnnotation != null) {
				annotations.add(lastAnnotation);
			}
			this.inAnnotation = true;
			lastAnnotation = new AnnotationWithId(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_DATE"))) {
			throw(new InvalidSpdxTagFileException("Annotation date found outside of an annotation: "+value));
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_COMMENT"))) {
			throw(new InvalidSpdxTagFileException("Annotation comment found outside of an annotation: "+value));
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_ID"))) {
			throw(new InvalidSpdxTagFileException("Annotation ID found outside of an annotation: "+value));
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_TYPE"))) {
			throw(new InvalidSpdxTagFileException("Annotation type found outside of an annotation: "+value));
		} else if (tag.equals(constants.getProperty("PROP_CREATION_CREATOR"))) {
			checkAnalysisNull();
			if (analysis.getCreationInfo() == null) {
				SPDXCreatorInformation creator = new SPDXCreatorInformation(new String[] { value }, "", "", "");
				analysis.setCreationInfo(creator);
			} else {
				List<String> creators = Lists.newArrayList(analysis.getCreationInfo().getCreators());
				creators.add(value);
				analysis.getCreationInfo().setCreators(creators.toArray(new String[0]));
			}
		} else if (tag.equals(constants.getProperty("PROP_CREATION_CREATED"))) {
			checkAnalysisNull();
			if (analysis.getCreationInfo() == null) {
				SPDXCreatorInformation creator = new SPDXCreatorInformation(new String[] {  }, "", "", "");
				analysis.setCreationInfo(creator);
			}
			analysis.getCreationInfo().setCreated(value);
		} else if (tag.equals(constants.getProperty("PROP_CREATION_COMMENT"))) {
			checkAnalysisNull();
			if (analysis.getCreationInfo() == null) {
				SPDXCreatorInformation creator = new SPDXCreatorInformation(new String[] { value }, "", "", "");
				analysis.setCreationInfo(creator);
			}
			analysis.getCreationInfo().setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_LICENSE_LIST_VERSION"))) {
			checkAnalysisNull();
			if (analysis.getCreationInfo() == null) {
				SPDXCreatorInformation creator = new SPDXCreatorInformation(new String[] { value }, "", "", "");
				analysis.setCreationInfo(creator);
			}
			analysis.getCreationInfo().setLicenseListVersion(value);
		} else if (tag.equals(constants.getProperty("PROP_SPDX_COMMENT"))) {
			checkAnalysisNull();
			analysis.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_REVIEW_REVIEWER"))) {
			checkAnalysisNull();
			lastReviewer = new SPDXReview(value, format.format(new Date()), ""); // update date later
			List<SPDXReview> reviewers = Lists.newArrayList(analysis.getReviewers());
			reviewers.add(lastReviewer);
			analysis.setReviewers(reviewers.toArray(new SPDXReview[0]));
		} else if (tag.equals(constants.getProperty("PROP_REVIEW_DATE"))) {
			checkAnalysisNull();
			if (lastReviewer == null) {
				throw(new InvalidSpdxTagFileException("Missing Reviewer - A reviewer must be provided before a review date"));
			}
			lastReviewer.setReviewDate(value);
		} else if (tag.equals(constants.getProperty("PROP_REVIEW_COMMENT"))) {
			checkAnalysisNull();
			if (lastReviewer == null) {
				throw(new InvalidSpdxTagFileException("Missing Reviewer - A reviewer must be provided before a review comment"));
			}
			lastReviewer.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_LICENSE_ID"))) {
			checkAnalysisNull();
			if (analysis.getDocumentContainer().extractedLicenseExists(value)) {
				lastExtractedLicense = analysis.getDocumentContainer().getExtractedLicense(value);
			} else {
				lastExtractedLicense = new ExtractedLicenseInfo(value, "WARNING: TEXT IS REQUIRED", null, null, null); //change text later
				analysis.addExtractedLicenseInfos(lastExtractedLicense);
			}
			this.inExtractedLicenseDefinition = true;
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_DECLARED_NAME"))) {
			checkAnalysisNull();
			inPackageDefinition = true;
			inFileDefinition = false;
			inAnnotation = false;
			inSnippetDefinition = false;
			if (this.lastPackage != null) {
				if (this.lastPackage.getId() == null) {
					this.warningMessages.add("Missing SPDX ID for "+this.lastPackage.getName()
							+ ".  An SPDX ID will be generated for this package.");
				}
				this.analysis.getDocumentContainer().addElement(this.lastPackage);
			}
			this.lastPackage = new SpdxPackage(value, null, null, null, null, null, null, null);
		} else if (tag.equals(constants.getProperty("PROP_FILE_NAME"))) {
			checkAnalysisNull();
			//NOTE: This must follow the inPackageDefinition check since
			// if a file is defined following a package, it is assumed to
			// be part of the package and not something standalone
			addLastFile();
			inFileDefinition = true;
			inPackageDefinition = false;
			inAnnotation = false;
			inSnippetDefinition = false;

			this.lastFile = new SpdxFile(value, null, new Annotation[0], new Relationship[0], null,
					new AnyLicenseInfo[0], null, null, new FileType[0], new Checksum[0],
					new String[0] , null, new DoapProject[0]);
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_SPDX_ID"))) {
			checkAnalysisNull();
			addLastSnippet();
			inSnippetDefinition = true;
			inFileDefinition = false;
			inPackageDefinition = false;
			inAnnotation = false;
			this.lastSnippet = new SpdxSnippet(null, null, new Annotation[0], new Relationship[0], null, 
					new AnyLicenseInfo[0], null, null, null, null, null);
			this.lastSnippet.setId(value);
		} else {
			throw new InvalidSpdxTagFileException("Expecting a definition of a file, package, license information, or document property at "+tag+value);
		}
	}


	/**
	 * Adds the last file to either the last package or the document
	 * @throws InvalidSPDXAnalysisException
	 *
	 */
	private void addLastFile() throws InvalidSPDXAnalysisException {
		if (this.lastFile != null) {
			if (this.lastFile.getId() == null) {
				this.warningMessages.add("Missing SPDX ID for "+this.lastFile.getName()
						+ ".  An SPDX ID will be generated for this file.");
			}
			if (lastPackage != null) {
				this.lastPackage.addFile(lastFile);
			} else {
				this.analysis.getDocumentContainer().addElement(lastFile);
			}
		}
		this.lastFile = null;
	}
	
	/**
	 * Adds the last snippet to the document
	 * @throws InvalidSPDXAnalysisException
	 *
	 */
	private void addLastSnippet() throws InvalidSPDXAnalysisException {
		if (this.lastSnippet != null) {
			if (this.lastSnippet.getId() == null) {
				String snippetName = this.lastSnippet.getName();
				if (snippetName == null) {
					snippetName = "[UNKNOWN]";
				}
				this.warningMessages.add("Missing SPDX ID for " + lastSnippet
						+ ".  An SPDX ID will be generated for this file.");
			}
			this.analysis.getDocumentContainer().addElement(lastSnippet);
		}
		this.lastSnippet = null;
	}

	/**
	 * @param tag
	 * @param value
	 * @param annotation
	 * @throws InvalidSPDXAnalysisException
	 */
	private void buildAnnotation(String tag, String value,
			AnnotationWithId annotation) throws InvalidSPDXAnalysisException {
		if (tag.equals(constants.getProperty("PROP_ANNOTATION_DATE"))) {
			annotation.setDate(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_COMMENT"))) {
			annotation.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_ID"))) {
			annotation.setId(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATION_TYPE"))) {
			AnnotationType annotationType = AnnotationType.fromTag(value);
			if (annotationType == null) {
				throw(new InvalidSPDXAnalysisException("Invalid annotation type: "+value));
			}
			annotation.setAnnotationType(annotationType);
		}
	}

	/**
	 * @param value
	 * @return
	 * @throws InvalidSpdxTagFileException
	 */
	private RelationshipWithId parseRelationship(String value) throws InvalidSpdxTagFileException {
		Matcher matcher = RELATIONSHIP_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			throw(new InvalidSpdxTagFileException("Invalid relationship: "+value));
		}
		RelationshipType relationshipType = RelationshipType.fromTag(matcher.group(2));
		if (relationshipType == null) {
			throw(new InvalidSpdxTagFileException("Invalid relationship type: "+value));
		}
		return new RelationshipWithId(matcher.group(1), matcher.group(3),
				relationshipType);
	}

	private void checkAnalysisNull() throws InvalidSpdxTagFileException, InvalidSPDXAnalysisException {
		if (this.analysis == null) {
			if (this.specVersion != null && this.specVersion.compareTo("SPDX-2.0") < 0) {
				result[0] = new SpdxDocumentContainer(generateDocumentNamespace());
				this.analysis = result[0].getSpdxDocument();
			} else {
				throw(new InvalidSpdxTagFileException("The SPDX Document Namespace must be set before other SPDX document properties are set."));
			}
		}
	}

	/**
	 * @return
	 */
	private String generateDocumentNamespace() {
		return "http://spdx.org/documents/"+UUID.randomUUID().toString();
	}

	/**
	 * @param value
	 * @throws InvalidSpdxTagFileException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void addExternalDocRef(String value) throws InvalidSpdxTagFileException, InvalidSPDXAnalysisException {
		ExternalDocumentRef ref = parseExternalDocumentRef(value);
		ExternalDocumentRef[] oldRefs = this.analysis.getExternalDocumentRefs();
		if (oldRefs == null) {
			oldRefs = new ExternalDocumentRef[0];
		}
		ExternalDocumentRef[] newRefs = Arrays.copyOf(oldRefs, oldRefs.length+1);
		newRefs[oldRefs.length] = ref;
		this.analysis.setExternalDocumentRefs(newRefs);
	}

	/**
	 * Parse a tag/value exteranl document reference string
	 * @param refStr
	 * @return
	 * @throws InvalidSpdxTagFileException
	 */
	public static ExternalDocumentRef parseExternalDocumentRef(String refStr) throws InvalidSpdxTagFileException {
		Matcher matcher = EXTERNAL_DOC_REF_PATTERN.matcher(refStr.trim());
		if (!matcher.find()) {
			throw(new InvalidSpdxTagFileException("Invalid external document reference: "+refStr));
		}
		ExternalDocumentRef ref = new ExternalDocumentRef(matcher.group(2),
				new Checksum(ChecksumAlgorithm.checksumAlgorithm_sha1, matcher.group(3)),
				matcher.group(1));
		return ref;
	}

	/**
	 * @param spdxPackage
	 * @throws InvalidSPDXAnalysisException
	 */
	private void buildPackage(SpdxPackage pkg, String tag, String value)
			throws Exception {
		if (tag.equals(constants.getProperty("PROP_ELEMENT_ID"))) {
			pkg.setId(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_VERSION_INFO"))) {
			pkg.setVersionInfo(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_FILE_NAME"))) {
			pkg.setPackageFileName(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_SUPPLIER"))) {
			pkg.setSupplier(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_ORIGINATOR"))) {
			pkg.setOriginator(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_DOWNLOAD_URL"))) {
			pkg.setDownloadLocation(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_VERIFICATION_CODE"))) {
			if (value.contains("(")) {
				String[] verification = value.split("\\(");
				String[] excludedFiles = verification[1].replace(")", "").split(",");
				for (int i = 0; i < excludedFiles.length; i++) {
					excludedFiles[i] = excludedFiles[i].trim();
				}
				pkg.setPackageVerificationCode(new SpdxPackageVerificationCode(verification[0].trim(), excludedFiles));
			}
			else {
				pkg.setPackageVerificationCode(new SpdxPackageVerificationCode(value, new String[0]));
			}
		} else if (constants.getProperty("PROP_PACKAGE_CHECKSUM").startsWith(tag)) {
			pkg.addChecksum(parseChecksum(value));
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_HOMEPAGE_URL"))) {
			pkg.setHomepage(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_SOURCE_INFO"))) {
			pkg.setSourceInfo(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_CONCLUDED_LICENSE"))) {
			AnyLicenseInfo licenseSet = LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer());
			pkg.setLicenseConcluded(licenseSet);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_LICENSE_INFO_FROM_FILES"))) {
			AnyLicenseInfo license = LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer());
			List<AnyLicenseInfo> licenses = Lists.newArrayList(pkg.getLicenseInfoFromFiles());
			licenses.add(license);
			pkg.setLicenseInfosFromFiles(licenses.toArray(new AnyLicenseInfo[0]));
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_DECLARED_LICENSE"))) {
			AnyLicenseInfo licenseSet = LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer());
			pkg.setLicenseDeclared(licenseSet);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_LICENSE_COMMENT"))) {
			pkg.setLicenseComments(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_DECLARED_COPYRIGHT"))) {
			pkg.setCopyrightText(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_SHORT_DESC"))) {
			pkg.setSummary(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_DESCRIPTION"))) {
			pkg.setDescription(value);
		} else if (tag.equals(constants.getProperty("PROP_EXTERNAL_REFERENCE"))) {
			this.lastExternalRef = parseExternalRef(value);
			pkg.addExternalRef(this.lastExternalRef);
		} else if (tag.equals(constants.getProperty("PROP_EXTERNAL_REFERENCE_COMMENT"))) {
			if (this.lastExternalRef == null) {
				throw new InvalidSpdxTagFileException("External reference comment found without an external reference: "+value);
			}
			if (this.lastExternalRef.getComment() != null && !this.lastExternalRef.getComment().isEmpty()) {
				throw new InvalidSpdxTagFileException("Second reference comment found for the same external reference: "+value);
			}
			this.lastExternalRef.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATOR"))) {
			if (lastAnnotation != null) {
				annotations.add(lastAnnotation);
			}
			this.inAnnotation = true;
			lastAnnotation = new AnnotationWithId(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP"))) {
			if (lastRelationship != null) {
				relationships.add(lastRelationship);
			}
			lastRelationship = parseRelationship(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP_COMMENT"))) {
			if (lastRelationship == null) {
				throw(new InvalidSpdxTagFileException("Relationship comment found outside of a relationship: "+value));
			}
			lastRelationship.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_NAME"))) {
			addLastFile();
			this.lastFile = new SpdxFile(value, null, new Annotation[0], new Relationship[0], null,
					new AnyLicenseInfo[0], null, null, new FileType[0], new Checksum[0],
					new String[0] , null, new DoapProject[0]);
			this.inFileDefinition = true;
			inSnippetDefinition = false;
			inAnnotation = false;
		} else if (tag.equals(constants.getProperty("PROP_SNIPPET_SPDX_ID"))) {
			addLastSnippet();
			inSnippetDefinition = true;
			inFileDefinition = false;
			inPackageDefinition = false;
			inAnnotation = false;
			this.lastSnippet = new SpdxSnippet(null, null, new Annotation[0], new Relationship[0], null, 
					new AnyLicenseInfo[0], null, null, null, null, null);
			this.lastSnippet.setId(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_COMMENT"))) {
			pkg.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_PACKAGE_FILES_ANALYZED"))) {
			if ("TRUE".equals(value.toUpperCase())) {
				pkg.setFilesAnalyzed(true);
			} else if ("FALSE".equals(value.toUpperCase())) {
				pkg.setFilesAnalyzed(false);
			} else {
				throw(new InvalidSpdxTagFileException("Invalid value for files analyzed.  Must be 'true' or 'false'.  Found value: "+value));
			}
//		} else if (this.inFileDefinition) {
//			buildFile(this.lastFile, tag, value);
			//TODO: Delete the above by end of 2017 if no error have been found - not quite sure why that code was there
		} else {
			throw(new InvalidSpdxTagFileException("Expecting a file definition, snippet definition or a package property.  Found "+value));
		}
	}

	/**
	 * Parse the external reference string
	 * @param value
	 * @return
	 * @throws InvalidSpdxTagFileException 
	 * @throws InvalidSPDXAnalysisException 
	 */
	private ExternalRef parseExternalRef(String value) throws InvalidSpdxTagFileException, InvalidSPDXAnalysisException {
		Matcher matcher = EXTERNAL_REF_PATTERN.matcher(value);
		if (!matcher.find()) {
			throw new InvalidSpdxTagFileException("Invalid External Ref format: "+value);
		}
		ReferenceCategory referenceCategory = ReferenceCategory.fromTag(matcher.group(1).trim());
		if (referenceCategory == null) {
			throw new InvalidSpdxTagFileException("Invalid External Ref category: "+value);
		}
		ReferenceType referenceType = null;
		String tagType = matcher.group(2).trim();
		try {
			// First, try to find a listed type
			referenceType = ListedReferenceTypes.getListedReferenceTypes().getListedReferenceTypeByName(tagType);
		} catch (InvalidSPDXAnalysisException e) {
			referenceType = null;
		}
		if (referenceType == null) {
			try {
				URI refTypeUri = null;
				if (tagType.contains("/") || tagType.contains(":")) {
					// Assume a full URI
					refTypeUri = new URI(tagType);
				} else {
					// User the document namespace
					refTypeUri = new URI(this.result[0].getDocumentNamespace() + matcher.group(2).trim());
				}
				referenceType = new ReferenceType(refTypeUri, null, null, null);
			} catch (URISyntaxException e) {
				throw new InvalidSpdxTagFileException("Invalid External Ref type: "+value);
			}
		}
		return new ExternalRef(referenceCategory, referenceType, matcher.group(3), null);
	}

	/**
	 * Creates a Checksum from the parameters specified in the tag value
	 * @param value
	 * @return
	 * @throws InvalidSpdxTagFileException
	 */
	public static Checksum parseChecksum(String value) throws InvalidSpdxTagFileException {
		Matcher matcher = CHECKSUM_PATTERN.matcher(value.trim());
		if (!matcher.find()) {
			throw(new InvalidSpdxTagFileException("Invalid checksum: "+value));
		}
		ChecksumAlgorithm algorithm = Checksum.CHECKSUM_TAG_TO_ALGORITHM.get(matcher.group(1));
		if (algorithm == null) {
			throw(new InvalidSpdxTagFileException("Invalid checksum algorithm: "+value));
		}
		return new Checksum(algorithm, matcher.group(2));
	}

	/**
	 * @param file
	 */
	private void buildFile(SpdxFile file, String tag, String value)
			throws Exception {
		if (file == null) {
			if (FILE_TAGS.contains(tag)) {
				throw(new InvalidSpdxTagFileException("Missing File Name - A file name must be specified before the file properties"));
			} else {
				throw(new InvalidSpdxTagFileException("Unrecognized SPDX Tag: "+tag));
			}
		}
		if (tag.equals(constants.getProperty("PROP_ELEMENT_ID"))) {
			file.setId(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_TYPE"))) {
			FileType fileType = FileType.fromTag(value.trim());
			if (fileType == null) {
				fileType = FileType.fromTag(value.trim().toUpperCase());
				if (fileType == null) {
					throw(new InvalidSpdxTagFileException("Unknown file type: "+value));
				} else {
					this.warningMessages.add("Invalid filetype - needs to be uppercased: "+value);
				}
			}
			file.addFileType(fileType);
		} else if (constants.getProperty("PROP_FILE_CHECKSUM").startsWith(tag)) {
			file.addChecksum(parseChecksum(value));
		} else if (tag.equals(constants.getProperty("PROP_FILE_LICENSE"))) {
			AnyLicenseInfo licenseSet = LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer());
			file.setLicenseConcluded(licenseSet);
		} else if (tag.equals(constants.getProperty("PROP_FILE_SEEN_LICENSE"))) {
			AnyLicenseInfo fileLicense = (LicenseInfoFactory.parseSPDXLicenseString(value, this.analysis.getDocumentContainer()));
			List<AnyLicenseInfo> seenLicenses = Lists.newArrayList(file.getLicenseInfoFromFiles());
			seenLicenses.add(fileLicense);
			file.setLicenseInfosFromFiles(seenLicenses.toArray(new AnyLicenseInfo[0]));
		} else if (tag.equals(constants.getProperty("PROP_FILE_LIC_COMMENTS"))) {
			file.setLicenseComments(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_COPYRIGHT"))) {
			file.setCopyrightText(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_COMMENT"))) {
			file.setComment(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_NOTICE_TEXT"))) {
			file.setNoticeText(value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_CONTRIBUTOR"))) {
			addFileContributor(file, value);
		} else if (tag.equals(constants.getProperty("PROP_FILE_DEPENDENCY"))) {
			addFileDependency(file, value);
		} else if (tag.equals(constants.getProperty("PROP_ANNOTATOR"))) {
			if (lastAnnotation != null) {
				annotations.add(lastAnnotation);
			}
			this.inAnnotation = true;
			lastAnnotation = new AnnotationWithId(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP"))) {
			if (lastRelationship != null) {
				relationships.add(lastRelationship);
			}
			lastRelationship = parseRelationship(value);
		} else if (tag.equals(constants.getProperty("PROP_RELATIONSHIP_COMMENT"))) {
			if (lastRelationship == null) {
				throw(new InvalidSpdxTagFileException("Relationship comment found outside of a relationship: "+value));
			}
			lastRelationship.setComment(value);
		} else {
			buildProject(file, tag, value);
		}
	}

	/**
	 * Adds a file contributor to the list of contributors for this file
	 * @param file
	 * @param contributor
	 */
	private void addFileContributor(SpdxFile file, String contributor) {
		String[] contributors = file.getFileContributors();
		if (contributors == null) {
			contributors = new String[] {contributor};

		} else {
			contributors = Arrays.copyOf(contributors, contributors.length + 1);
			contributors[contributors.length-1] = contributor;
		}
		file.setFileContributors(contributors);
	}

	/**
	 * Adds a file dependency to a file
	 * @param file
	 * @param dependentFileName
	 */
	private void addFileDependency(SpdxFile file, String dependentFileName) {
		// Since the files have not all been parsed, we just keep track of the
		// dependencies in a hashmap until we finish all processing and are building the package
		List<SpdxFile> filesWithThisAsADependency = this.fileDependencyMap.get(dependentFileName);
		if (filesWithThisAsADependency == null) {
			filesWithThisAsADependency = Lists.newArrayList();
			this.fileDependencyMap.put(dependentFileName, filesWithThisAsADependency);
		}
		filesWithThisAsADependency.add(file);
	}

	/**
	 * @param doapProject
	 */
	@SuppressWarnings("deprecation")
	private void buildProject(SpdxFile file, String tag, String value)
			throws Exception {
		if (tag.equals(constants.getProperty("PROP_PROJECT_NAME"))) {
			lastProject = new DoapProject(value, null);
			List<DoapProject> projects = Lists.newArrayList(file.getArtifactOf());
			projects.add(lastProject);
			file.setArtifactOf(projects.toArray(new DoapProject[0]));
		} else {
			if (tag.equals(constants.getProperty("PROP_PROJECT_HOMEPAGE"))) {
				if (lastProject == null) {
					throw(new InvalidSpdxTagFileException("Missing Project Name - A project name must be provided before the project properties"));
				}
				lastProject.setHomePage(value);
			} else if (tag.equals(constants.getProperty("PROP_PROJECT_URI"))) {
				if (lastProject == null) {
					throw(new InvalidSpdxTagFileException("Missing Project Name - A project name must be provided before the project properties"));
				}
				// can not set the URI since it is already created, we need to replace DOAP project
				DoapProject[] existingProjects = file.getArtifactOf();
				int i = 0;
				while (i < existingProjects.length && !existingProjects[i].equals(lastProject)) {
					i++;
				}
				if (i >= existingProjects.length) {
					existingProjects = Arrays.copyOf(existingProjects, existingProjects.length+1);
				}
				existingProjects[i] = new DoapProject(lastProject.getName(), lastProject.getHomePage());
				existingProjects[i].setProjectUri(value);
				file.setArtifactOf(existingProjects);
				lastProject = existingProjects[i];
			} else {
				throw(new InvalidSpdxTagFileException("Unrecognized tag: "+tag));
			}
		}
	}

	private static String trim(String value) {
		value = value.replaceAll("\u00A0", " ");
		value = value.trim();
		value = value.replaceAll("<text>", "").replaceAll("</text>", "");
		return value;
	}

	@Override
	public void exit() throws Exception {
		addLastFile();
		if (this.lastPackage != null) {
			this.analysis.getDocumentContainer().addElement(this.lastPackage);
		}
		fixFileAndSnippetDependencies();
		addRelationships();
		checkSinglePackageDefault();
		addAnnotations();
		warningMessages.addAll(analysis.verify());
//        assertEquals("SpdxDocument", 0, warningMessages);
		// Moved the responsibility for printing warnings to the caller
		// The warnings is now passed in as a parameter to the constructor
		// leaving the commented out code in place until we have tested the change
		// in the interface - can be removed after Sept. 2015
	}

	/**
	 * Makes sure there is a describes relationships for a single package
	 * SPDX document
	 * @throws InvalidSPDXAnalysisException
	 */
	private void checkSinglePackageDefault() throws InvalidSPDXAnalysisException {
		List<SpdxPackage> pkgs = this.analysis.getDocumentContainer().findAllPackages();
		Relationship[] documentRelationships = this.analysis.getRelationships();
		List<String> describedElementIds = Lists.newArrayList();
		for (int i = 0; i < documentRelationships.length; i++) {
			if (documentRelationships[i].getRelationshipType() == Relationship.RelationshipType.DESCRIBES) {
				if (pkgs.contains(documentRelationships[i])) {
					describedElementIds.add(documentRelationships[i].getRelatedSpdxElement().getId());
				}
			}
		}
		if (describedElementIds.size() == 0 && pkgs.size() == 0) {
			// add a relationship for the package as a default
			// See the spec for the definition of this default behavior
			Relationship describesRelationship = new Relationship(pkgs.get(0),
					Relationship.RelationshipType.DESCRIBES,
					"This describes relationship was added as a default relationship by the SPDX Tools Tag parser.");
			this.analysis.addRelationship(describesRelationship);
		}
	}

	/**
	 * @throws InvalidSPDXAnalysisException
	 *
	 */
	private void addAnnotations() throws InvalidSPDXAnalysisException {
		if (this.lastAnnotation != null) {
			this.annotations.add(lastAnnotation);
			lastAnnotation = null;
		}
		for (int i = 0; i < annotations.size(); i++) {
			String id = annotations.get(i).getId();
			if (id == null) {
				this.warningMessages.add("missing SPDXREF: tag in annotation " + annotations.get(i).getAnnotation().getComment());
				continue;
			}
			SpdxElement element = this.analysis.getDocumentContainer().findElementById(id);
			if (element == null) {
				this.warningMessages.add("Invalid element reference in annotation: " + id);
				continue;
			}
			Annotation[] elementAnnotations = element.getAnnotations();
			if (elementAnnotations == null) {
				elementAnnotations = new Annotation[0];
			}
			Annotation[] newAnnotations = Arrays.copyOf(elementAnnotations, elementAnnotations.length+1);
			newAnnotations[elementAnnotations.length] = annotations.get(i).getAnnotation();
			element.setAnnotations(newAnnotations);
		}
	}

	/**
	 * @throws InvalidSPDXAnalysisException
	 *
	 */
	private void addRelationships() throws InvalidSPDXAnalysisException {
		if (this.lastRelationship != null) {
			this.relationships.add(lastRelationship);
			lastRelationship = null;
		}
		for (int i = 0; i < relationships.size(); i++) {
			RelationshipWithId relationship = relationships.get(i);
			SpdxElement element = this.analysis.getDocumentContainer().findElementById(relationship.getId());
			if (element == null) {
				this.warningMessages.add("Missing element for a relationship.  SPDX ID: "+relationship.getId());
				continue;
			}
			SpdxElement relatedElement = this.analysis.getDocumentContainer().findElementById(relationship.getRelatedId());
			//Invoked for processing
			element.getRelationships();
			element.addRelationship(new Relationship(relatedElement,
					relationships.get(i).getRelationshipType(), relationships.get(i).getComment()));
		}
	}
	
	/**
	 * Go through all of the file dependencies and snippet dependencies and add them to the file
	 * @throws InvalidSPDXAnalysisException
	 * @throws InvalidSpdxTagFileException 
	 */
	@SuppressWarnings("deprecation")
	private void fixFileAndSnippetDependencies() throws InvalidSPDXAnalysisException, InvalidSpdxTagFileException {
		// be prepared - it is complicate to make this efficient
		// the HashMap fileDependencyMap contains a map from a file name to all SPDX files which
		// reference that file name as a dependency
		// This method goes through all of the files in the analysis in a single pass and creates
		// a new HashMap of files (as the key) and the dependency files (arraylist) as the values.
		// We also take care of updating the files in the Snippet Dependencies in this pass.
		// Once that hashmap is built, the actual dependencies and snippets are then updated.
		// the key contains an SPDX file with one or more dependencies.  The value is the array list of file dependencies
		Map<SpdxFile, List<SpdxFile>> filesWithDependencies = Maps.newHashMap();
		Map<SpdxFile, List<SpdxSnippet>> filesWithSnippets = Maps.newHashMap();
		this.checkAnalysisNull();
		SpdxFile[] allFiles = analysis.getDocumentContainer().getFileReferences();
		// fill in the filesWithDependencies map
		for (int i = 0;i < allFiles.length; i++) {
			List<SpdxFile> alFilesHavingThisDependency = this.fileDependencyMap.get(allFiles[i].getName());
			if (alFilesHavingThisDependency != null) {
				for (int j = 0; j < alFilesHavingThisDependency.size(); j++) {
					SpdxFile fileWithDependency = alFilesHavingThisDependency.get(j);
					List<SpdxFile> alDepdenciesForThisFile = filesWithDependencies.get(fileWithDependency);
					if (alDepdenciesForThisFile == null) {
						alDepdenciesForThisFile = Lists.newArrayList();
						filesWithDependencies.put(fileWithDependency, alDepdenciesForThisFile);
					}
					alDepdenciesForThisFile.add(allFiles[i]);
				}
				// remove from the file dependency map so we can keep track of any files which did
				// not match at the end
				this.fileDependencyMap.remove(allFiles[i].getName());
			}
			List<SpdxSnippet> alSnippetsWithThisFile = this.snippetDependencyMap.get(allFiles[i].getId());
			if (alSnippetsWithThisFile != null) {
				List<SpdxSnippet> snippets = Lists.newArrayList();
				filesWithSnippets.put(allFiles[i], snippets);
				for (SpdxSnippet snippet:alSnippetsWithThisFile) {
					snippets.add(snippet);
				}
			}
			this.snippetDependencyMap.remove(allFiles[i].getId());
		}
		// Go through the file dependency hashmap we just created and add the dependent files
		Iterator<Entry<SpdxFile, List<SpdxFile>>> iter = filesWithDependencies.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<SpdxFile, List<SpdxFile>> entry = iter.next();
			List<SpdxFile> alDependencies = entry.getValue();
			if (alDependencies != null && alDependencies.size() > 0) {
				entry.getKey().setFileDependencies(alDependencies.toArray(new SpdxFile[alDependencies.size()]));
			}
		}
		// Now go through the snippets map
		Iterator<Entry<SpdxFile, List<SpdxSnippet>>> snIter = filesWithSnippets.entrySet().iterator();
		while (snIter.hasNext()) {
			Entry<SpdxFile, List<SpdxSnippet>> entry = snIter.next();
			List<SpdxSnippet> alSnippets = entry.getValue();
			if (alSnippets != null) {
				for (SpdxSnippet snippet:alSnippets) {
					snippet.setSnippetFromFile(entry.getKey());
				}
			}
		}
		// Check to see if there are any left over and and throw an error if the dependent files were
		// not found
		Set<String> missingDependencies = this.fileDependencyMap.keySet();
		if (missingDependencies != null && missingDependencies.size() > 0) {
			this.warningMessages.add("The following file names were listed as file dependencies but were not found in the list of files:");
			Iterator<String> missingIter = missingDependencies.iterator();
			while(missingIter.hasNext()) {
				this.warningMessages.add("\t"+missingIter.next());
			}
		}
		Set<String> missingSnippetFileIds = this.snippetDependencyMap.keySet();
		if (missingSnippetFileIds != null && missingSnippetFileIds.size() > 0) {
			this.warningMessages.add("The following file IDs were listed as files for snippets but were not found in the list of files:");
			Iterator<String> missingIter = missingDependencies.iterator();
			while(missingIter.hasNext()) {
				this.warningMessages.add("\t"+missingIter.next());
			}
		}
	}
}

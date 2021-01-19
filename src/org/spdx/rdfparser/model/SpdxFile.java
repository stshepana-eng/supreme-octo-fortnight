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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.RdfModelHelper;
import org.spdx.rdfparser.RdfParserHelper;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A File represents a named sequence of information 
 * that is contained in a software package.
 * @author Gary O'Neall
 *
 */
public class SpdxFile extends SpdxItem implements Comparable<SpdxFile> {
	
	static final Logger logger = LoggerFactory.getLogger(SpdxFile.class.getName());

	public enum FileType {fileType_application, fileType_archive,
		fileType_audio, fileType_binary, fileType_documentation,
		fileType_image, fileType_other, fileType_source, fileType_spdx,
		fileType_text, fileType_video;

		/**
		 * @return The tag for this file type.
		 */
		public String getTag(){
			return FILE_TYPE_TO_TAG.get(this);
		}

		/**
		 *
		 * @param tag
		 * @return The file type corresponding to the provided tag.
		 */
		public static FileType fromTag(String tag){
			return TAG_TO_FILE_TYPE.get(tag);
		}
	}

	@Deprecated
	/**
	 * Use {@link FileType#getTag()} instead.
	 * @deprecated
	 */
	public static final Map<FileType, String> FILE_TYPE_TO_TAG;

	@Deprecated
	/**
	 * Use {@link org.spdx.rdfparser.model.SpdxFile.FileType#fromTag(String)} instead
	 * @deprecated
	 */
	public static final Map<String, FileType> TAG_TO_FILE_TYPE;

	static {
		ImmutableMap.Builder<FileType, String> fileTypeToTagBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<String, FileType> tagToFileTypeBuilder = ImmutableMap.builder();
		
		fileTypeToTagBuilder.put(FileType.fileType_application, "APPLICATION");
		tagToFileTypeBuilder.put("APPLICATION", FileType.fileType_application);
		fileTypeToTagBuilder.put(FileType.fileType_archive, "ARCHIVE");
		tagToFileTypeBuilder.put("ARCHIVE", FileType.fileType_archive);
		fileTypeToTagBuilder.put(FileType.fileType_audio, "AUDIO");
		tagToFileTypeBuilder.put("AUDIO", FileType.fileType_audio);
		fileTypeToTagBuilder.put(FileType.fileType_binary, "BINARY");
		tagToFileTypeBuilder.put("BINARY", FileType.fileType_binary);
		fileTypeToTagBuilder.put(FileType.fileType_documentation, "DOCUMENTATION");
		tagToFileTypeBuilder.put("DOCUMENTATION", FileType.fileType_documentation);
		fileTypeToTagBuilder.put(FileType.fileType_image, "IMAGE");
		tagToFileTypeBuilder.put("IMAGE", FileType.fileType_image);
		fileTypeToTagBuilder.put(FileType.fileType_other, "OTHER");
		tagToFileTypeBuilder.put("OTHER", FileType.fileType_other);
		fileTypeToTagBuilder.put(FileType.fileType_source, "SOURCE");
		tagToFileTypeBuilder.put("SOURCE", FileType.fileType_source);
		fileTypeToTagBuilder.put(FileType.fileType_spdx, "SPDX");
		tagToFileTypeBuilder.put("SPDX", FileType.fileType_spdx);
		fileTypeToTagBuilder.put(FileType.fileType_text, "TEXT");
		tagToFileTypeBuilder.put("TEXT", FileType.fileType_text);
		fileTypeToTagBuilder.put(FileType.fileType_video, "VIDEO");
		tagToFileTypeBuilder.put("VIDEO", FileType.fileType_video);

		FILE_TYPE_TO_TAG = fileTypeToTagBuilder.build();
		TAG_TO_FILE_TYPE = tagToFileTypeBuilder.build();

	}
	FileType[] fileTypes = new FileType[0];
	Checksum[] checksums;
	String[] fileContributors = new String[0];
	String noticeText;
	DoapProject[] artifactOf = new DoapProject[0];
	SpdxFile[] fileDependencies = new SpdxFile[0];

	/**
	 * @param name fileName
	 * @param comment Comment on the file
	 * @param annotations annotations for the file
	 * @param relationships Relationships to this file
	 * @param licenseConcluded
	 * @param licenseInfoInFile
	 * @param copyrightText
	 * @param licenseComment
	 * @throws InvalidSPDXAnalysisException 
	 */
	public SpdxFile(String name, String comment, Annotation[] annotations,
			Relationship[] relationships, AnyLicenseInfo licenseConcluded,
			AnyLicenseInfo[] licenseInfoInFile, String copyrightText,
			String licenseComment, FileType[] fileTypes, Checksum[] checksums,
			String[] fileContributors, String noticeText, DoapProject[] artifactOf) throws InvalidSPDXAnalysisException {
		super(name, comment, annotations, relationships, 
				licenseConcluded, licenseInfoInFile,
				copyrightText, licenseComment);
		this.fileTypes = fileTypes;
		if (this.fileTypes == null) {
			this.fileTypes = new FileType[0];
		}
		this.checksums = checksums;
		if (this.checksums == null) {
			this.checksums = new Checksum[0];
		}
		this.fileContributors = fileContributors;
		if (this.fileContributors == null) {
			this.fileContributors = new String[0];
		}
		this.noticeText = noticeText;
		this.fileDependencies = new SpdxFile[0];
		this.artifactOf = artifactOf;
		if (this.artifactOf == null) {
			this.artifactOf = new DoapProject[0];
		}
	}
	

	/**
	 * @param fileName
	 * @param fileTypes
	 * @param sha1Value
	 * @param licenseConcluded
	 * @param licenseInfoInFiles
	 * @param licenseComment
	 * @param copyrightText
	 * @param artifactOfs
	 * @param fileComment
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxFile(String fileName, FileType[] fileTypes, String sha1Value,
			AnyLicenseInfo licenseConcluded, AnyLicenseInfo[] licenseInfoInFiles,
			String licenseComment, String copyrightText,
			DoapProject[] artifactOfs, String fileComment) throws InvalidSPDXAnalysisException {
		this(fileName, fileComment, new Annotation[0], new Relationship[0], licenseConcluded, 
				licenseInfoInFiles, copyrightText, licenseComment, fileTypes, 
				new Checksum[] {new Checksum(ChecksumAlgorithm.checksumAlgorithm_sha1, sha1Value) },
				new String[0], "", artifactOfs);
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
		String[] fileTypeUris = findUriPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_TYPE);
		this.fileTypes = urisToFileType(fileTypeUris, false);
		this.checksums = findMultipleChecksumPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_CHECKSUM);
		this.fileContributors = findMultiplePropertyValues(SpdxRdfConstants.SPDX_NAMESPACE,
				SpdxRdfConstants.PROP_FILE_CONTRIBUTOR);
		this.noticeText = findSinglePropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_NOTICE);
		// File dependencies
		SpdxElement[] fileDependencyElements = findMultipleElementPropertyValues(
				SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_FILE_DEPENDENCY);
		int count = 0;
		if (fileDependencyElements != null) {
			for (int i = 0; i < fileDependencyElements.length; i++) {
				if (fileDependencyElements[i] instanceof SpdxFile) {
					count++;
				}
			}
		}
		if (count > 0) {
			this.fileDependencies = new SpdxFile[count];
			int j = 0;
			for (int i = 0; i < fileDependencyElements.length; i++) {
				if (fileDependencyElements[i] instanceof SpdxFile) {
					this.fileDependencies[j++] = (SpdxFile)fileDependencyElements[i];
				}
			}
		} else {
			this.fileDependencies = new SpdxFile[0];
		}
		// ArtifactOfs
		this.artifactOf = findMultipleDoapPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE,
				SpdxRdfConstants.PROP_FILE_ARTIFACTOF);
	}
	
	/**
	 * @param modelContainer
	 * @param node
	 * @throws InvalidSPDXAnalysisException
	 */
	public SpdxFile(IModelContainer modelContainer, Node node)
			throws InvalidSPDXAnalysisException {
		super(modelContainer, node);
		getMyPropertiesFromModel();
	}

	/**
	 * Finds the resource for an existing file in the model
	 * @param spdxFile
	 * @return resource of an SPDX file with the same name and checksum.  Null if none found
	 * @throws InvalidSPDXAnalysisException 
	 */
	static protected Resource findFileResource(IModelContainer modelContainer, SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
		// find any matching file names
		Model model = modelContainer.getModel();
		Node fileNameProperty = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_NAME).asNode();
		if (spdxFile.getName() == null) {
			return null;	// Can't match without a name
		}
		Triple fileNameMatch = Triple.createMatch(null, fileNameProperty, NodeFactory.createLiteral(spdxFile.getName()));
		
		ExtendedIterator<Triple> filenameMatchIter = model.getGraph().find(fileNameMatch);	
		if (filenameMatchIter.hasNext()) {
			Triple fileMatchTriple = filenameMatchIter.next();
			Node fileNode = fileMatchTriple.getSubject();
			// check the checksum
			Node checksumProperty = model.getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_CHECKSUM).asNode();
			Triple checksumMatch = Triple.createMatch(fileNode, checksumProperty, null);
			ExtendedIterator<Triple> checksumMatchIterator = model.getGraph().find(checksumMatch);
			if (checksumMatchIterator.hasNext()) {
				Triple checksumMatchTriple = checksumMatchIterator.next();
				Checksum cksum = new Checksum(modelContainer, checksumMatchTriple.getObject());
				if (cksum.getAlgorithm().equals(ChecksumAlgorithm.checksumAlgorithm_sha1) &&
						cksum.getValue().compareToIgnoreCase(spdxFile.getSha1()) == 0) {
					return RdfParserHelper.convertToResource(model, fileNode);
				}
			}
		}
		// if we get to here, we did not find a match
		return null;
	}
	
	@Override
	public Resource findDuplicateResource(IModelContainer modelContainer, String uri) throws InvalidSPDXAnalysisException {
		// see if we want to change what is considered a duplicate
		// currently, a file is considered a duplicate if the checksum and filename
		// are the same.
		return findFileResource(modelContainer, this);
	}
	
	/**
	 * @return the Sha1 checksum value for this file, or a blank string if no 
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
	 * Converts URI's for the different file types to file types
	 * @param uris
	 * @param ignoreErrors If true, any URI's that don't correspond to a know file type will not be included.  If true, an exception is thrown.
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private FileType[] urisToFileType(String[] uris, boolean ignoreErrors) throws InvalidSPDXAnalysisException {
		List<FileType> retval = Lists.newArrayList();
		for (int i = 0; i < uris.length; i++) {
			if (uris[i] != null && !uris[i].isEmpty()) {
				if (!uris[i].startsWith(SpdxRdfConstants.SPDX_NAMESPACE)) {
					throw(new InvalidSPDXAnalysisException("Invalid file type: "+uris[i]));
				}
				String fileTypeS = uris[i].substring(SpdxRdfConstants.SPDX_NAMESPACE.length());
				try {
					retval.add(FileType.valueOf(fileTypeS));
				} catch (Exception ex) {
					logger.error("Invalid file type in the model - "+fileTypeS);
					if (!ignoreErrors) {
						throw(new InvalidSPDXAnalysisException("Invalid file type: "+uris[i]));
					}
				}
			}
		}
		return retval.toArray(new FileType[retval.size()]);
	}
	
	private String[] fileTypesToUris(FileType[] fileTypes) {
		String[] retval = new String[fileTypes.length];
		for (int i = 0; i < retval.length; i++) {
			retval[i] = SpdxRdfConstants.SPDX_NAMESPACE + fileTypes[i].toString();
		}
		return retval;
	}


	@Override
	public void populateModel() throws InvalidSPDXAnalysisException {
		super.populateModel();
		setPropertyUriValues(SpdxRdfConstants.SPDX_NAMESPACE, 
			SpdxRdfConstants.PROP_FILE_TYPE, 
			fileTypesToUris(this.fileTypes));
		setPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_CHECKSUM, this.checksums);		
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
			SpdxRdfConstants.PROP_FILE_CONTRIBUTOR, this.fileContributors);
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
			SpdxRdfConstants.PROP_FILE_NOTICE, noticeText);
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
			SpdxRdfConstants.PROP_FILE_ARTIFACTOF, artifactOf);
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
			SpdxRdfConstants.PROP_FILE_FILE_DEPENDENCY, fileDependencies, false);
	}
	
	@Override
	protected String getLicenseInfoFromFilesPropertyName() {
		return SpdxRdfConstants.PROP_FILE_SEEN_LICENSE;
	}
	
	@Override
	protected String getNamePropertyName() {
		return SpdxRdfConstants.PROP_FILE_NAME;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getType(org.apache.jena.rdf.model.Model)
	 */
	@Override
	public Resource getType(Model model) {
		return model.createResource(SpdxRdfConstants.SPDX_NAMESPACE + SpdxRdfConstants.CLASS_SPDX_FILE);
	}
	
	/**
	 * @return the fileType
	 */
	public FileType[] getFileTypes() {
		if (this.resource != null && this.refreshOnGet) {
			String[] fileTypeUris = findUriPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_FILE_TYPE);
			try {
				this.fileTypes = urisToFileType(fileTypeUris, true);
			} catch (InvalidSPDXAnalysisException e) {
				// ignore the error
			} 
		}
		return fileTypes;
	}

	/**
	 * @param fileTypes the fileTypes to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void setFileTypes(FileType[] fileTypes) throws InvalidSPDXAnalysisException {
		this.fileTypes = fileTypes;
		if (this.fileTypes == null) {
			this.fileTypes = new FileType[0];
		}
		setPropertyUriValues(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_TYPE, 
				fileTypesToUris(this.fileTypes));
	}
	
	/**
	 * Add a file type to this file
	 * @param fileType
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void addFileType(FileType fileType) throws InvalidSPDXAnalysisException {
		if (fileType == null) {
			return;
		}
		this.fileTypes = Arrays.copyOf(this.fileTypes, this.fileTypes.length + 1);
		this.fileTypes[this.fileTypes.length-1] = fileType;
		addPropertyUriValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_TYPE, fileType.toString());
	}

	/**
	 * @return the checksums
	 */
	public Checksum[] getChecksums() {
		if (this.resource != null && this.refreshOnGet) {
			try {
				Checksum[] refresh = findMultipleChecksumPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
						SpdxRdfConstants.PROP_FILE_CHECKSUM);
				if (refresh == null || !arraysEquivalent(refresh, this.checksums, true)) {
					this.checksums = refresh;
				}
			} catch (InvalidSPDXAnalysisException e) {
				logger.error("Invalid checksum in model");
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
		setPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_CHECKSUM, this.checksums);
	}
	
	/**
	 * Add a checksum
	 * @param checksum
	 * @throws InvalidSPDXAnalysisException
	 */
	public void addChecksum(Checksum checksum) throws InvalidSPDXAnalysisException {
		if (checksum == null) {
			return;
		}
		this.checksums = Arrays.copyOf(this.checksums, this.checksums.length + 1);
		this.checksums[this.checksums.length - 1] = checksum;
		addPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_CHECKSUM, checksum);
	}

	/**
	 * @return the fileContributors
	 */
	public String[] getFileContributors() {
		if (this.resource != null && this.refreshOnGet) {
			this.fileContributors = findMultiplePropertyValues(SpdxRdfConstants.SPDX_NAMESPACE,
					SpdxRdfConstants.PROP_FILE_CONTRIBUTOR);
		}
		return fileContributors;
	}

	/**
	 * @param fileContributors the fileContributors to set
	 */
	public void setFileContributors(String[] fileContributors) {	
		if (fileContributors == null) {
			this.fileContributors = new String[0];
		} else {
			this.fileContributors = fileContributors;
		}
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
				SpdxRdfConstants.PROP_FILE_CONTRIBUTOR, fileContributors);
	}

	/**
	 * @return the noticeText
	 */
	public String getNoticeText() {
		if (this.resource != null && this.refreshOnGet) {
			this.noticeText = findSinglePropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_FILE_NOTICE);
		}
		return noticeText;
	}

	/**
	 * @param noticeText the noticeText to set
	 */
	public void setNoticeText(String noticeText) {
		this.noticeText = noticeText;
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_NOTICE, noticeText);
	}
	
	/**
	 * Artifact of has been replaced by a relationship to a package with the filesAnalyzed set to false
	 * @return the artifactOf
	 */
	@Deprecated
	public DoapProject[] getArtifactOf() {
		if (this.resource != null && this.refreshOnGet) {
			try {
				DoapProject[] refresh = findMultipleDoapPropertyValues(SpdxRdfConstants.SPDX_NAMESPACE,
						SpdxRdfConstants.PROP_FILE_ARTIFACTOF);
				if (refresh == null || !arraysEquivalent(refresh, this.artifactOf, true)) {
					this.artifactOf = refresh;
				}
			} catch (InvalidSPDXAnalysisException e) {
				logger.error("Invalid artifact of in the model");
			}
		}
		return artifactOf;
	}

	/**
	 * Artifact of has been replaced by a relationship to a package with the filesAnalyzed set to false
	 * @param artifactOf the artifactOf to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	@Deprecated
	public void setArtifactOf(DoapProject[] artifactOf) throws InvalidSPDXAnalysisException {
		if (artifactOf == null) {
			this.artifactOf = new DoapProject[0];
		} else {
			this.artifactOf = artifactOf;
		}
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_ARTIFACTOF, this.artifactOf);
	}

	/**
	 * This method should no longer be used.  The Relationship property should be used in its place.
	 * @return the fileDependencies
	 */
	@Deprecated
	public SpdxFile[] getFileDependencies() {
		if (this.resource != null && this.refreshOnGet) {
			try {
				SpdxElement[] fileDependencyElements = findMultipleElementPropertyValues(
						SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_FILE_FILE_DEPENDENCY);
				if (!arraysEquivalent(fileDependencyElements, this.fileDependencies, false)) {
					int count = 0;
					if (fileDependencyElements != null) {
						for (int i = 0; i < fileDependencyElements.length; i++) {
							if (fileDependencyElements[i] instanceof SpdxFile) {
								count++;
							}
						}
					}
					if (count > 0) {
						this.fileDependencies = new SpdxFile[count];
						int j = 0;
						for (int i = 0; i < fileDependencyElements.length; i++) {
							if (fileDependencyElements[i] instanceof SpdxFile) {
								this.fileDependencies[j++] = (SpdxFile)fileDependencyElements[i];
							}
						}
					}
				}
			} catch (InvalidSPDXAnalysisException ex) {
				logger.error("Error getting file dependencies",ex);
			}
		}
		return fileDependencies;
	}

	/**
	 * This method should no longer be used.  The Relationship property should be used in its place.
	 * @param fileDependencies the fileDependencies to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	@Deprecated
	public void setFileDependencies(SpdxFile[] fileDependencies) throws InvalidSPDXAnalysisException {
		if (fileDependencies == null) {
			this.fileDependencies = new SpdxFile[0];
		} else {
			this.fileDependencies = fileDependencies;
		}
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_FILE_FILE_DEPENDENCY, this.fileDependencies);
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
		if (!(o instanceof SpdxFile)) {
			return false;
		}
		SpdxFile comp = (SpdxFile)o;
		if (!super.equivalent(comp, testRelationships)) {
			return false;
		}
		// compare based on properties
		// Note: We don't compare the ID's since they may be different if they come
		// from different models
		return (arraysEquivalent(this.getChecksums(), comp.getChecksums(), testRelationships) &&
				RdfModelHelper.arraysEqual(this.getFileTypes(), comp.getFileTypes())&&
				RdfModelHelper.arraysEqual(this.getFileContributors(), comp.getFileContributors()) &&
				arraysEquivalent(this.getArtifactOf(), comp.getArtifactOf(), testRelationships) &&
				arraysEquivalent(this.getFileDependencies(), comp.getFileDependencies(), testRelationships) &&
				RdfModelHelper.stringsEquivalent(this.getNoticeText(), comp.getNoticeText()));
	}
	
	protected Checksum[] cloneChecksum() {
		if (checksums == null) {
			return null;
		}
		Checksum[] retval = new Checksum[checksums.length];
		for (int i = 0; i < checksums.length; i++) {
			retval[i] = checksums[i].clone();
		}
		return retval;
	}
	
	protected DoapProject[] cloneArtifactOf() {
		if (this.artifactOf == null) {
			return null;
		}
		DoapProject[] retval = new DoapProject[this.artifactOf.length];
		for (int i = 0; i < this.artifactOf.length; i++) {
			retval[i] = artifactOf[i].clone();
		}
		return retval;
	}
	
	public SpdxFile[] cloneFileDependencies(Map<String, SpdxElement> clonedElementIds) {
		if (this.fileDependencies == null) {
			return null;
		}
		SpdxFile[] retval = new SpdxFile[this.fileDependencies.length];
		for (int i = 0; i < this.fileDependencies.length; i++) {
			retval[i] = this.fileDependencies[i].clone(clonedElementIds);
		}
		return retval;
	}
	
	@Override
    public SpdxFile clone(Map<String, SpdxElement> clonedElementIds) {
		if (clonedElementIds.containsKey(this.getId())) {
			return (SpdxFile)clonedElementIds.get(this.getId());
		}
		SpdxFile retval;
		try {
			retval = new SpdxFile(name, comment, cloneAnnotations(),
					null, cloneLicenseConcluded(),
					cloneLicenseInfosFromFiles(), copyrightText,
					licenseComments, fileTypes, cloneChecksum(),
					fileContributors, noticeText, cloneArtifactOf());
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Error cloning file: ",e);
			retval = null;
		}
		clonedElementIds.put(this.getId(), retval);
		if(retval != null){
    		try {
    			retval.setRelationships(cloneRelationships(clonedElementIds));
    		} catch (InvalidSPDXAnalysisException e) {
    			logger.error("Unexected error setting relationships during clone",e);
    		}
    		if (this.fileDependencies != null) {
    			try {
    				retval.setFileDependencies(cloneFileDependencies(clonedElementIds));
    			} catch (InvalidSPDXAnalysisException e1) {
    				logger.warn("Error setting file dependencies on clone", e1);
    			}
    		}
		}
		return retval;
	}
	
	@Override 
	public SpdxFile clone() {
		return clone(Maps.<String, SpdxElement>newHashMap());
	}
	
	@Override
	public List<String> verify() {
		List<String> retval = super.verify();
		String fileName = this.getName();
		if (fileName == null) {
			fileName = "UNKNOWN";
		}
		if (checksums == null || checksums.length == 0) {
			retval.add("Missing required checksum for file "+fileName);
		} else {
			for (int i = 0; i < checksums.length; i++) {
				List<String> verify = checksums[i].verify();
				addNameToWarnings(verify);
				retval.addAll(verify);
			}			
		}
		String sha1 = getSha1();
		if (sha1 == null || sha1.isEmpty()) {
			retval.add("Missing required SHA1 hashcode value for "+name);
		}
		DoapProject[] projects = this.getArtifactOf();
		if (projects != null) {
			for (int i = 0;i < projects.length; i++) {
				retval.addAll(projects[i].verify());
			}
		}	
		// fileDependencies
		if (fileDependencies != null) {
			for (int i = 0; i < fileDependencies.length; i++) {
				List<String> verifyFileDependency = fileDependencies[i].verify();
				for (int j = 0; j < verifyFileDependency.size(); j++) {
					retval.add("Invalid file dependency for file named "+
							fileDependencies[i].getName()+": "+verifyFileDependency.get(j));
				}
			}
		}
		return retval;
	}

    /**
     * This method is used for sorting a list of SPDX files
     * @param file SPDXFile that is compared
     * @return 
     */
    @Override
    public int compareTo(SpdxFile file) {
    	int retval = this.getName().compareTo(file.getName());
        return retval;        
    }

    // the following methods are added as a TEMPORARY convenience to those
    // migrating from the 1.2 version of the utilities
	/**
	 * This method should be replaced by the more consistent getCopyrightText
	 * This method will be removed in a future release
	 * @return
	 */
    @Deprecated
	public String getCopyright() {
		return this.getCopyrightText();
	}

    /**
	 * This method should be replaced by the more consistent setCopyrightText
	 * This method will be removed in a future release
     * @param copyright
     */
    @Deprecated
    public void setCopyright(String copyright) {
    	this.setCopyrightText(copyright);
    }

	/**
	 * This method should be replaced by the more consistent getLicenseConcluded
	 * This method will be removed in a future release
	 * @return
	 */
    @Deprecated
	public AnyLicenseInfo getConcludedLicenses() {
		return this.getLicenseConcluded();
	}
    
	/**
	 * This method should be replaced by the more consistent setLicenseConcluded
	 * This method will be removed in a future release
	 * @throws InvalidSPDXAnalysisException 
	 */
    @Deprecated
	public void setConcludedLicenses(AnyLicenseInfo concludedLicense) throws InvalidSPDXAnalysisException {
		this.setLicenseConcluded(concludedLicense);
	}

	/**
	 * This method should be replaced by the more consistent getFileContributors
	 * This method will be removed in a future release
	 * @return
	 */
    @Deprecated
	public String[] getContributors() {
		return this.getFileContributors();
	}
    
	/**
	 * This method should be replaced by the more consistent setFileContributors
	 * This method will be removed in a future release
	 * @return
	 */
    @Deprecated
	public void setContributors(String[] contributors) {
		this.setFileContributors(contributors);
	}

	/**
	 * This method should be replaced by the more consistent getLicenseInfoFromFiles
	 * This method will be removed in a future release
	 * @return
	 */
    @Deprecated
	public AnyLicenseInfo[] getSeenLicenses() {
		return this.getLicenseInfoFromFiles();
	}
    
	/**
	 * This method should be replaced by the more consistent setLicenseInfoFromFiles
	 * This method will be removed in a future release
	 * @throws InvalidSPDXAnalysisException 
	 */
    @Deprecated
	public void setSeenLicenses(AnyLicenseInfo[] seenLicenses) throws InvalidSPDXAnalysisException {
		this.setLicenseInfosFromFiles(seenLicenses);
	}
}


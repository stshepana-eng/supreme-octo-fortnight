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

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.SpdxVerificationHelper;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Information about an external SPDX document reference including the checksum.  
 * This allows for verification of the external references.
 * 
 * Since an SPDX document must be in its own container, there are a few special
 * considerations for this class:
 *   - model, node, and resource are associated with the document making an external reference,
 *     it does not include the actual document being referenced
 *   - This class can be used with only the URI for the external document being provided.  It
 *     does not require the entire document to be passed in.  The spdxDocument itself is optional.
 * @author Gary O'Neall
 *
 */
public class ExternalDocumentRef extends RdfModelObject implements Comparable<ExternalDocumentRef> {

	static final Logger logger = LoggerFactory.getLogger(RdfModelObject.class.getClass());
	/**
	 * Force a refresh for the model on every property get.  This is slower, but
	 * will make sure that the correct value is returned if there happens to be
	 * two Java objects using the same RDF properties.
	 */

	Checksum checksum;
	String spdxDocumentNamespace;
	String externalDocumentId;
	SpdxDocument spdxDocument = null;
	
	/**
	 * @param modelContainer
	 * @param node
	 * @throws InvalidSPDXAnalysisException
	 */
	public ExternalDocumentRef(IModelContainer modelContainer, Node node)
			throws InvalidSPDXAnalysisException {
		super(modelContainer, node);
		getPropertiesFromModel();
	}
	
	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getPropertiesFromModel()
	 */
	@Override
	public void getPropertiesFromModel() throws InvalidSPDXAnalysisException {
		this.checksum = findChecksumPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
				SpdxRdfConstants.PROP_EXTERNAL_DOC_CHECKSUM);
		this.spdxDocumentNamespace = findUriPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
				SpdxRdfConstants.PROP_EXTERNAL_SPDX_DOCUMENT);
		this.externalDocumentId = findSinglePropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_DOC_ID);
	}

	/**
	 * @param spdxDocumentUri Unique URI for the external SPDX document
	 * @param checksum Sha1 checksum for the external document
	 */
	public ExternalDocumentRef(String spdxDocumentUri, Checksum checksum, String externalDocumentId) {
		this.spdxDocumentNamespace = spdxDocumentUri;
		this.checksum = checksum;
		this.externalDocumentId = externalDocumentId;
	}
	
	/**
	 * @param externalDocument SPDX Document being referenced
	 * @param checksum Sha1 checksum of the external document
	 */
	public ExternalDocumentRef(SpdxDocument externalDocument, Checksum checksum, String externalDocumentId) {
		this.spdxDocument = externalDocument;
		this.spdxDocumentNamespace = documentToDocumentUri(externalDocument);
		this.externalDocumentId = externalDocumentId;
		this.checksum = checksum;
	}

	/**
	 * Gets the absolute URI representing the document
	 * @param document
	 * @return
	 */
	private String documentToDocumentUri(SpdxDocument document) {
		if (document == null) {
			return null;
		}
		String retval = document.getDocumentContainer().getDocumentNamespace();
		if (retval.endsWith("#")) {
			retval = retval.substring(0, retval.length()-1);
		}
		return retval;
	}


	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.IRdfModel#verify()
	 */
	@Override
	public List<String> verify() {
		List<String> retval = Lists.newArrayList();
		String uri = "UNKNOWN";
		if (this.spdxDocumentNamespace == null) {
			retval.add("Missing required external document URI");
		} else {
			uri = this.spdxDocumentNamespace;
			if (!SpdxVerificationHelper.isValidUri(uri)) {
				retval.add("Invalid URI for external Spdx Document URI: "+this.spdxDocumentNamespace);
			}
		}
		if (this.checksum == null) {
			retval.add("Missing checksum for external document "+uri);
		} else {
			retval.addAll(this.checksum.verify());
			if (this.checksum.getAlgorithm() != ChecksumAlgorithm.checksumAlgorithm_sha1) {
				retval.add("Checksum algorithm is not SHA1 for external reference "+uri);
			}
		}
		if (this.externalDocumentId == null) {
			retval.add("Missing external document ID for document "+uri);
		} else {
			if (!SpdxVerificationHelper.isValidExternalDocRef(this.externalDocumentId)) {
				retval.add("Invalid external document ID: "+this.externalDocumentId);
			}
		}
		return retval;
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getUri(org.spdx.rdfparser.IModelContainer)
	 */
	@Override
	public String getUri(IModelContainer modelContainer) {
		return null;	// these are always anonymous
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#getType(org.apache.jena.rdf.model.Model)
	 */
	@Override
	public Resource getType(Model model) {
		return model.createResource(SpdxRdfConstants.SPDX_NAMESPACE + 
				SpdxRdfConstants.CLASS_EXTERNAL_DOC_REF);
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#populateModel()
	 */
	@Override
	public void populateModel() throws InvalidSPDXAnalysisException {
		this.setPropertyUriValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_SPDX_DOCUMENT, 
				this.spdxDocumentNamespace);
		this.setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_DOC_CHECKSUM,
				this.checksum);
		this.setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_DOC_ID, this.externalDocumentId);
	}
	
	

	/**
	 * @return the checksum
	 * @throws InvalidSPDXAnalysisException 
	 */
	public Checksum getChecksum() throws InvalidSPDXAnalysisException {
		if (this.resource != null && refreshOnGet) {
			Checksum refreshedChecksum = findChecksumPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
					SpdxRdfConstants.PROP_EXTERNAL_DOC_CHECKSUM);
			if (refreshedChecksum == null || !refreshedChecksum.equivalent(this.checksum)) {
				this.checksum = refreshedChecksum;
			}
		}
		return checksum;
	}


	/**
	 * @param checksum the checksum to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void setChecksum(Checksum checksum) throws InvalidSPDXAnalysisException {
		this.checksum = checksum;
		this.setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_DOC_CHECKSUM,
				this.checksum);
	}


	/**
	 * @return the spdxDocumentNamespace
	 */
	public String getSpdxDocumentNamespace() {
		if (this.resource != null && refreshOnGet) {
			this.spdxDocumentNamespace = findUriPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE,
					SpdxRdfConstants.PROP_EXTERNAL_SPDX_DOCUMENT);
		}
		return spdxDocumentNamespace;
	}


	/**
	 * @param spdxDocumentNamespace the spdxDocumentNamespace to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void setSpdxDocumentNamespace(String spdxDocumentNamespace) throws InvalidSPDXAnalysisException {
		this.spdxDocumentNamespace = spdxDocumentNamespace;
		if (this.spdxDocumentNamespace == null) {
			this.removePropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_EXTERNAL_SPDX_DOCUMENT);
		} else {
			this.setPropertyUriValue(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_EXTERNAL_SPDX_DOCUMENT, 
					this.spdxDocumentNamespace);
		}
	}


	/**
	 * @return the spdxDocument
	 */
	public SpdxDocument getSpdxDocument() {
		return spdxDocument;
	}


	/**
	 * @param spdxDocument the spdxDocument to set
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void setSpdxDocument(SpdxDocument spdxDocument) throws InvalidSPDXAnalysisException {
		this.spdxDocument = spdxDocument;
		setSpdxDocumentNamespace(documentToDocumentUri(spdxDocument));
	}


	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.model.RdfModelObject#equivalent(org.spdx.rdfparser.model.RdfModelObject)
	 */
	@Override
	public boolean equivalent(IRdfModel compare) {
		if (compare == this) {
			return true;
		}
		if (!(compare instanceof ExternalDocumentRef)) {
			return false;
		}
		ExternalDocumentRef compref = (ExternalDocumentRef)compare;
		try {
            return (Objects.equal(this.getSpdxDocumentNamespace(), compref.getSpdxDocumentNamespace()) &&
                    equivalentConsideringNull(this.getChecksum(), compref.getChecksum()) && Objects.equal(this.getExternalDocumentId(),
                    compref.getExternalDocumentId()));
		} catch (InvalidSPDXAnalysisException e) {
			logger.error("Invald SPDX Analysis exception comparing external document references: "+e.getMessage(),e);
			return false;
		}
	}
	
	@Override
	public ExternalDocumentRef clone() {
		return new ExternalDocumentRef(this.spdxDocumentNamespace, this.checksum.clone(),
				this.externalDocumentId);
	}


	/**
	 *  a string containing letters, numbers, “.”, “-” or “+” which uniquely identifies an external document within this document.
	 * @return
	 */
	public String getExternalDocumentId() {
		if (this.resource != null && refreshOnGet) {
			this.externalDocumentId = findSinglePropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
					SpdxRdfConstants.PROP_EXTERNAL_DOC_ID);
		}
		return this.externalDocumentId;
	}
	
	/**
	 * @param externalDocumentId
	 */
	public void setExternalDocumentId(String externalDocumentId) {
		this.externalDocumentId = externalDocumentId;
		setPropertyValue(SpdxRdfConstants.SPDX_NAMESPACE, 
				SpdxRdfConstants.PROP_EXTERNAL_DOC_ID, externalDocumentId);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ExternalDocumentRef o) {
		if (o.getExternalDocumentId() == null) {
			if (this.externalDocumentId != null) {
				return 1;
			}
		}
		if (this.externalDocumentId == null) {
			return -1;
		}
		int retval = this.externalDocumentId.compareTo(o.getExternalDocumentId());
		if (retval != 0) {
			return retval;
		}
		if (o.getSpdxDocumentNamespace() == null) {
			if (this.spdxDocumentNamespace != null) {
				return 1;
			}
		}
		if (this.spdxDocumentNamespace == null) {
			return -1;
		}
		retval = this.spdxDocumentNamespace.compareTo(o.getSpdxDocumentNamespace());
		if (retval != 0) {
			return retval;
		}
		try {
			if (o.getChecksum() == null || o.getChecksum().getValue() == null) {
				if (this.checksum != null && this.checksum.getValue() != null) {
					return 1;
				}
			}
		} catch (InvalidSPDXAnalysisException e) {
			// do nothing
		}
		if (this.checksum == null || this.checksum.getValue() == null) {
			return -1;
		}
		try {
			return (this.checksum.getValue().compareTo(o.getChecksum().getValue()));
		} catch (InvalidSPDXAnalysisException e) {
			return 0;
		}
	}
}

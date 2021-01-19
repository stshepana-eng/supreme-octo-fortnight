/**
 * Copyright (c) 2014 Gang Ling.
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
package org.spdx.merge;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxPackage;

/**
 * @author Gang Ling
 *
 */
public class SpdxFileInfoMergerTest {

	static final String TEST_RDF_FILE_PATH = "TestFiles"+File.separator+"SPDXRdfExample-v2.0.rdf";
	File testFile;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.testFile = new File(TEST_RDF_FILE_PATH);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#SpdxFileInfoMerger(org.spdx.rdfparser.SpdxDocument.SpdxPackage)}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
	@Test
	public void testSpdxFileInfoMerger() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		List<SpdxPackage> packageInfoList = doc1.getDocumentContainer().findAllPackages();
		SpdxPackage[] packageInfo = packageInfoList.toArray(new SpdxPackage[packageInfoList.size()]);
//		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
	}

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#mergeFileInfo(org.spdx.rdfparser.SpdxDocument[])}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
/*	@Test
	public void testMergeFileInfo() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxDocument doc2 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxPackage packageInfo = doc1.getDocumentContainer().findAllPackages();
		ExtractedLicenseInfo[] subNonStdLics = doc2.getExtractedLicenseInfos();

		SpdxLicenseMapper mapper = new SpdxLicenseMapper();
		ExtractedLicenseInfo clonedNonStdLic = (ExtractedLicenseInfo) subNonStdLics[0].clone();
		mapper.mappingNewNonStdLic(doc1, doc2, clonedNonStdLic);

		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
		SpdxDocument [] subDocs = new SpdxDocument[]{doc2};
		SpdxFile[] mergedResult = fileMerger.mergeFileInfo(subDocs);

		SpdxFile[] expectedResult = packageInfo.getFiles();
		int num = 0;
		for(int i = 0; i < mergedResult.length; i++){
			for(int j = 0; j < expectedResult.length; j++){
				if(mergedResult[i].equivalent(expectedResult[j])){
					num ++;
					break;
				}
			}
		}
		assertEquals(3,num);
		assertEquals(expectedResult.length, mergedResult.length);
	}*/

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#checkDoapProject(org.spdx.rdfparser.SpdxFile)}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
/*	@Test
	public void testCheckDoapProject() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxPackage packageInfo = doc1.getSpdxPackage();
		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
		SpdxFile[] testFiles = packageInfo.getFiles();
		int num = 0;
		for(int i =0; i < testFiles.length; i++){
			if(fileMerger.checkDoapProject(testFiles[i])){
				num ++;
			}
		}
		assertEquals(2, num);
	}

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#mergeDOAPInfo(org.spdx.rdfparser.DoapProject[], org.spdx.rdfparser.DoapProject[])}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
/*	@Test
	public void testMergeDOAPInfo() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxPackage packageInfo = doc1.getSpdxPackage();
		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
		SpdxFile[] testFiles = packageInfo.getFiles();
		List<DoapProject> testProjects = Lists.newArrayList();
		for(int i = 0; i < testFiles.length; i++){
			if(fileMerger.checkDoapProject(testFiles[i])){
				DoapProject[] retval = testFiles[i].getArtifactOf();
				for(int k = 0; k < retval.length; k++){
					testProjects.add(retval[k]);
				}
			}
		}
		DoapProject[] testProjects1 = new DoapProject[testProjects.size()];
		testProjects.toArray(testProjects1);
		DoapProject[] testProjects2 = new DoapProject[testProjects.size()];
		testProjects.toArray(testProjects2);

		DoapProject[] result = fileMerger.mergeDOAPInfo(testProjects1, testProjects2);
		assertEquals(testProjects1.length,result.length);
		assertEquals(2, result.length);
	}

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#cloneFiles(org.spdx.rdfparser.SpdxFile[])}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
/*	@Test
	public void testCloneFiles() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxPackage packageInfo = doc1.getSpdxPackage();
		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
		SpdxFile[] testFiles = packageInfo.getFiles();
		SpdxFile[] clonedFiles = fileMerger.cloneFiles(testFiles);
		int num = 0;
		for(int i = 0; i < clonedFiles.length; i++){
			for(int j = 0; j < testFiles.length; j++){
				if(clonedFiles[i].equivalent(testFiles[j])){
					num++;
					break;
				}
			}
		}
		assertEquals(3, num);
	}

	/**
	 * Test method for {@link org.spdx.merge.SpdxFileInfoMerger#cloneDoapProject(org.spdx.rdfparser.DoapProject[])}.
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
/*	@Test
	public void testCloneDoapProject() throws IOException, InvalidSPDXAnalysisException {
		SpdxDocument doc1 = SPDXDocumentFactory.createSpdxDocument(TEST_RDF_FILE_PATH);
		SpdxPackage packageInfo = doc1.getSpdxPackage();
		SpdxFileInfoMerger fileMerger = new SpdxFileInfoMerger(packageInfo, new SpdxLicenseMapper());
		SpdxFile[] testFiles = packageInfo.getFiles();
		List<DoapProject> testProjects = Lists.newArrayList();
		for(int i = 0; i < testFiles.length; i++){
			if(fileMerger.checkDoapProject(testFiles[i])){
				DoapProject[] projects = testFiles[i].getArtifactOf();
				DoapProject[] clonedProjects = fileMerger.cloneDoapProject(projects);
				for(int j = 0; j < clonedProjects.length; j++){
					testProjects.add(clonedProjects[j]);
				}
			}
		}
		assertEquals(2,testProjects.size());
	}
*/
}

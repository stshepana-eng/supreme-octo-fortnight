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
package org.spdx.rdfparser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.spdx.rdfparser.model.SpdxFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Generates a package verification code from a directory of source code or an array of <code>SPDXFile</code>s.
 *
 * A class implementing the IFileChecksumGenerator is supplied as a parameter to the constructor.
 * The method <code>getFileChecksum</code> is called for each file in the directory.  This can
 * be used as a hook to capture all files in the directory and capture the checksum values at
 * a file level.
 *
 * @author Gary O'Neall
 *
 */
public class VerificationCodeGenerator {

	private IFileChecksumGenerator fileChecksumGenerator;

	public VerificationCodeGenerator(IFileChecksumGenerator fileChecksumGenerator) {
		this.fileChecksumGenerator = fileChecksumGenerator;
	}

	/**
	 * Generate the SPDX Package Verification Code from an array of SPDXFiles
	 * @param spdxFiles Files to generate the VerificationCode from
	 * @param skippedFilePaths File path names to not include in the VerificationCode
	 * @return VerificationCode based on all files in spdxFiles minus the skippedFilePaths
	 * @throws NoSuchAlgorithmException
	 */
	public SpdxPackageVerificationCode generatePackageVerificationCode(SpdxFile[] spdxFiles, String[] skippedFilePaths) throws NoSuchAlgorithmException {
		if (spdxFiles == null) {
			return null;
		}
		Set<String> skippedFilePathSet = Sets.newTreeSet();
		if (skippedFilePaths != null) {
			for (int i = 0; i < skippedFilePaths.length; i++) {
				if (skippedFilePaths[i] != null) {
					skippedFilePathSet.add(skippedFilePaths[i]);
				}
			}
		}
		List<String> fileChecksums = Lists.newArrayList();
		for (int i = 0; i < spdxFiles.length; i++) {
			if (spdxFiles[i] != null && spdxFiles[i].getName() != null &&
					!skippedFilePathSet.contains(spdxFiles[i].getName())) {
				fileChecksums.add(spdxFiles[i].getSha1());
			}
		}
		return generatePackageVerificationCode(fileChecksums, skippedFilePaths);
	}


	/**
	 * Generate the SPDX Package Verification Code from a directory of files included in the archive
	 * @param sourceDirectory
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public SpdxPackageVerificationCode generatePackageVerificationCode(File sourceDirectory, File[] skippedFiles) throws NoSuchAlgorithmException, IOException {
		// create a sorted list of file paths
		Set<String> skippedFilesPath = Sets.newTreeSet();
		String rootOfDirectory = sourceDirectory.getAbsolutePath();
		int rootLen = rootOfDirectory.length()+1;
		for (int i = 0; i < skippedFiles.length; i++) {
			String skippedPath = normalizeFilePath(skippedFiles[i].getAbsolutePath().substring(rootLen));
			skippedFilesPath.add(skippedPath);
		}
		List<String> fileChecksums = Lists.newArrayList();
		collectFileData(rootOfDirectory, sourceDirectory, fileChecksums, skippedFilesPath);
		String[] skippedFileNames = new String[skippedFilesPath.size()];
		Iterator<String> iter = skippedFilesPath.iterator();
		int i = 0;
		while (iter.hasNext()) {
			skippedFileNames[i++] = iter.next();
		}
		return generatePackageVerificationCode(fileChecksums, skippedFileNames);
	}

	protected SpdxPackageVerificationCode generatePackageVerificationCode(List<String> fileChecksums,
			String[] skippedFilePaths) throws NoSuchAlgorithmException {
		Collections.sort(fileChecksums);
		MessageDigest verificationCodeDigest = MessageDigest.getInstance("SHA-1");
		for (int i = 0;i < fileChecksums.size(); i++) {
			byte[] hashInput = fileChecksums.get(i).getBytes(Charset.forName("UTF-8"));
			verificationCodeDigest.update(hashInput);
		}
		String value = convertChecksumToString(verificationCodeDigest.digest());
		SpdxPackageVerificationCode retval = new SpdxPackageVerificationCode(value, skippedFilePaths);
		return retval;
	}

	/**
	 * Collect the file level checksums and filenames
	 * @param prefixForRelative The portion of the filepath which preceeds the relative file path for the archive
	 * @param sourceDirectory
	 * @param fileNameAndChecksums
	 * @throws IOException
	 */
	private void collectFileData(String prefixForRelative, File sourceDirectory,
			List<String> fileNameAndChecksums, Set<String> skippedFiles) throws IOException {
		if (!sourceDirectory.isDirectory()) {
			return;
		}
		File[] filesAndDirs = sourceDirectory.listFiles();
		if (filesAndDirs == null) {
			return;
		}
		for (int i = 0; i < filesAndDirs.length; i++) {
			if (filesAndDirs[i].isDirectory()) {
				collectFileData(prefixForRelative, filesAndDirs[i], fileNameAndChecksums, skippedFiles);
			} else {
				String filePath = normalizeFilePath(filesAndDirs[i].getAbsolutePath()
						.substring(prefixForRelative.length()+1));
				if (!skippedFiles.contains(filePath)) {
					String checksumValue = this.fileChecksumGenerator.getFileChecksum(filesAndDirs[i]).toLowerCase();
					fileNameAndChecksums.add(checksumValue);
				}
			}
		}
	}

	/**
	 * Normalizes a file path per the SPDX spec
	 * @param nonNormalizedFilePath
	 * @return
	 */
	public static String normalizeFilePath(String nonNormalizedFilePath) {
		String filePath = nonNormalizedFilePath.replace('\\', '/').trim();
		if (filePath.contains("../")) {
			// need to remove these references
			String[] filePathParts = filePath.split("/");
			StringBuilder normalizedFilePath = new StringBuilder();
			for (int j = 0; j < filePathParts.length; j++) {
				if (j+1 < filePathParts.length && filePathParts[j+1].equals("..")) {
					// skip this directory
				} else if (filePathParts[j].equals("..")) {
					// remove these from the filePath
				} else {
					if (j > 0) {
						normalizedFilePath.append('/');
					}
					normalizedFilePath.append(filePathParts[j]);
				}
			}
			filePath = normalizedFilePath.toString();
		}
		filePath = filePath.replace("./", "");
		if (!filePath.isEmpty() && filePath.charAt(0) == '/') {
			filePath = "." + filePath;
		} else {
			filePath = "./" + filePath;
		}
		return filePath;
	}
	/**
	 * Convert a byte array SHA-1 digest into a 40 character hex string
	 * @param digest
	 * @return
	 */
	private static String convertChecksumToString(byte[] digest) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			String hex = Integer.toHexString(0xff & digest[i]);
			if (hex.length() < 2) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	/**
	 * @param sourceDirectory
	 * @param skippedFiles
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public SpdxPackageVerificationCode generatePackageVerificationCode(
			File sourceDirectory) throws NoSuchAlgorithmException, IOException {
		return generatePackageVerificationCode(sourceDirectory, new File[0]);
	}
}

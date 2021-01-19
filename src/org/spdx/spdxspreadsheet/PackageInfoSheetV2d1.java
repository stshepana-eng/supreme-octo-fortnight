/**
 * Copyright (c) 2016 Source Auditor Inc.
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
package org.spdx.spdxspreadsheet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.compare.CompareHelper;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SpdxVerificationHelper;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxNoneLicense;
import org.spdx.rdfparser.model.Annotation;
import org.spdx.rdfparser.model.Checksum;
import org.spdx.rdfparser.model.ExternalRef;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxPackage;

/**
 * Version 2.1 of the package info sheet
 * @author Gary O'Neall
 *
 */
public class PackageInfoSheetV2d1 extends PackageInfoSheet {

	int NAME_COL = 0;
	int ID_COL = NAME_COL + 1;
	int VERSION_COL = ID_COL+1;
	int MACHINE_NAME_COL = VERSION_COL+1;
	int SUPPLIER_COL = MACHINE_NAME_COL + 1;
	int ORIGINATOR_COL = SUPPLIER_COL + 1;
	int HOME_PAGE_COL = ORIGINATOR_COL + 1;
	int DOWNLOAD_URL_COL = HOME_PAGE_COL + 1;
	int PACKAGE_CHECKSUMS_COL = DOWNLOAD_URL_COL + 1;
	int FILE_VERIFICATION_VALUE_COL = PACKAGE_CHECKSUMS_COL + 1;
	int VERIFICATION_EXCLUDED_FILES_COL = FILE_VERIFICATION_VALUE_COL + 1;
	int SOURCE_INFO_COL = VERIFICATION_EXCLUDED_FILES_COL + 1;
	int DECLARED_LICENSE_COL = SOURCE_INFO_COL + 1;
	int CONCLUDED_LICENSE_COL = DECLARED_LICENSE_COL + 1;
	int LICENSE_INFO_IN_FILES_COL = CONCLUDED_LICENSE_COL + 1;
	int LICENSE_COMMENT_COL = LICENSE_INFO_IN_FILES_COL + 1;
	int DECLARED_COPYRIGHT_COL = LICENSE_COMMENT_COL + 1;
	int SHORT_DESC_COL = DECLARED_COPYRIGHT_COL + 1;
	int FULL_DESC_COL = SHORT_DESC_COL + 1;
	int FILES_ANALYZED_COL = FULL_DESC_COL + 1;
	int USER_DEFINED_COL = FILES_ANALYZED_COL + 1;
	int NUM_COLS = USER_DEFINED_COL;


	static final boolean[] REQUIRED = new boolean[] {true, true, false, false, false, false, false, true,
		false, false, false, false, true, true, false, false, true, false, false, false, false, false};
	static final String[] HEADER_TITLES = new String[] {"Package Name", "SPDX Identifier", "Package Version",
		"Package FileName", "Package Supplier", "Package Originator", "Home Page",
		"Package Download Location", "Package Checksum", "Package Verification Code",
		"Verification Code Excluded Files", "Source Info", "License Declared", "License Concluded", "License Info From Files",
		"License Comments", "Package Copyright Text", "Summary", "Description",
		"Files Analyzed", "External Refs", "User Defined Columns..."};

	static final int[] COLUMN_WIDTHS = new int[] {30, 17, 17, 30, 30, 30, 50, 50, 75, 60, 40, 30,
		40, 40, 90, 50, 50, 50, 80, 10, 50, 50};

	/**
	 * @param workbook
	 * @param sheetName
	 * @param version
	 */
	public PackageInfoSheetV2d1(Workbook workbook, String sheetName, String version) {
		super(workbook, sheetName, version);
	}

	/* (non-Javadoc)
	 * @see org.spdx.rdfparser.AbstractSheet#verify()
	 */
	@Override
	public String verify() {
		try {
			if (sheet == null) {
				return "Worksheet for SPDX Package Info does not exist";
			}
			if (!SPDXSpreadsheet.verifyVersion(version)) {
				return "Unsupported version "+version;
			}
			Row firstRow = sheet.getRow(firstRowNum);
			for (int i = 0; i < NUM_COLS - 1; i++) {
				Cell cell = firstRow.getCell(i+firstCellNum);
				if (cell == null ||
						cell.getStringCellValue() == null ||
						!cell.getStringCellValue().equals(HEADER_TITLES[i])) {
					return "Column "+HEADER_TITLES[i]+" missing for SPDX Package Info worksheet";
				}
			}
			// validate rows
			boolean done = false;
			int rowNum = firstRowNum + 1;
			while (!done) {
				Row row = sheet.getRow(rowNum);
				if (row == null || row.getCell(firstCellNum) == null) {
					done = true;
				} else {
					String error = validateRow(row);
					if (error != null) {
						return error;
					}
					rowNum++;
				}
			}
			return null;
		} catch (Exception ex) {
			return "Unexpected error in verifying SPDX Package Info work sheet: "+ex.getMessage();
		}
	}

	private String validateRow(Row row) {
		for (int i = 0; i < NUM_COLS; i++) {
			Cell cell = row.getCell(i);
			if (cell == null) {
				if (REQUIRED[i]) {
					return "Required cell "+HEADER_TITLES[i]+" missing for row "+String.valueOf(row.getRowNum() + " in PackageInfo sheet.");
				}
			} else {
				if (i == DECLARED_LICENSE_COL || i == CONCLUDED_LICENSE_COL) {
					try {
						LicenseInfoFactory.parseSPDXLicenseString(cell.getStringCellValue(), null);
					} catch(SpreadsheetException ex) {
						if (i == DECLARED_LICENSE_COL) {
							return "Invalid declared license in row "+String.valueOf(row.getRowNum())+" detail: "+ex.getMessage() + " in PackageInfo sheet.";
						} else {
							return "Invalid seen license in row "+String.valueOf(row.getRowNum())+" detail: "+ex.getMessage() + " in PackageInfo sheet.";
						}
					}
				} else if (i == LICENSE_INFO_IN_FILES_COL) {
					String[] licenses = row.getCell(LICENSE_INFO_IN_FILES_COL).getStringCellValue().split(",");
					if (licenses.length < 1) {
						return "Missing licenss information in files in PackageInfo sheet.";
					}
					for (int j = 0; j < licenses.length; j++) {
						try {
							LicenseInfoFactory.parseSPDXLicenseString(licenses[j], null);
						} catch(SpreadsheetException ex) {
							return "Invalid license information in in files for license "+licenses[j]+ " row "+String.valueOf(row.getRowNum())+" detail: "+ex.getMessage() + " in PackageInfo sheet.";
						}
					}
				} else if (i == ORIGINATOR_COL) {
					Cell origCell = row.getCell(ORIGINATOR_COL);
					if (origCell != null) {
						String originator = origCell.getStringCellValue();
						if (originator != null && !originator.isEmpty()) {
							String error = SpdxVerificationHelper.verifyOriginator(originator);
							if (error != null && !error.isEmpty()) {
								return "Invalid originator in row "+String.valueOf(row.getRowNum()) + ": "+error + " in PackageInfo sheet.";
							}
						}
					}
				} else if (i == SUPPLIER_COL) {
					Cell supplierCell = row.getCell(SUPPLIER_COL);
					if (supplierCell != null) {
						String supplier = supplierCell.getStringCellValue();
						if (supplier != null && !supplier.isEmpty()) {
							String error = SpdxVerificationHelper.verifySupplier(supplier);
							if (error != null && !error.isEmpty()) {
								return "Invalid supplier in row "+String.valueOf(row.getRowNum()) + ": "+error + " in PackageInfo sheet.";
							}
						}
					}
				} else if (i == FILES_ANALYZED_COL) {
					Cell filesAnalyzedCell = row.getCell(FILES_ANALYZED_COL);
					if (filesAnalyzedCell != null && filesAnalyzedCell.getStringCellValue() != null) {
						String filesAnalyzedStr = filesAnalyzedCell.getStringCellValue().trim().toLowerCase();
						if (!filesAnalyzedStr.equals("true") && !filesAnalyzedStr.equals("false")) {
							return "Invalid value for files analyzed (expecting 'true' or 'false') in row "+String.valueOf(row.getRowNum()) + ":" + filesAnalyzedStr;
						}
					}
				}
//				if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
//					return "Invalid cell format for "+HEADER_TITLES[i]+" for forw "+String.valueOf(row.getRowNum());
//				}
			}
		}
		return null;
	}

	public static void create(Workbook wb, String sheetName) {
		int sheetNum = wb.getSheetIndex(sheetName);
		if (sheetNum >= 0) {
			wb.removeSheetAt(sheetNum);
		}
		Sheet sheet = wb.createSheet(sheetName);
		CellStyle headerStyle = AbstractSheet.createHeaderStyle(wb);
		CellStyle defaultStyle = AbstractSheet.createLeftWrapStyle(wb);
		Row row = sheet.createRow(0);
		for (int i = 0; i < HEADER_TITLES.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i]*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
			cell.setCellValue(HEADER_TITLES[i]);
		}
	}

	public void add(SpdxPackage pkgInfo) throws InvalidSPDXAnalysisException {
		Row row = addRow();
		Cell nameCell = row.createCell(NAME_COL);
		nameCell.setCellValue(pkgInfo.getName());
		Cell idCell = row.createCell(ID_COL);
		idCell.setCellValue(pkgInfo.getId());
		Cell copyrightCell = row.createCell(DECLARED_COPYRIGHT_COL);
		copyrightCell.setCellValue(pkgInfo.getCopyrightText());
		Cell DeclaredLicenseCol = row.createCell(DECLARED_LICENSE_COL);
		DeclaredLicenseCol.setCellValue(pkgInfo.getLicenseDeclared().toString());
		Cell concludedLicenseCol = row.createCell(CONCLUDED_LICENSE_COL);
		concludedLicenseCol.setCellValue(pkgInfo.getLicenseConcluded().toString());
		Cell fileChecksumCell = row.createCell(FILE_VERIFICATION_VALUE_COL);
		if (pkgInfo.getPackageVerificationCode() != null) {
			fileChecksumCell.setCellValue(pkgInfo.getPackageVerificationCode().getValue());
			Cell verificationExcludedFilesCell = row.createCell(VERIFICATION_EXCLUDED_FILES_COL);
			StringBuilder excFilesStr = new StringBuilder();
			String[] excludedFiles = pkgInfo.getPackageVerificationCode().getExcludedFileNames();
			if (excludedFiles.length > 0) {
				excFilesStr.append(excludedFiles[0]);
				for (int i = 1;i < excludedFiles.length; i++) {
					excFilesStr.append(", ");
					excFilesStr.append(excludedFiles[i]);
				}
			}
			verificationExcludedFilesCell.setCellValue(excFilesStr.toString());
		}

		if (pkgInfo.getDescription() != null) {
			Cell descCell = row.createCell(FULL_DESC_COL);
			descCell.setCellValue(pkgInfo.getDescription());
		}
		Cell fileNameCell = row.createCell(MACHINE_NAME_COL);
		fileNameCell.setCellValue(pkgInfo.getPackageFileName());
		Cell checksumsCell = row.createCell(PACKAGE_CHECKSUMS_COL);
		Checksum[] checksums = pkgInfo.getChecksums();
		checksumsCell.setCellValue(CompareHelper.checksumsToString(checksums));
		// add the license infos in files in multiple rows
		AnyLicenseInfo[] licenseInfosInFiles = pkgInfo.getLicenseInfoFromFiles();
		if (licenseInfosInFiles != null && licenseInfosInFiles.length > 0) {
			StringBuilder sb = new StringBuilder(licenseInfosInFiles[0].toString());
			for (int i = 1; i < licenseInfosInFiles.length; i++) {
				sb.append(',');
				sb.append(licenseInfosInFiles[i].toString());
			}
			row.createCell(LICENSE_INFO_IN_FILES_COL).setCellValue(sb.toString());
		}
		if (pkgInfo.getLicenseComments() != null) {
			row.createCell(LICENSE_COMMENT_COL).setCellValue(pkgInfo.getLicenseComments());
		}
		if (pkgInfo.getSummary() != null) {
			Cell shortDescCell = row.createCell(SHORT_DESC_COL);
			shortDescCell.setCellValue(pkgInfo.getSummary());
		}
		if (pkgInfo.getSourceInfo() != null) {
			Cell sourceInfoCell = row.createCell(SOURCE_INFO_COL);
			sourceInfoCell.setCellValue(pkgInfo.getSourceInfo());
		}
		Cell urlCell = row.createCell(DOWNLOAD_URL_COL);
		urlCell.setCellValue(pkgInfo.getDownloadLocation());
		if (pkgInfo.getVersionInfo() != null) {
			Cell versionInfoCell = row.createCell(VERSION_COL);
			versionInfoCell.setCellValue(pkgInfo.getVersionInfo());
		}
		if (pkgInfo.getOriginator() != null) {
			Cell originatorCell = row.createCell(ORIGINATOR_COL);
			originatorCell.setCellValue(pkgInfo.getOriginator());
		}
		if (pkgInfo.getSupplier() != null) {
			Cell supplierCell = row.createCell(SUPPLIER_COL);
			supplierCell.setCellValue(pkgInfo.getSupplier());
		}
		if (pkgInfo.getHomepage() != null) {
			Cell homePageCell = row.createCell(HOME_PAGE_COL);
			homePageCell.setCellValue(pkgInfo.getHomepage());
		}
		Cell filesAnalyzedCell = row.createCell(FILES_ANALYZED_COL);
		if (pkgInfo.isFilesAnalyzed()) {
			filesAnalyzedCell.setCellValue("true");
		} else {
			filesAnalyzedCell.setCellValue("false");
		}
	}

	public SpdxPackage[] getPackages(SpdxDocumentContainer container) throws SpreadsheetException {
		SpdxPackage[] retval = new SpdxPackage[getNumDataRows()];
		for (int i = 0; i < retval.length; i++) {
			retval[i] = getPackage(getFirstDataRow() + i, container);
		}
		return retval;
	}

	@SuppressWarnings("deprecation")
	private SpdxPackage getPackage(int rowNum, SpdxDocumentContainer container) throws SpreadsheetException {
		Row row = sheet.getRow(rowNum);
		if (row == null) {
			return null;
		}
		Cell nameCell = row.getCell(NAME_COL);
		if (nameCell == null || nameCell.getStringCellValue().isEmpty()) {
			return null;
		}
		String error = validateRow(row);
		if (error != null && !error.isEmpty()) {
			throw(new SpreadsheetException(error));
		}
		String declaredName = nameCell.getStringCellValue();
		String id = row.getCell(ID_COL).getStringCellValue();
		Cell machineNameCell = row.getCell(MACHINE_NAME_COL);

		String machineName = null;
		if (machineNameCell != null) {
			machineName = row.getCell(MACHINE_NAME_COL).getStringCellValue();
		}
		Cell checksumsCell = row.getCell(PACKAGE_CHECKSUMS_COL);
		Checksum[] checksums = new Checksum[0];
		if (checksumsCell != null) {
			try {
				checksums = CompareHelper.strToChecksums(checksumsCell.getStringCellValue());
			} catch (InvalidSPDXAnalysisException e) {
				throw(new SpreadsheetException("Error converting file checksums: "+e.getMessage()));
			}
		}		String sourceInfo;
		Cell sourceInfocol = row.getCell(SOURCE_INFO_COL);
		if (sourceInfocol != null) {
			sourceInfo = sourceInfocol.getStringCellValue();
		} else {
			sourceInfo = "";
		}
		AnyLicenseInfo declaredLicenses =
				LicenseInfoFactory.parseSPDXLicenseString(row.getCell(DECLARED_LICENSE_COL).getStringCellValue(), container);
		AnyLicenseInfo concludedLicense;
		Cell concludedLicensesCell = row.getCell(CONCLUDED_LICENSE_COL);
		if (concludedLicensesCell != null && !concludedLicensesCell.getStringCellValue().isEmpty()) {
			concludedLicense = LicenseInfoFactory.parseSPDXLicenseString(concludedLicensesCell.getStringCellValue(), container);
		} else {
			concludedLicense = new SpdxNoneLicense();
		}
		Cell licenseInfoInFilesCell = row.getCell(LICENSE_INFO_IN_FILES_COL);
		String[] licenseStrings;
		if (licenseInfoInFilesCell != null) {
			licenseStrings = row.getCell(LICENSE_INFO_IN_FILES_COL).getStringCellValue().split(",");
		} else {
			licenseStrings = new String[0];
		}

		AnyLicenseInfo[] licenseInfosFromFiles = new AnyLicenseInfo[licenseStrings.length];
		for (int i = 0; i < licenseStrings.length; i++) {
			licenseInfosFromFiles[i] = LicenseInfoFactory.parseSPDXLicenseString(licenseStrings[i].trim(), container);
		}
		Cell licenseCommentCell = row.getCell(LICENSE_COMMENT_COL);
		String licenseComment;
		if (licenseCommentCell != null && !licenseCommentCell.getStringCellValue().isEmpty()) {
			licenseComment = licenseCommentCell.getStringCellValue();
		} else {
			licenseComment = "";
		}
		String declaredCopyright = row.getCell(DECLARED_COPYRIGHT_COL).getStringCellValue();
		Cell shortDescCell = row.getCell(SHORT_DESC_COL);
		String shortDesc;
		if (shortDescCell != null && !shortDescCell.getStringCellValue().isEmpty()) {
			shortDesc = shortDescCell.getStringCellValue();
		} else {
			shortDesc = "";
		}
		Cell descCell = row.getCell(FULL_DESC_COL);
		String description;
		if (descCell != null && !descCell.getStringCellValue().isEmpty()) {
			description = descCell.getStringCellValue();
		} else {
			description = "";
		}
		String url;
		Cell downloadUrlCell = row.getCell(DOWNLOAD_URL_COL);
		if (downloadUrlCell != null) {
			url = downloadUrlCell.getStringCellValue();
		} else {
			url = "";
		}
		Cell packageVerificationCell = row.getCell(FILE_VERIFICATION_VALUE_COL);
		String packageVerificationValue;
		if (packageVerificationCell != null) {
			packageVerificationValue = packageVerificationCell.getStringCellValue();
		} else {
			packageVerificationValue = "";
		}
		String[] excludedFiles;

		Cell excludedFilesCell = row.getCell(VERIFICATION_EXCLUDED_FILES_COL);
		String excludedFilesStr = null;
		if (excludedFilesCell != null) {
			excludedFilesStr = excludedFilesCell.getStringCellValue();
		}
		if (excludedFilesStr != null && !excludedFilesStr.isEmpty()) {
			excludedFiles = excludedFilesStr.split(",");
			for (int i = 0;i < excludedFiles.length; i++) {
				excludedFiles[i] = excludedFiles[i].trim();
			}
		} else {
			excludedFiles = new String[0];
		}
		Cell versionInfoCell = row.getCell(VERSION_COL);
		String versionInfo;
		if (versionInfoCell != null) {
			if (versionInfoCell.getCellTypeEnum()== CellType.STRING  && !versionInfoCell.getStringCellValue().isEmpty()) {
				versionInfo = versionInfoCell.getStringCellValue();
			} else if (versionInfoCell.getCellTypeEnum() == CellType.NUMERIC) {
				versionInfo = Double.toString(versionInfoCell.getNumericCellValue());
			} else {
				versionInfo = "";
			}

		} else {
			versionInfo = "";
		}
		String supplier;
		Cell supplierCell = row.getCell(SUPPLIER_COL);
		if (supplierCell != null && !supplierCell.getStringCellValue().isEmpty()) {
			supplier = supplierCell.getStringCellValue();
		} else {
			supplier = "";
		}
		String originator;
		Cell originatorCell = row.getCell(ORIGINATOR_COL);
		if (originatorCell != null && !originatorCell.getStringCellValue().isEmpty()) {
			originator = originatorCell.getStringCellValue();
		} else {
			originator = "";
		}
		String homePage;
		Cell homePageCell = row.getCell(HOME_PAGE_COL);
		if (homePageCell != null && !homePageCell.getStringCellValue().isEmpty()) {
			homePage = homePageCell.getStringCellValue();
		} else {
			homePage = "";
		}
		boolean filesAnalyzed = true;
		Cell filesAnalyzedCell = row.getCell(FILES_ANALYZED_COL);
		if (filesAnalyzedCell != null) {
			if (filesAnalyzedCell.getCellTypeEnum() == CellType.BOOLEAN) {
				filesAnalyzed = filesAnalyzedCell.getBooleanCellValue();
			} else {
				String filesAnalyzedStr = filesAnalyzedCell.getStringCellValue();
				if (filesAnalyzedStr != null) {
					if (filesAnalyzedStr.toLowerCase().trim().equals("false")) {
						filesAnalyzed = false;
					}
				}
			}
		}
		SpdxPackageVerificationCode verificationCode = new SpdxPackageVerificationCode(packageVerificationValue, excludedFiles);
		SpdxPackage retval = new SpdxPackage(declaredName, "", new Annotation[0],
				new Relationship[0], concludedLicense, licenseInfosFromFiles,
				declaredCopyright, licenseComment, declaredLicenses, checksums,
				description, url, new SpdxFile[0], homePage, originator,
				machineName, verificationCode, sourceInfo, shortDesc, supplier,
				versionInfo, filesAnalyzed, new ExternalRef[0]);
		try {
			retval.setId(id);
		} catch (InvalidSPDXAnalysisException e) {
			throw(new SpreadsheetException("Unable to set package ID: "+e.getMessage()));
		}
		return retval;
	}
}

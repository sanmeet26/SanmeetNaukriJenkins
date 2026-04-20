package com.framework.utils;

import com.framework.constants.FrameworkConstants;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : ExcelUtil
 * Description :
 * Utility class for reading Excel data (Data-Driven Testing).
 * <p>
 * Features:
 * - Read cell data
 * - Get row count
 * - Get column count
 * - Fetch data for DataProvider
 * <p>
 * =================================================================================================
 */

public final class ExcelUtil {

    private static Workbook workbook;
    private static Sheet sheet;

    /**
     * Private constructor
     */
    private ExcelUtil() {
    }

    // ===================== LOAD SHEET =====================
    public static void loadSheet(String sheetName) {
        try {
            FileInputStream fis = new FileInputStream(FrameworkConstants.EXCEL_FILE_PATH);
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheet(sheetName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load Excel file");
        }
    }

    // ===================== GET ROW COUNT =====================
    public static int getRowCount() {
        return sheet.getLastRowNum();
    }

    // ===================== GET COLUMN COUNT =====================
    public static int getColumnCount() {
        return sheet.getRow(0).getLastCellNum();
    }

    // ===================== GET CELL DATA =====================
    public static String getCellData(int rowNum, int colNum) {

        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(sheet.getRow(rowNum).getCell(colNum));
    }

    // ===================== GET ALL DATA =====================

    /**
     * Returns data in 2D Object array for TestNG DataProvider
     */
    public static Object[][] getTestData(String sheetName) {

        loadSheet(sheetName);

        int rows = getRowCount();
        int cols = getColumnCount();

        Object[][] data = new Object[rows][cols];

        for (int i = 1; i <= rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i - 1][j] = getCellData(i, j);
            }
        }

        return data;
    }
}
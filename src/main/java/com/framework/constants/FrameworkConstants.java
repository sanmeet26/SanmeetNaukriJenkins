package com.framework.constants;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : FrameworkConstants
 * Description :
 * This class holds all the framework-level constant values such as file paths,
 * timeouts, report locations, and other static configurations.
 * <p>
 * Why this class?
 * - Avoids hardcoding values across the framework
 * - Centralized control of configurations
 * - Improves maintainability and scalability
 * <p>
 * Note:
 * - This class should NOT be instantiated
 * - All variables are static and final
 * =================================================================================================
 */

public final class FrameworkConstants {

    // ===================== PRIVATE CONSTRUCTOR =====================

    // ===================== TIMEOUTS =====================
    public static final int EXPLICIT_WAIT = 10;
    public static final int PAGE_LOAD_TIMEOUT = 30;
    public static final int IMPLICIT_WAIT = 5;
    // ===================== RETRY =====================
    public static final int RETRY_COUNT = 1;
    // ===================== PROJECT PATH =====================
    private static final String PROJECT_PATH = System.getProperty("user.dir");
    // ===================== CONFIG FILE PATH =====================
    public static final String CONFIG_FILE_PATH =
            PROJECT_PATH + "/src/main/resources/config/config.properties";
    // ===================== TEST DATA (EXCEL) =====================
    public static final String EXCEL_FILE_PATH =
            PROJECT_PATH + "/src/main/resources/testdata/testdata.xlsx";
    // ===================== REPORTS =====================
    public static final String EXTENT_REPORT_PATH =
            PROJECT_PATH + "/reports/extent-report.html";
    // ===================== LOGS =====================
    public static final String LOG_FILE_PATH =
            PROJECT_PATH + "/logs/automation.log";
    // ===================== SCREENSHOTS =====================
    public static final String SCREENSHOT_PATH =
            PROJECT_PATH + "/screenshots/";
    // ===================== DRIVER PATHS =====================
    public static final String CHROME_DRIVER_PATH =
            PROJECT_PATH + "/drivers/chromedriver.exe";
    public static final String EDGE_DRIVER_PATH =
            PROJECT_PATH + "/drivers/edgedriver.exe";
    public static final String GECKO_DRIVER_PATH =
            PROJECT_PATH + "/drivers/geckodriver.exe";
    // ===================== REPORT CONFIG =====================
    public static final String EXTENT_CONFIG_PATH =
            PROJECT_PATH + "/src/main/resources/extent-config.xml";
    // ===================== LOG4J CONFIG =====================
    public static final String LOG4J_CONFIG_PATH =
            PROJECT_PATH + "/src/main/resources/log4j2.xml";

    // ===================== RESUME PATH =====================
    public static final String RESUME_PATH =
            PROJECT_PATH + "/src/main/resources/testdata/Sanmeet_Wakchaure_Resume.pdf";

    /**
     * Private constructor to prevent instantiation
     */
    private FrameworkConstants() {
    }

}

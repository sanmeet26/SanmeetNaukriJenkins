package com.framework.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.framework.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : ExtentManager
 * Description :
 * This class is responsible for creating and configuring the Extent Report instance.
 * <p>
 * Responsibilities:
 * - Initialize ExtentReports
 * - Attach reporter (Spark Reporter)
 * - Configure report settings
 * <p>
 * =================================================================================================
 */

public final class ExtentManager {

    private static final Logger logger = LogManager.getLogger();

    private static ExtentReports extent;

    /**
     * Private constructor to prevent instantiation
     */
    private ExtentManager() {
    }

    // ===================== GET INSTANCE =====================

    /**
     * Returns singleton instance of ExtentReports
     *
     * @return ExtentReports instance
     */
    public static ExtentReports getInstance() {

        if (extent == null) {
            logger.info("ExtentReports instance not found. Initializing...");
            extent = createInstance();
        }
        return extent;
    }

    // ===================== CREATE INSTANCE =====================
    private static ExtentReports createInstance() {

        logger.info("Creating ExtentReports instance. Report path: {}", FrameworkConstants.EXTENT_REPORT_PATH);

        ExtentSparkReporter spark =
                new ExtentSparkReporter(FrameworkConstants.EXTENT_REPORT_PATH);

        // Report UI Config
        spark.config().setReportName("Automation Test Report");
        spark.config().setDocumentTitle("Test Execution Report");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        // System Info (shown in report)
        extent.setSystemInfo("Author", "Harshal Thitame");
        extent.setSystemInfo("Framework", "Selenium Java");
        extent.setSystemInfo("Environment", "QA");

        logger.info("ExtentReports initialized successfully.");

        return extent;
    }
}


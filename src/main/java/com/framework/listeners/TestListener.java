package com.framework.listeners;


import com.aventstack.extentreports.Status;
import com.framework.reports.ExtentManager;
import com.framework.reports.ExtentTestManager;
import com.framework.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAnnotationTransformer;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : TestListener
 * Description :
 * This class implements TestNG listeners to handle test execution events.
 * <p>
 * Features:
 * - Extent Report integration
 * - Screenshot capture on failure
 * - Retry mechanism integration
 * - Log4j logging
 * <p>
 * =================================================================================================
 */

public class TestListener implements ITestListener, IAnnotationTransformer {

    private static final Logger logger = LogManager.getLogger();

    // ===================== TEST START =====================
    @Override
    public void onTestStart(ITestResult result) {

        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();

        logger.info("========== TEST STARTED ==========");
        logger.info("Test     : {}", testName);
        logger.info("Class    : {}", className);

        ExtentTestManager.setTest(
                ExtentManager.getInstance().createTest(testName)
        );

        ExtentTestManager.getTest().log(Status.INFO, "Test Started: " + testName);
    }

    // ===================== TEST SUCCESS =====================
    @Override
    public void onTestSuccess(ITestResult result) {

        String testName = result.getMethod().getMethodName();
        long duration = result.getEndMillis() - result.getStartMillis();

        logger.info("TEST PASSED  : {}", testName);
        logger.info("Duration     : {} ms", duration);
        logger.info("==========================================");

        ExtentTestManager.getTest().log(Status.PASS, "Test Passed");
    }

    // ===================== TEST FAILURE =====================
    @Override
    public void onTestFailure(ITestResult result) {

        String testName = result.getMethod().getMethodName();
        long duration = result.getEndMillis() - result.getStartMillis();
        Throwable cause = result.getThrowable();

        logger.error("TEST FAILED  : {}", testName);
        logger.error("Duration     : {} ms", duration);
        logger.error("Cause        : {}", cause != null ? cause.getMessage() : "Unknown");
        logger.error("Stack Trace  : ", cause);
        logger.error("==========================================");

        ExtentTestManager.getTest().log(Status.FAIL, cause);

        // Capture screenshot
        String screenshotPath = ScreenshotUtil.captureScreenshot(testName);

        try {
            ExtentTestManager.getTest().addScreenCaptureFromPath(screenshotPath);
            logger.info("Screenshot attached: {}", screenshotPath);
        } catch (Exception e) {
            logger.error("Failed to attach screenshot for '{}': {}", testName, e.getMessage());
        }
    }

    // ===================== TEST SKIPPED =====================
    @Override
    public void onTestSkipped(ITestResult result) {

        String testName = result.getMethod().getMethodName();
        Throwable cause = result.getThrowable();

        logger.warn("TEST SKIPPED : {}", testName);
        if (cause != null) {
            logger.warn("Reason       : {}", cause.getMessage());
        }
        logger.warn("==========================================");

        ExtentTestManager.getTest().log(Status.SKIP, "Test Skipped");
    }

    // ===================== SUITE START =====================
    @Override
    public void onStart(ITestContext context) {
        logger.info("========== SUITE STARTED ==========");
        logger.info("Suite : {}", context.getName());
        logger.info("Total Tests Scheduled: {}", context.getAllTestMethods().length);
    }

    // ===================== SUITE END =====================
    @Override
    public void onFinish(ITestContext context) {

        int passed  = context.getPassedTests().size();
        int failed  = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();

        logger.info("========== SUITE FINISHED ==========");
        logger.info("Suite   : {}", context.getName());
        logger.info("Passed  : {}", passed);
        logger.info("Failed  : {}", failed);
        logger.info("Skipped : {}", skipped);
        logger.info("====================================");

        // Flush report
        ExtentManager.getInstance().flush();

        // Cleanup ThreadLocal
        ExtentTestManager.unload();
    }

    // ===================== RETRY MECHANISM =====================

    /**
     * Automatically attaches RetryAnalyzer to all test methods
     */
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {

        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}


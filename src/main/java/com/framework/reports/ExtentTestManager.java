package com.framework.reports;

import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : ExtentTestManager
 * Description :
 * This class manages ExtentTest instances using ThreadLocal.
 * <p>
 * Why ThreadLocal?
 * - Each test thread gets its own report instance
 * - Prevents log mixing in parallel execution
 * <p>
 * Responsibilities:
 * - Create test
 * - Get current test
 * - Remove test after execution
 * <p>
 * =================================================================================================
 */

public final class ExtentTestManager {

    private static final Logger logger = LogManager.getLogger();

    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation
     */
    private ExtentTestManager() {
    }

    // ===================== GET TEST =====================
    public static ExtentTest getTest() {
        ExtentTest test = extentTest.get();
        if (test == null) {
            logger.warn("getTest() called but no ExtentTest found for thread '{}'", Thread.currentThread().getName());
        }
        return test;
    }

    // ===================== SET TEST =====================
    public static void setTest(ExtentTest test) {
        logger.debug("Setting ExtentTest for thread '{}': {}", Thread.currentThread().getName(), test.getModel().getName());
        extentTest.set(test);
    }

    // ===================== UNLOAD =====================
    public static void unload() {
        logger.debug("Unloading ExtentTest from thread '{}'", Thread.currentThread().getName());
        extentTest.remove();
    }
}


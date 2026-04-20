package com.framework.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : DriverManager
 * Description :
 * This class is responsible for managing WebDriver instances using ThreadLocal.
 * It ensures that each test thread gets its own WebDriver instance, enabling
 * safe parallel execution without conflicts.
 * <p>
 * Why ThreadLocal?
 * - Prevents WebDriver from being shared across threads
 * - Avoids session conflicts
 * - Enables parallel execution in TestNG
 * <p>
 * Key Responsibilities:
 * - Set driver instance
 * - Get driver instance
 * - Remove driver instance after execution
 * <p>
 * Note:
 * - This class should NOT be instantiated
 * =================================================================================================
 */

public final class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    // ThreadLocal variable to store WebDriver instances per thread
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation
     */
    private DriverManager() {
    }

    // ===================== SET DRIVER =====================

    /**
     * Gets the WebDriver instance from ThreadLocal
     *
     * @return WebDriver instance for current thread
     */
    public static WebDriver getDriver() {
        log.debug("Retrieving WebDriver for thread: {}", Thread.currentThread().getName());
        return driverThreadLocal.get();
    }

    // ===================== GET DRIVER =====================

    /**
     * Sets the WebDriver instance to ThreadLocal
     *
     * @param driver WebDriver instance
     */
    public static void setDriver(WebDriver driver) {
        log.info("Setting WebDriver for thread: {}", Thread.currentThread().getName());
        driverThreadLocal.set(driver);
    }

    // ===================== UNLOAD DRIVER =====================

    /**
     * Removes the WebDriver instance from ThreadLocal
     * This is very important to avoid memory leaks
     */
    public static void unload() {
        log.info("Unloading WebDriver from ThreadLocal for thread: {}", Thread.currentThread().getName());
        driverThreadLocal.remove();
    }
}

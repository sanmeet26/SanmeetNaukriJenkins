package base;

import com.framework.driver.DriverFactory;
import com.framework.driver.DriverManager;
import com.framework.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : BaseTest
 * Description :
 * Abstract base class that every test class in this framework must extend.
 * <p>
 * Responsibilities:
 * - Bootstraps a fresh WebDriver instance before each test method via {@link DriverFactory#initDriver()}.
 * - Navigates to the application URL defined in config.properties immediately after driver creation.
 * - Tears down the WebDriver session after each test method, preventing browser/memory leaks.
 * - Provides thread-safe driver lifecycle management through {@link DriverManager} (ThreadLocal),
 * enabling safe parallel test execution in TestNG.
 * <p>
 * Execution Flow:
 * {@code @BeforeMethod} → initDriver() → navigate to URL
 * {@code @AfterMethod}  → quitDriver() → ThreadLocal cleanup
 * <p>
 * Usage:
 * Any test class that requires a browser session should extend BaseTest:
 * <pre>
 *     public class LoginTest extends BaseTest {
 *         {@literal @}Test
 *         public void testLogin() { ... }
 *     }
 * </pre>
 * <p>
 * Configuration:
 * - {@code browser} key in config.properties controls which browser is launched.
 * - {@code url} key in config.properties controls the application entry point.
 * =================================================================================================
 */
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    // ===================== SETUP =====================

    /**
     * Executed by TestNG before each individual test method.
     * <p>
     * Steps performed:
     * 1. Initialises a new WebDriver instance for the configured browser and binds it
     * to the current thread via {@link DriverFactory#initDriver()}.
     * 2. Reads the application URL from {@code config.properties} and navigates to it,
     * ensuring every test starts from a clean, known entry point.
     * <p>
     * If driver initialisation fails (e.g. unsupported browser, missing driver binary),
     * a {@link RuntimeException} is propagated and the test is marked as failed before
     * it even begins.
     */
    @BeforeMethod
    public void setUp() {
        log.info("===== Test Setup Started | Thread: {} =====", Thread.currentThread().getName());

        log.info("Initializing WebDriver...");
        DriverFactory.initDriver();
// In BaseTest.java setUp()
        DriverManager.getDriver().manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
// Change from default 300s to 60s so it fails fast with clear error
        String url = ConfigReader.get("url");
        log.info("Navigating to application URL: {}", url);
        DriverManager.getDriver().get(url);

        log.info("===== Test Setup Completed =====");
    }

    // ===================== TEARDOWN =====================

    /**
     * Executed by TestNG after each individual test method, regardless of pass or fail.
     * <p>
     * Steps performed:
     * 1. Calls {@link DriverFactory#quitDriver()} which invokes {@code WebDriver.quit()}
     * to close all browser windows and terminate the WebDriver process.
     * 2. Removes the WebDriver instance from {@link DriverManager}'s ThreadLocal storage
     * to prevent memory leaks, especially critical during parallel test runs.
     * <p>
     * If no WebDriver is found for the current thread (e.g. setUp failed), the call is
     * a safe no-op — a warning is logged by {@link DriverFactory} and no exception is thrown.
     */
    @AfterMethod
    public void tearDown() {
        log.info("===== Test Teardown Started | Thread: {} =====", Thread.currentThread().getName());

        log.info("Quitting WebDriver and cleaning up ThreadLocal...");
        DriverFactory.quitDriver();

        log.info("===== Test Teardown Completed =====");
    }
}

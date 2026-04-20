package com.framework.driver;


import com.framework.constants.FrameworkConstants;
import com.framework.enums.BrowserType;
import com.framework.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : DriverFactory
 * Description :
 * This class is responsible for creating and initializing WebDriver instances
 * based on the browser type provided in configuration.
 * <p>
 * Responsibilities:
 * - Read browser from config
 * - Initialize corresponding WebDriver
 * - Apply browser-specific options
 * - Store driver using DriverManager (ThreadLocal)
 * <p>
 * Supported Browsers:
 * - Chrome
 * - Edge
 * - Firefox
 * <p>
 * =================================================================================================
 */

public final class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    // Script to remove the 'webdriver' property that bots detectors look for
    private static final String REMOVE_WEBDRIVER_SCRIPT =
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})";

    /**
     * Private constructor to prevent instantiation of the DriverFactory class.
     */
    private DriverFactory() {
    }

    // ===================== INIT DRIVER =====================

    /**
     * Initializes the WebDriver instance based on the browser specified in the configuration file.
     */
    public static void initDriver() {

        String browserValue = ConfigReader.get("browser");
        log.info("Initializing WebDriver for browser: {}", browserValue);
        BrowserType browser = BrowserType.valueOf(browserValue.toUpperCase());

        WebDriver driver;

        switch (browser) {

            case CHROME:
                log.debug("Configuring ChromeOptions");
                ChromeOptions chromeOptions = getChromeOptions();

                ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);

                // ✅ Remove 'navigator.webdriver' flag via CDP to bypass bot detection (e.g., Akamai)
                chromeDriver.executeCdpCommand(
                        "Page.addScriptToEvaluateOnNewDocument",
                        Map.of("source", REMOVE_WEBDRIVER_SCRIPT)
                );
                log.info("CDP script injected to mask WebDriver flag");

                driver = chromeDriver;
                log.info("ChromeDriver initialized successfully");
                break;

            case EDGE:
                log.debug("Configuring EdgeOptions");
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");

                driver = new EdgeDriver(edgeOptions);
                log.info("EdgeDriver initialized successfully");
                break;

            case FIREFOX:
                System.setProperty("webdriver.gecko.driver",
                        FrameworkConstants.GECKO_DRIVER_PATH);

                log.debug("Configuring FirefoxOptions");
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--width=1920");
                firefoxOptions.addArguments("--height=1080");

                driver = new FirefoxDriver(firefoxOptions);
                log.info("FirefoxDriver initialized successfully");
                break;

            default:
                log.error("Invalid browser type specified: {}", browserValue);
                throw new RuntimeException("Invalid Browser: " + browserValue);
        }

        // Store driver in ThreadLocal
        DriverManager.setDriver(driver);
    }

    /**
     * Builds ChromeOptions with anti-bot-detection settings.
     * <p>
     * Key configurations:
     * - Disables 'AutomationControlled' feature flag exposed to websites
     * - Removes 'enable-automation' switch from Chrome info bar
     * - Disables the automation extension
     * - Sets a realistic User-Agent to avoid headless detection
     * - Uses '--headless=new' (Chrome 112+) which has better parity with real browser
     */
    private static @NonNull ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
// Memory saving flags for low-RAM server

        // ✅ Core anti-detection flags
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--lang=en-US,en");

        // ✅ Remove "Chrome is being controlled by automated software" banner
        chromeOptions.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

        // ✅ Disable Chrome automation extension
        chromeOptions.setExperimentalOption("useAutomationExtension", false);

        // ✅ Set realistic User-Agent (avoids "HeadlessChrome" in UA string)
        chromeOptions.addArguments(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/123.0.0.0 Safari/537.36"
        );

        // ✅ Set prefs to mimic a real user profile
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        chromeOptions.setExperimentalOption("prefs", prefs);

        chromeOptions.addArguments("--single-process");
        chromeOptions.addArguments("--memory-pressure-off");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-background-networking");

        // ---- Headless config (toggle via config if needed) ----
        boolean isHeadless = Boolean.parseBoolean(ConfigReader.get("headless"));
        if (isHeadless) {
            log.info("Running Chrome in headless mode");
            // ✅ Use '--headless=new' — significantly harder to detect than legacy '--headless'
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--window-size=1920,1080");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");

//            chromeOptions.addArguments("--disable-gpu");
        } else {
            chromeOptions.addArguments("--start-maximized");
        }

        return chromeOptions;
    }

    // ===================== QUIT DRIVER =====================

    /**
     * Quits the WebDriver instance and removes it from the ThreadLocal storage.
     */
    public static void quitDriver() {
        if (DriverManager.getDriver() != null) {
            log.info("Quitting WebDriver for thread: {}", Thread.currentThread().getName());
            DriverManager.getDriver().quit();
            DriverManager.unload();
            log.info("WebDriver quit and unloaded successfully");
        } else {
            log.warn("quitDriver called but no WebDriver found for thread: {}", Thread.currentThread().getName());
        }
    }
}
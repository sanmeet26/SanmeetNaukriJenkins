package com.framework.utils;

import com.framework.constants.FrameworkConstants;
import com.framework.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : ScreenshotUtil
 * Description :
 * Utility class to capture screenshots during test execution.
 * <p>
 * Key Features:
 * - Captures screenshot on failure
 * - Saves with unique timestamp
 * - Returns file path for reporting (Extent Reports)
 * <p>
 * Usage:
 * String path = ScreenshotUtil.captureScreenshot("LoginTest");
 * <p>
 * =================================================================================================
 */

public final class ScreenshotUtil {

    /**
     * Private constructor to prevent instantiation
     */
    private ScreenshotUtil() {
    }

    // ===================== CAPTURE SCREENSHOT =====================

    /**
     * Captures screenshot and saves to screenshots folder
     *
     * @param testName name of test
     * @return screenshot file path
     */
    public static String captureScreenshot(String testName) {

        // Generate unique file name using timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = testName + "_" + timestamp + ".png";

        String filePath = FrameworkConstants.SCREENSHOT_PATH + fileName;

        try {
            // Take screenshot
            TakesScreenshot ts = (TakesScreenshot) DriverManager.getDriver();
            byte[] source = ts.getScreenshotAs(OutputType.BYTES);

            // Create directories if not exist
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            // Save file
            Files.write(path, source);

        } catch (IOException e) {
            throw new RuntimeException("Failed to capture screenshot: " + e.getMessage());
        }

        return filePath;
    }
}

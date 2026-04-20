package com.framework.utils;

import com.framework.constants.FrameworkConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : ConfigReader
 * Description :
 * This class reads configuration values from config.properties file.
 * <p>
 * Responsibilities:
 * - Load properties file
 * - Provide key-value access across a framework
 * <p>
 * Example:
 * browser=chrome
 * url=<a href="https://example.com">...</a>
 * <p>
 * =================================================================================================
 */

public final class ConfigReader {

    private static final Properties properties = new Properties();

    // Static block → executes once when class is loaded
    static {
        try {
            FileInputStream fis = new FileInputStream(FrameworkConstants.CONFIG_FILE_PATH);
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties file");
        }
    }

    /**
     * Private constructor to prevent instantiation
     */
    private ConfigReader() {
    }

    // ===================== GET VALUE =====================

    /**
     * Returns value for given key from config.properties
     *
     * @param key property key
     * @return property value
     */
    public static String get(String key) {
        if (properties.getProperty(key) == null) {
            throw new RuntimeException("Property not found: " + key);
        }
        return properties.getProperty(key).trim();
    }

    /**
     *
     * @param key
     * @return
     */
    public static String getCredential(String key) {
        // First check environment variable (set by Jenkins)
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        System.out.println("=============================================");
        System.out.println(envValue);
        // Fallback to config.properties for local runs
        return properties.getProperty(key);
    }
}

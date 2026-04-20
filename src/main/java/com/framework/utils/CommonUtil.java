package com.framework.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : CommonUtil
 * Description :
 * Contains common reusable utility methods used across framework.
 * <p>
 * =================================================================================================
 */

public final class CommonUtil {

    private static final Random random = new Random();

    /**
     * Private constructor
     */
    private CommonUtil() {
    }

    // ===================== RANDOM STRING =====================
    public static String getRandomString(int length) {

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    // ===================== RANDOM NUMBER =====================
    public static int getRandomNumber(int bound) {
        return random.nextInt(bound);
    }

    // ===================== TIMESTAMP =====================
    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    // ===================== RANDOM EMAIL =====================
    public static String getRandomEmail() {
        return "user" + getTimestamp() + "@test.com";
    }

    // ===================== NULL CHECK =====================
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}

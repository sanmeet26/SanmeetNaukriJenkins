package com.framework.enums;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Enum Name : WaitStrategy
 * Description :
 * Defines different wait strategies to handle WebElement synchronization.
 * <p>
 * Why use this?
 * - Avoid hardcoding wait logic
 * - Cleaner and reusable design
 * - Central control of wait behavior
 * <p>
 * =================================================================================================
 */

public enum WaitStrategy {

    CLICKABLE,
    VISIBLE,
    PRESENCE,
    NONE

}

package com.framework.utils;

import com.framework.constants.FrameworkConstants;
import com.framework.driver.DriverManager;
import com.framework.enums.WaitStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * @author Harshal.Thitame
 * @implNote <p>
 * Class Name : WaitUtil
 * <p>
 * Description :<br>
 * Industry-grade utility class for all Selenium wait operations.
 * <p>
 * Strategies Supported:<br>
 * - VISIBLE    : Element is present in DOM and visible on screen<br>
 * - CLICKABLE  : Element is visible and enabled (ready for interaction)<br>
 * - PRESENCE   : Element is present in DOM (may not be visible)<br>
 * - NONE       : No wait applied — direct findElement<br>
 * <p>
 * Wait Types Covered:<br>
 * - Explicit wait   (WebDriverWait)<br>
 * - Fluent wait     (custom polling interval + ignored exceptions)<br>
 * - Alert wait<br>
 * - Frame wait<br>
 * - URL / title wait<br>
 * - Staleness wait<br>
 * - Attribute / text condition waits<br>
 * - JavaScript ready-state wait<br>
 * - Custom condition wait<br>
 * <p>
 * Usage:<br>
 * WebElement el = WaitUtil.waitForElement(By.id("username"), WaitStrategy.VISIBLE);<br>
 * WaitUtil.waitForPageLoad();<br>
 * WaitUtil.waitForTextToBePresentInElement(By.id("msg"), "Success", WaitStrategy.VISIBLE);<br>
 *
 */

public final class WaitUtil {

    private static final Logger logger = LogManager.getLogger();

    private WaitUtil() {
    }

    // =====================================================================
    // REGION: FACTORY HELPERS
    // =====================================================================

    /**
     * Creates a standard WebDriverWait using the default explicit wait timeout.
     */
    private static WebDriverWait getWait() {
        return new WebDriverWait(
                DriverManager.getDriver(),
                Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
        );
    }

    /**
     * Creates a WebDriverWait with a custom timeout.
     */
    private static WebDriverWait getWait(int timeoutSeconds) {
        return new WebDriverWait(
                DriverManager.getDriver(),
                Duration.ofSeconds(timeoutSeconds)
        );
    }

    /**
     * Creates a FluentWait with custom timeout, polling interval,
     * ignoring NoSuchElementException and StaleElementReferenceException.
     */
    private static FluentWait<WebDriver> getFluentWait(int timeoutSeconds, int pollingMillis) {
        return new FluentWait<>(DriverManager.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    // =====================================================================
    // REGION: CORE — waitForElement
    // =====================================================================

    /**
     * Waits for a WebElement using the specified strategy and default timeout.
     *
     * @param by       locator
     * @param strategy wait strategy
     * @return WebElement
     */
    public static WebElement waitForElement(By by, WaitStrategy strategy) {
        return waitForElement(by, strategy, FrameworkConstants.EXPLICIT_WAIT);
    }

    /**
     * Waits for a WebElement using the specified strategy and a custom timeout.
     *
     * @param by             locator
     * @param strategy       wait strategy
     * @param timeoutSeconds custom timeout in seconds
     * @return WebElement
     */
    public static WebElement waitForElement(By by, WaitStrategy strategy, int timeoutSeconds) {
        logger.debug("Waiting for element [{}] with strategy [{}], timeout [{}s]", by, strategy, timeoutSeconds);

        WebDriverWait wait = getWait(timeoutSeconds);

        switch (strategy) {

            case CLICKABLE:
                return wait.until(ExpectedConditions.elementToBeClickable(by));

            case VISIBLE:
                return wait.until(ExpectedConditions.visibilityOfElementLocated(by));

            case PRESENCE:
                return wait.until(ExpectedConditions.presenceOfElementLocated(by));

            case NONE:
                return DriverManager.getDriver().findElement(by);

            default:
                throw new IllegalArgumentException("Unsupported WaitStrategy: " + strategy);
        }
    }

    /**
     * Waits for a WebElement already found (WebElement overload) using the specified strategy.
     *
     * @param element  pre-located WebElement
     * @param strategy wait strategy (VISIBLE or CLICKABLE)
     * @return WebElement
     */
    public static WebElement waitForElement(WebElement element, WaitStrategy strategy) {
        logger.debug("Waiting for WebElement with strategy [{}]", strategy);

        WebDriverWait wait = getWait();

        switch (strategy) {
            case CLICKABLE:
                return wait.until(ExpectedConditions.elementToBeClickable(element));
            case VISIBLE:
                return wait.until(ExpectedConditions.visibilityOf(element));
            default:
                return element;
        }
    }

    // =====================================================================
    // REGION: MULTIPLE ELEMENTS
    // =====================================================================

    /**
     * Waits until all elements matching the locator are visible.
     *
     * @param by locator
     * @return list of visible WebElements
     */
    public static List<WebElement> waitForAllElements(By by) {
        logger.debug("Waiting for all elements to be visible: [{}]", by);
        return getWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(by));
    }

    /**
     * Waits until at least one element matching the locator is present in DOM.
     *
     * @param by locator
     * @return list of present WebElements
     */
    public static List<WebElement> waitForPresenceOfAllElements(By by) {
        logger.debug("Waiting for presence of all elements: [{}]", by);
        return getWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
    }

    // =====================================================================
    // REGION: FLUENT WAIT
    // =====================================================================

    /**
     * Waits for an element using FluentWait with custom timeout and polling.
     * Useful when elements appear intermittently.
     *
     * @param by             locator
     * @param timeoutSeconds total wait time
     * @param pollingMillis  how often to check
     * @return WebElement when found
     */
    public static WebElement fluentWaitForElement(By by, int timeoutSeconds, int pollingMillis) {
        logger.debug("FluentWait for [{}] — timeout: {}s, polling: {}ms", by, timeoutSeconds, pollingMillis);
        return getFluentWait(timeoutSeconds, pollingMillis)
                .until(driver -> driver.findElement(by));
    }

    /**
     * Waits for an element to be visible using FluentWait.
     *
     * @param by             locator
     * @param timeoutSeconds total wait time
     * @param pollingMillis  how often to check
     * @return visible WebElement
     */
    public static WebElement fluentWaitForVisibility(By by, int timeoutSeconds, int pollingMillis) {
        logger.debug("FluentWait for visibility [{}] — timeout: {}s, polling: {}ms", by, timeoutSeconds, pollingMillis);
        return getFluentWait(timeoutSeconds, pollingMillis)
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    // =====================================================================
    // REGION: VISIBILITY / INVISIBILITY
    // =====================================================================

    /**
     * Waits until the element is invisible or removed from the DOM.
     *
     * @param by locator
     * @return true when element is invisible
     */
    public static boolean waitForInvisibility(By by) {
        logger.debug("Waiting for element to be invisible: [{}]", by);
        return getWait().until(ExpectedConditions.invisibilityOfElementLocated(by));
    }

    /**
     * Waits until the element is invisible with a custom timeout.
     *
     * @param by             locator
     * @param timeoutSeconds custom timeout
     * @return true when element is invisible
     */
    public static boolean waitForInvisibility(By by, int timeoutSeconds) {
        logger.debug("Waiting for invisibility [{}] — timeout: {}s", by, timeoutSeconds);
        return getWait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(by));
    }

    /**
     * Waits until an already-found element disappears from view.
     *
     * @param element WebElement to wait on
     * @return true when invisible
     */
    public static boolean waitForInvisibility(WebElement element) {
        logger.debug("Waiting for WebElement to become invisible");
        return getWait().until(ExpectedConditions.invisibilityOf(element));
    }

    // =====================================================================
    // REGION: STALENESS
    // =====================================================================

    /**
     * Waits until the given element becomes stale (detached from DOM).
     * Useful after actions that trigger a page refresh or DOM rebuild.
     *
     * @param element WebElement expected to go stale
     * @return true when stale
     */
    public static boolean waitForStaleness(WebElement element) {
        logger.debug("Waiting for element to become stale");
        return getWait().until(ExpectedConditions.stalenessOf(element));
    }

    // =====================================================================
    // REGION: CLICKABILITY
    // =====================================================================

    /**
     * Waits until the element is clickable (visible + enabled).
     *
     * @param by locator
     * @return clickable WebElement
     */
    public static WebElement waitForClickability(By by) {
        logger.debug("Waiting for element to be clickable: [{}]", by);
        return getWait().until(ExpectedConditions.elementToBeClickable(by));
    }

    /**
     * Waits until the element is clickable with a custom timeout.
     *
     * @param by             locator
     * @param timeoutSeconds custom timeout
     * @return clickable WebElement
     */
    public static WebElement waitForClickability(By by, int timeoutSeconds) {
        logger.debug("Waiting for clickability [{}] — timeout: {}s", by, timeoutSeconds);
        return getWait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(by));
    }

    // =====================================================================
    // REGION: TEXT CONDITIONS
    // =====================================================================

    /**
     * Waits until the element's visible text contains the expected value.
     *
     * @param by       locator
     * @param text     expected text substring
     * @param strategy wait strategy to locate the element first
     * @return true when text is present
     */
    public static boolean waitForTextToBePresentInElement(By by, String text, WaitStrategy strategy) {
        logger.debug("Waiting for text '{}' in element [{}]", text, by);
        waitForElement(by, strategy);
        return getWait().until(ExpectedConditions.textToBePresentInElementLocated(by, text));
    }

    /**
     * Waits until the element's value attribute contains the expected text.
     * Useful for input fields where text() is empty but value= is set.
     *
     * @param by   locator
     * @param text expected value text
     * @return true when value text is present
     */
    public static boolean waitForTextInValue(By by, String text) {
        logger.debug("Waiting for text '{}' in value attribute of [{}]", text, by);
        return getWait().until(ExpectedConditions.textToBePresentInElementValue(by, text));
    }

    /**
     * Waits until the element's text exactly matches the expected value.
     *
     * @param by           locator
     * @param expectedText exact text to match
     * @return true when matched
     */
    public static boolean waitForExactText(By by, String expectedText) {
        logger.debug("Waiting for exact text '{}' in element [{}]", expectedText, by);
        return getWait().until(driver -> {
            String actual = driver.findElement(by).getText().trim();
            return actual.equals(expectedText);
        });
    }

    // =====================================================================
    // REGION: ATTRIBUTE CONDITIONS
    // =====================================================================

    /**
     * Waits until the specified attribute of an element contains the expected value.
     *
     * @param by        locator
     * @param attribute attribute name (e.g. "class", "href", "value")
     * @param value     expected value substring
     * @return true when condition is met
     */
    public static boolean waitForAttributeContains(By by, String attribute, String value) {
        logger.debug("Waiting for attribute '{}' to contain '{}' on element [{}]", attribute, value, by);
        return getWait().until(ExpectedConditions.attributeContains(by, attribute, value));
    }

    /**
     * Waits until the specified attribute of an element is not null and not empty.
     *
     * @param by        locator
     * @param attribute attribute name
     * @return true when attribute has a value
     */
    public static boolean waitForAttributeToBeNotEmpty(By by, String attribute) {
        logger.debug("Waiting for attribute '{}' to be non-empty on element [{}]", attribute, by);
        WebElement element = waitForElement(by, WaitStrategy.PRESENCE);
        return getWait().until(ExpectedConditions.attributeToBeNotEmpty(element, attribute));
    }

    // =====================================================================
    // REGION: SELECTION STATE
    // =====================================================================

    /**
     * Waits until a checkbox or radio button is selected.
     *
     * @param by locator
     * @return true when selected
     */
    public static boolean waitForElementToBeSelected(By by) {
        logger.debug("Waiting for element to be selected: [{}]", by);
        return getWait().until(ExpectedConditions.elementToBeSelected(by));
    }

    // =====================================================================
    // REGION: PAGE LOAD & JAVASCRIPT READY STATE
    // =====================================================================

    /**
     * Waits until the browser's document.readyState equals "complete".
     * Handles AJAX-heavy pages that don't trigger standard Selenium page events.
     */
    public static void waitForPageLoad() {
        logger.info("Waiting for page to fully load (document.readyState == complete)");
        getWait(FrameworkConstants.PAGE_LOAD_TIMEOUT).until(driver ->
                ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete")
        );
        logger.info("Page load complete.");
    }

    /**
     * Waits until jQuery AJAX calls are finished (if jQuery is present on the page).
     * Combine with waitForPageLoad() for SPA/AJAX-heavy apps.
     */
    public static void waitForJQueryLoad() {
        logger.info("Waiting for jQuery AJAX to complete");
        getWait().until(driver -> {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Boolean jQueryDefined = (Boolean) js.executeScript("return typeof jQuery !== 'undefined'");
            if (!jQueryDefined) {
                logger.warn("jQuery is not defined on this page — skipping jQuery wait.");
                return true;
            }
            return (Boolean) js.executeScript("return jQuery.active == 0");
        });
    }

    /**
     * Waits until Angular $http / $timeout calls settle (Angular 1.x).
     */
    public static void waitForAngularLoad() {
        logger.info("Waiting for Angular to settle");
        getWait().until(driver ->
                (Boolean) ((JavascriptExecutor) driver)
                        .executeScript("return angular.element(document).injector()" +
                                ".get('$http').pendingRequests.length === 0")
        );
    }

    // =====================================================================
    // REGION: URL & TITLE CONDITIONS
    // =====================================================================

    /**
     * Waits until the browser URL contains the expected fragment.
     *
     * @param urlFragment partial URL string to match
     */
    public static void waitForUrlContains(String urlFragment) {
        logger.debug("Waiting for URL to contain: '{}'", urlFragment);
        getWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Waits until the browser URL matches the expected URL exactly.
     *
     * @param expectedUrl full URL to match
     */
    public static void waitForUrlToBe(String expectedUrl) {
        logger.debug("Waiting for URL to be: '{}'", expectedUrl);
        getWait().until(ExpectedConditions.urlToBe(expectedUrl));
    }

    /**
     * Waits until the page title contains the expected text.
     *
     * @param titleFragment partial title to match
     */
    public static void waitForTitleContains(String titleFragment) {
        logger.debug("Waiting for title to contain: '{}'", titleFragment);
        getWait().until(ExpectedConditions.titleContains(titleFragment));
    }

    /**
     * Waits until the page title exactly matches the expected value.
     *
     * @param expectedTitle exact title to match
     */
    public static void waitForTitleToBe(String expectedTitle) {
        logger.debug("Waiting for title to be: '{}'", expectedTitle);
        getWait().until(ExpectedConditions.titleIs(expectedTitle));
    }

    // =====================================================================
    // REGION: FRAME / WINDOW
    // =====================================================================

    /**
     * Waits until the frame identified by a locator is available and switches to it.
     *
     * @param by locator for the frame element
     * @return WebDriver focused on the frame
     */
    public static WebDriver waitAndSwitchToFrame(By by) {
        logger.debug("Waiting for frame and switching: [{}]", by);
        return getWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(by));
    }

    /**
     * Waits until the frame identified by its index is available and switches to it.
     *
     * @param frameIndex zero-based frame index
     * @return WebDriver focused on the frame
     */
    public static WebDriver waitAndSwitchToFrame(int frameIndex) {
        logger.debug("Waiting for frame by index [{}] and switching", frameIndex);
        return getWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameIndex));
    }

    /**
     * Waits until the frame identified by its name or ID is available and switches to it.
     *
     * @param nameOrId frame name or ID attribute
     * @return WebDriver focused on the frame
     */
    public static WebDriver waitAndSwitchToFrame(String nameOrId) {
        logger.debug("Waiting for frame '{}' and switching", nameOrId);
        return getWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    /**
     * Waits until the number of open browser windows/tabs reaches the expected count.
     *
     * @param expectedCount expected window handle count
     */
    public static void waitForNumberOfWindows(int expectedCount) {
        logger.debug("Waiting for {} browser window(s) to be open", expectedCount);
        getWait().until(ExpectedConditions.numberOfWindowsToBe(expectedCount));
    }

    // =====================================================================
    // REGION: ALERT
    // =====================================================================

    /**
     * Waits for a JavaScript alert/confirm/prompt to be present and returns it.
     *
     * @return the Alert object
     */
    public static Alert waitForAlert() {
        logger.debug("Waiting for alert to be present");
        return getWait().until(ExpectedConditions.alertIsPresent());
    }

    /**
     * Waits for an alert to appear, accepts it, and returns the text.
     *
     * @return alert text
     */
    public static String acceptAlert() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        logger.info("Alert accepted. Text: '{}'", text);
        alert.accept();
        return text;
    }

    /**
     * Waits for an alert to appear, dismisses it, and returns the text.
     *
     * @return alert text
     */
    public static String dismissAlert() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        logger.info("Alert dismissed. Text: '{}'", text);
        alert.dismiss();
        return text;
    }

    // =====================================================================
    // REGION: ELEMENT COUNT
    // =====================================================================

    /**
     * Waits until the number of elements matching the locator equals the expected count.
     *
     * @param by            locator
     * @param expectedCount expected number of elements
     * @return list of WebElements
     */
    public static List<WebElement> waitForElementCount(By by, int expectedCount) {
        logger.debug("Waiting for exactly {} element(s) matching [{}]", expectedCount, by);
        return getWait().until(ExpectedConditions.numberOfElementsToBe(by, expectedCount));
    }

    /**
     * Waits until at least the given number of elements are present.
     *
     * @param by           locator
     * @param minimumCount minimum count
     * @return list of WebElements
     */
    public static List<WebElement> waitForMinimumElementCount(By by, int minimumCount) {
        logger.debug("Waiting for at least {} element(s) matching [{}]", minimumCount, by);
        return getWait().until(ExpectedConditions.numberOfElementsToBeMoreThan(by, minimumCount - 1));
    }

    // =====================================================================
    // REGION: CUSTOM CONDITION
    // =====================================================================

    /**
     * Waits for any arbitrary ExpectedCondition using the default timeout.
     * Use this for conditions not covered by other methods.
     *
     * @param condition any ExpectedCondition
     * @param <T>       return type of the condition
     * @return result when condition is satisfied
     */
    public static <T> T waitFor(ExpectedCondition<T> condition) {
        logger.debug("Waiting for custom ExpectedCondition");
        return getWait().until(condition);
    }

    /**
     * Waits for any arbitrary lambda/Function condition using FluentWait.
     *
     * @param condition      function returning a non-null, non-false value when satisfied
     * @param timeoutSeconds custom timeout
     * @param pollingMillis  polling interval
     * @param <T>            return type
     * @return result when condition is satisfied
     */
    public static <T> T waitFor(Function<WebDriver, T> condition, int timeoutSeconds, int pollingMillis) {
        logger.debug("Waiting for custom condition — timeout: {}s, polling: {}ms", timeoutSeconds, pollingMillis);
        return getFluentWait(timeoutSeconds, pollingMillis).until(condition);
    }

    // =====================================================================
    // REGION: SAFE CHECKS (no exception — returns boolean)
    // =====================================================================

    /**
     * Checks if an element is currently visible without throwing an exception.
     *
     * @param by locator
     * @return true if visible, false otherwise
     */
    public static boolean isVisible(By by) {
        try {
            return waitForElement(by, WaitStrategy.VISIBLE).isDisplayed();
        } catch (Exception e) {
            logger.debug("isVisible check failed for [{}]: {}", by, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an element is present in the DOM without throwing an exception.
     *
     * @param by locator
     * @return true if present, false otherwise
     */
    public static boolean isPresent(By by) {
        try {
            waitForElement(by, WaitStrategy.PRESENCE);
            return true;
        } catch (Exception e) {
            logger.debug("isPresent check failed for [{}]: {}", by, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an element is clickable without throwing an exception.
     *
     * @param by locator
     * @return true if clickable, false otherwise
     */
    public static boolean isClickable(By by) {
        try {
            waitForElement(by, WaitStrategy.CLICKABLE);
            return true;
        } catch (Exception e) {
            logger.debug("isClickable check failed for [{}]: {}", by, e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // REGION: INTERACTION HELPERS
    // =====================================================================

    /**
     * Clicks the element after waiting for it to be clickable.
     *
     * @param by       locator
     * @param strategy wait strategy
     */
    public static void click(By by, WaitStrategy strategy) {
        logger.debug("Clicking element [{}]", by);
        waitForElement(by, strategy).click();
    }

    /**
     * Clears and types text into an input field after waiting for it.
     *
     * @param by       locator
     * @param value    text to enter
     * @param strategy wait strategy
     */
    public static void sendKeys(By by, String value, WaitStrategy strategy) {
        logger.debug("Sending keys '{}' to element [{}]", value, by);
        WebElement element = waitForElement(by, strategy);
        element.clear();
        element.sendKeys(value);
    }

    /**
     * Gets visible text from the element after waiting for it.
     *
     * @param by       locator
     * @param strategy wait strategy
     * @return trimmed text content
     */
    public static String getText(By by, WaitStrategy strategy) {
        String text = waitForElement(by, strategy).getText().trim();
        logger.debug("Got text '{}' from element [{}]", text, by);
        return text;
    }

    /**
     * Gets the value of an attribute from the element after waiting for it.
     *
     * @param by        locator
     * @param attribute attribute name
     * @param strategy  wait strategy
     * @return attribute value
     */
    public static String getAttribute(By by, String attribute, WaitStrategy strategy) {
        String value = waitForElement(by, strategy).getAttribute(attribute);
        logger.debug("Got attribute '{}' = '{}' from element [{}]", attribute, value, by);
        return value;
    }

    /**
     * Returns true if the element is currently checked/selected.
     *
     * @param by       locator
     * @param strategy wait strategy
     * @return true if selected
     */
    public static boolean isSelected(By by, WaitStrategy strategy) {
        return waitForElement(by, strategy).isSelected();
    }

    /**
     * Returns true if the element is enabled.
     *
     * @param by       locator
     * @param strategy wait strategy
     * @return true if enabled
     */
    public static boolean isEnabled(By by, WaitStrategy strategy) {
        return waitForElement(by, strategy).isEnabled();
    }
}

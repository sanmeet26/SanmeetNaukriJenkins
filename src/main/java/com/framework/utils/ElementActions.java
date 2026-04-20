package com.framework.utils;

import com.framework.constants.FrameworkConstants;
import com.framework.driver.DriverManager;
import com.framework.enums.WaitStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Harshal.Thitame
 * @implNote Provides utility methods for performing various web element actions
 * in Selenium-based automation frameworks.
 * The methods in this class support multiple fallback strategies and
 * employ retry mechanisms to handle transient issues during web interaction.
 */
public final class ElementActions {

    private static final Logger log = LogManager.getLogger();

    private ElementActions() {
    }

    /**
     * Performs a click action on the specified web element using a predefined wait strategy.
     * This method attempts multiple fallback strategies if the initial click fails, including:
     * 1. Scrolling to the element and retrying the click.
     * 2. Using the Actions class to perform the click.
     * 3. Executing a JavaScript click.
     *
     * @param locator  the location of the web element to be clicked
     * @param strategy the wait strategy to synchronize the web element before clicking
     * @throws RuntimeException if all click attempts fail after multiple retries
     */
    public static void click(By locator, WaitStrategy strategy) {

        WebElement element = WaitUtil.waitForElement(locator, strategy);

        // Retry mechanism
        int attempts = 0;
        int maxAttempts = 3;

        while (attempts < maxAttempts) {
            try {
                element.click();
                log.info("Normal click successful: {}", locator);
                return;

            } catch (ElementNotInteractableException e) {
                log.warn("Normal click failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 1️⃣ Scroll into view and retry
                    scrollToElement(locator);
                    element.click();
                    log.info("Click after scroll successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Scroll click failed: {}", locator);

                    try {
                        // 2️⃣ Actions class click
                        new Actions(DriverManager.getDriver())
                                .moveToElement(element)
                                .click()
                                .perform();

                        log.info("Actions click successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.warn("Actions click failed: {}", locator);

                        try {
                            // 3️⃣ JS click
                            jsClick(locator);
                            log.info("JS click successful: {}", locator);
                            return;

                        } catch (Exception ex3) {
                            log.error("All click strategies failed (attempt {}): {}", attempts + 1, locator);
                        }
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, re-locating: {}", locator);
                element = WaitUtil.waitForElement(locator, strategy);

            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to click element after all attempts: " + locator);
    }

    /**
     * Performs a JavaScript click on the web element identified by the specified locator.
     * The method retries the operation up to three times in case of failures due to
     * stale element reference or other exceptions.
     *
     * @param locator the locator used to identify the web element to be clicked.
     *                Examples include By.id, By.xpath, By.cssSelector, etc.
     * @throws RuntimeException if the JavaScript click fails after three retries.
     */
    public static void jsClick(By locator) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);

                JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

                js.executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});",
                        element
                );

                js.executeScript("arguments[0].click();", element);

                log.info("JS Click success on attempt {}: {}", attempts + 1, locator);
                return;

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying JS click due to stale element: {}", locator);

            } catch (Exception e) {
                log.warn("JS click attempt {} failed: {}", attempts + 1, locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ JS Click failed after retries: " + locator);
    }

    /**
     * Scrolls the page to bring the specified web element into view.
     * <p>
     * Handles lazy-loaded pages by first scrolling the page progressively in chunks
     * to trigger DOM rendering of off-screen sections. Once the element appears in
     * the DOM, an explicit wait confirms its presence before a precise
     * {@code scrollIntoView} centres it in the viewport.
     * <p>
     * Steps:
     * 1. If the element is already in the DOM, skip progressive scroll.
     * 2. Otherwise, scroll down in 300px increments and check after each step.
     * 3. Wait up to 10 seconds for the element to be present in the DOM.
     * 4. Use JavaScript {@code scrollIntoView({block:'center'})} for final positioning.
     *
     * @param locator the {@link By} locator of the target element
     * @throws RuntimeException if the element is not found after full page scroll or scroll fails
     */
    public static void scrollToElement(By locator) {
        try {
            WebDriver driver = DriverManager.getDriver();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Step 1: Check if element is already in DOM; if not, trigger lazy loading
            // by scrolling progressively down the page in 300px chunks
            try {
                driver.findElement(locator);
                log.debug("Element already present in DOM, skipping progressive scroll: {}", locator);
            } catch (NoSuchElementException e) {
                log.warn("Element not in DOM yet — triggering lazy load via progressive scroll: {}", locator);

                long totalHeight = (long) js.executeScript("return document.body.scrollHeight");
                boolean found = false;

                for (long scrollY = 300; scrollY <= totalHeight + 300; scrollY += 300) {
                    js.executeScript("window.scrollTo(0, arguments[0]);", scrollY);
                    try {
                        driver.findElement(locator);
                        log.info("Element appeared in DOM after scrolling to {}px", scrollY);
                        found = true;
                        break;
                    } catch (NoSuchElementException ex) {
                        // not yet — keep scrolling
                    }
                }

                if (!found) {
                    throw new NoSuchElementException("Element did not appear in DOM after full page scroll: " + locator);
                }
            }

            // Step 2: Wait for element presence (handles any brief render delay after scroll)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            // Step 3: Scroll element to centre of viewport
            js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);

            log.info("Scrolled to element (center): {}", locator);

        } catch (Exception e) {
            log.error("Scroll failed: {}", locator, e);
            throw new RuntimeException("Unable to scroll: " + locator);
        }
    }

    /**
     * Sends the specified text value to an input field located by the given locator using multiple fallback strategies.
     * Ensures the operation retries on failure up to three attempts. Supports three methods of input:
     * standard sendKeys, Actions-based typing, and JavaScript value assignment as a last resort.
     *
     * @param locator  The {@code By} locator used to identify the target input field.
     * @param value    The text value to be entered into the input field.
     * @param strategy The {@code WaitStrategy} to use for locating and waiting on the input element's readiness.
     *                 Determines how the wait mechanism is applied (e.g., visible, clickable, etc.).
     * @throws RuntimeException if all attempts to send keys using the available strategies fail.
     */
    public static void sendKeys(By locator, String value, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll to element (center)
                scrollToElement(locator);

                // Clear field properly
                element.clear();

                // Normal sendKeys
                element.sendKeys(value);

                log.info("Text entered '{}' into: {}", value, locator);
                return;

            } catch (InvalidElementStateException e) {
                log.warn("Normal sendKeys failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 1️⃣ Actions-based typing
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    new Actions(DriverManager.getDriver())
                            .moveToElement(element)
                            .click()
                            .sendKeys(value)
                            .perform();

                    log.info("Actions sendKeys successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Actions sendKeys failed: {}", locator);

                    try {
                        // 2️⃣ JS set value (last fallback)
                        jsSendKeys(locator, value);
                        log.info("JS sendKeys successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All sendKeys strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to send keys after retries: " + locator);
    }

    /**
     * Sends text input to a web element located by the specified locator using JavaScript.
     * This method clears any existing value in the input field, sets the provided value,
     * and triggers the appropriate events ('input' and 'change') to simulate user input.
     * <p>
     * If the operation fails due to a stale element reference or other issues,
     * it retries the operation up to 3 times before throwing a RuntimeException.
     *
     * @param locator The {@code By} locator used to find the web element.
     * @param value   The string value to input into the target web element.
     */
    public static void jsSendKeys(By locator, String value) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
                JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

                js.executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});",
                        element
                );

                // Clear existing value
                js.executeScript("arguments[0].value='';", element);

                // Set new value + trigger events
                js.executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                        element, value
                );

                log.info("JS sendKeys success (attempt {}): {}", attempts + 1, locator);
                return;

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying jsSendKeys due to stale element: {}", locator);
            } catch (Exception e) {
                log.warn("JS sendKeys attempt {} failed: {}", attempts + 1, locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ JS sendKeys failed after retries: " + locator);
    }

    /**
     * Retrieves the textual content of a web element located by the specified locator.
     * The method tries multiple strategies to obtain the text, including direct retrieval,
     * checking the value attribute, textContent attribute, and JavaScript-based extraction.
     * If the element's text cannot be retrieved within the allowed retry attempts, an exception is thrown.
     *
     * @param locator  The {@code By} object used to locate the web element.
     * @param strategy The {@code WaitStrategy} specifying the waiting mechanism for the element to be interactable.
     * @return The text content of the located web element as a {@code String}, trimmed of leading and trailing spaces.
     * @throws RuntimeException if the text cannot be retrieved within the maximum retry attempts.
     */
    public static String getText(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                String text = element.getText();

                if (text.trim().isEmpty()) {
                    text = element.getAttribute("value");
                }

                if (text == null || text.trim().isEmpty()) {
                    text = element.getAttribute("textContent");
                }

                if (text == null || text.trim().isEmpty()) {
                    text = getTextUsingJS(locator);
                }

                log.info("Text fetched: {} -> {}", locator, text);
                return text.trim();

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying getText due to stale element: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to get text after retries: " + locator);
    }

    /**
     * Retrieves the visible text of a web element using JavaScript.
     *
     * @param locator the By locator used to identify the web element.
     * @return the trimmed visible text of the web element fetched using JavaScript.
     * @throws RuntimeException if an error occurs while fetching the text or if the text is null.
     */
    public static String getTextUsingJS(By locator) {
        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);

            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            String text = (String) js.executeScript(
                    "return arguments[0].innerText;", element
            );

            log.info("JS fetched text from {}: {}", locator, text);
            assert text != null;
            return text.trim();

        } catch (Exception e) {
            log.error("JS getText failed: {}", locator, e);
            throw new RuntimeException("❌ JS getText failed: " + locator);
        }
    }

    /**
     * Checks if an element located by the given locator is displayed on the webpage.
     *
     * @param locator  The locating mechanism to find the element.
     * @param strategy The wait strategy used before checking the element's visibility.
     * @return {@code true} if the element is displayed, otherwise {@code false}.
     */
    public static boolean isDisplayed(By locator, WaitStrategy strategy) {

        try {
            boolean status = isDisplayedInternal(locator, strategy);

            if (!status) {
                // fallback using JS
                return isDisplayedUsingJS(locator);
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines whether the web element located by the specified locator is displayed on the page,
     * using the provided wait strategy to ensure the element is available.
     *
     * @param locator  the By locator used to identify the web element.
     * @param strategy the wait strategy to apply when locating the element.
     * @return true if the web element is displayed, false otherwise.
     */
    private static boolean isDisplayedInternal(By locator, WaitStrategy strategy) {

        WebElement element = WaitUtil.waitForElement(locator, strategy);
        return element.isDisplayed();
    }

    /**
     * Checks if an element is displayed in the DOM using JavaScript execution.
     *
     * @param locator The {@code By} locator used to identify the web element.
     * @return {@code true} if the element is visible according to the JavaScript evaluation,
     * {@code false} otherwise.
     */
    public static boolean isDisplayedUsingJS(By locator) {
        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);

            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            Object result = js.executeScript(
                    "return arguments[0] != null && arguments[0].offsetParent !== null;",
                    element
            );

            boolean visible = Boolean.TRUE.equals(result);

            log.info("JS visibility check for {}: {}", locator, visible);
            return visible;

        } catch (Exception e) {
            log.warn("JS isDisplayed failed: {}", locator);
            return false;
        }
    }

    /**
     * Performs a double-click action on the specified web element. This method attempts
     * multiple strategies to ensure the double-click is executed successfully, including:
     * Actions API, fallback to two consecutive single clicks, and JavaScript execution.
     *
     * @param locator  The {@code By} locator used to locate the target web element.
     * @param strategy The waiting strategy to use for ensuring the element is ready
     *                 for interaction before performing the double-click action.
     * @throws RuntimeException if all strategies to perform the double-click action fail.
     */
    public static void doubleClick(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll to element (center)
                scrollToElement(locator);

                // 1️⃣ Actions double click
                new Actions(DriverManager.getDriver())
                        .moveToElement(element)
                        .doubleClick()
                        .perform();

                log.info("Double click successful: {}", locator);
                return;

            } catch (ElementNotInteractableException | MoveTargetOutOfBoundsException e) {
                log.warn("Actions double click failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Click twice fallback
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    element.click();
                    element.click();

                    log.info("Fallback double click (2 clicks) successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Normal double click fallback failed: {}", locator);

                    try {
                        // 3️⃣ JS double click
                        jsDoubleClick(locator);
                        log.info("JS double click successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All double click strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying double click: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to double click element: " + locator);
    }

    /**
     * Performs a double-click operation on a web element using JavaScript.
     * <p>
     * This method first waits for the specified web element to be present,
     * scrolls it into view, and then dispatches a JavaScript mouse event
     * to simulate a double-click on the element.
     *
     * @param locator the By locator used to identify the web element on the page
     */
    public static void jsDoubleClick(By locator) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);

            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    element
            );

            js.executeScript(
                    "var evt = new MouseEvent('dblclick', { bubbles: true, cancelable: true });" +
                            "arguments[0].dispatchEvent(evt);",
                    element
            );

            log.info("JS double click performed: {}", locator);

        } catch (Exception e) {
            log.error("JS double click failed: {}", locator, e);
            throw new RuntimeException("Unable to perform JS double click: " + locator);
        }
    }

    /**
     * Performs a right-click (context-click) on the specified web element identified by the given locator.
     * Attempts three different strategies sequentially (Actions API, keyboard emulation, JavaScript) if initial attempts fail.
     * Retries up to three times in case of transient issues.
     *
     * @param locator  The {@link By} locator used to identify the target element.
     * @param strategy The {@link WaitStrategy} to define how the element is waited for before performing the action.
     * @throws RuntimeException If all attempts to perform the right-click fail after retrying.
     */
    public static void rightClick(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll to element (center)
                scrollToElement(locator);

                // 1️⃣ Actions Right Click
                new Actions(DriverManager.getDriver())
                        .moveToElement(element)
                        .contextClick()
                        .perform();

                log.info("Right click successful: {}", locator);
                return;

            } catch (ElementNotInteractableException | MoveTargetOutOfBoundsException e) {
                log.warn("Actions right click failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Click + Keyboard fallback (Shift+F10 → opens context menu in many apps)
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    element.click();
                    element.sendKeys(Keys.SHIFT, Keys.F10);

                    log.info("Keyboard right click fallback successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Keyboard fallback failed: {}", locator);

                    try {
                        // 3️⃣ JS Right Click
                        jsRightClick(locator);
                        log.info("JS right click successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All right click strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying right click: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to right click element: " + locator);
    }

    /**
     * Performs a right-click action on the specified web element using JavaScript.
     * This method scrolls the element into view, centers it, and triggers a context menu event.
     *
     * @param locator The {@link By} locator used to identify the web element to perform the right-click on.
     *                The locator should point to a visible and interactable element on the web page.
     * @throws RuntimeException If the right-click action fails due to an exception during execution.
     */
    public static void jsRightClick(By locator) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            // Scroll to center
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    element
            );

            // Trigger contextmenu event
            js.executeScript(
                    "var evt = new MouseEvent('contextmenu', {" +
                            "bubbles: true, cancelable: true, view: window, button: 2" +
                            "}); arguments[0].dispatchEvent(evt);",
                    element
            );

            log.info("JS right click performed: {}", locator);

        } catch (Exception e) {
            log.error("JS right click failed: {}", locator, e);
            throw new RuntimeException("Unable to perform JS right click: " + locator);
        }
    }

    /**
     * Performs a drag-and-drop action from a source element to a target element.
     * The method attempts multiple strategies including Actions API, alternative Actions API
     * with offset, and JavaScript-based drag-and-drop to ensure reliability.
     *
     * @param sourceLocator the locator of the source element to be dragged
     * @param targetLocator the locator of the target element where the source element will be dropped
     * @param strategy      the wait strategy to ensure the elements are available before performing the action
     * @throws RuntimeException if all drag-and-drop strategies fail after multiple attempts
     */
    public static void dragAndDrop(By sourceLocator, By targetLocator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement source = WaitUtil.waitForElement(sourceLocator, strategy);
                WebElement target = WaitUtil.waitForElement(targetLocator, strategy);

                // Scroll both elements into view
                scrollToElement(sourceLocator);
                scrollToElement(targetLocator);

                // 1️⃣ Actions drag and drop
                new Actions(DriverManager.getDriver())
                        .clickAndHold(source)
                        .pause(Duration.ofMillis(500))
                        .moveToElement(target)
                        .pause(Duration.ofMillis(500))
                        .release()
                        .build()
                        .perform();

                log.info("Drag and drop successful: {} → {}", sourceLocator, targetLocator);
                return;

            } catch (MoveTargetOutOfBoundsException | ElementNotInteractableException e) {
                log.warn("Actions drag and drop failed (attempt {}): {}", attempts + 1, sourceLocator);

                try {
                    // 2️⃣ Alternative Actions (more stable)
                    WebElement source = WaitUtil.waitForElement(sourceLocator, strategy);
                    WebElement target = WaitUtil.waitForElement(targetLocator, strategy);

                    new Actions(DriverManager.getDriver())
                            .clickAndHold(source)
                            .moveByOffset(0, 0) // trigger drag start
                            .moveToElement(target)
                            .release()
                            .perform();

                    log.info("Alternative drag and drop successful");
                    return;

                } catch (Exception ex1) {
                    log.warn("Alternative drag failed");

                    try {
                        // 3️⃣ JS Drag & Drop (HTML5 support)
                        jsDragAndDrop(sourceLocator, targetLocator);
                        log.info("JS drag and drop successful");
                        return;

                    } catch (Exception ex2) {
                        log.error("All drag and drop strategies failed (attempt {}): {}", attempts + 1, sourceLocator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying drag and drop: {}", sourceLocator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to drag and drop: " + sourceLocator);
    }

    /**
     * Simulates a drag-and-drop operation using JavaScript between two elements identified by the provided locators.
     * <p>
     * This method relies on JavaScript to perform the drag-and-drop interaction. It creates and dispatches custom
     * drag and drop events (`dragstart`, `drop`, `dragend`) on the elements specified by the locators.
     *
     * @param sourceLocator The {@code By} locator used to identify the source element to drag.
     * @param targetLocator The {@code By} locator used to identify the target element to drop.
     */
    public static void jsDragAndDrop(By sourceLocator, By targetLocator) {

        WebElement source = WaitUtil.waitForElement(sourceLocator, WaitStrategy.PRESENCE);
        WebElement target = WaitUtil.waitForElement(targetLocator, WaitStrategy.PRESENCE);

        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

        String script =
                """
                        function createEvent(typeOfEvent) {
                          var event = document.createEvent("CustomEvent");
                          event.initCustomEvent(typeOfEvent, true, true, null);
                          event.dataTransfer = {
                            data: {},
                            setData: function(key, value) { this.data[key] = value; },
                            getData: function(key) { return this.data[key]; }
                          };
                          return event;
                        }
                        function dispatchEvent(element, event, transferData) {
                          if (transferData !== undefined) {
                            event.dataTransfer = transferData;
                          }
                          element.dispatchEvent(event);
                        }
                        var dragStartEvent = createEvent('dragstart');
                        dispatchEvent(arguments[0], dragStartEvent);
                        var dropEvent = createEvent('drop');
                        dispatchEvent(arguments[1], dropEvent, dragStartEvent.dataTransfer);
                        var dragEndEvent = createEvent('dragend');
                        dispatchEvent(arguments[0], dragEndEvent, dropEvent.dataTransfer);""";

        js.executeScript(script, source, target);

        log.info("JS drag and drop executed");
    }

    /**
     * Performs a drag-and-drop operation on a web element identified by the specified locator.
     * The element is moved by the given x and y offsets. Multiple strategies are used to
     * attempt the drag-and-drop operation in case of failures, including standard action chains,
     * a step-by-step movement approach, and a JavaScript fallback.
     *
     * @param locator  The {@code By} locator used to find the web element to be dragged.
     * @param xOffset  The horizontal offset by which to move the element.
     * @param yOffset  The vertical offset by which to move the element.
     * @param strategy The waiting strategy to wait for the element to become interactable.
     *                 It specifies how the element will be waited for before performing the drag operation.
     */
    public static void dragAndDropBy(By locator, int xOffset, int yOffset, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll into view
                scrollToElement(locator);

                // 1️⃣ Standard drag by offset
                new Actions(DriverManager.getDriver())
                        .clickAndHold(element)
                        .pause(Duration.ofMillis(300))
                        .moveByOffset(xOffset, yOffset)
                        .pause(Duration.ofMillis(300))
                        .release()
                        .perform();

                log.info("Drag and drop by offset successful: {} ({} , {})", locator, xOffset, yOffset);
                return;

            } catch (MoveTargetOutOfBoundsException | ElementNotInteractableException e) {
                log.warn("Standard dragBy failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Alternative approach (step-by-step move)
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    new Actions(DriverManager.getDriver())
                            .moveToElement(element)
                            .clickAndHold()
                            .moveByOffset(xOffset / 2, yOffset / 2)
                            .moveByOffset(xOffset / 2, yOffset / 2)
                            .release()
                            .perform();

                    log.info("Step-based dragBy successful");
                    return;

                } catch (Exception ex1) {
                    log.warn("Step-based drag failed");

                    try {
                        // 3️⃣ JS fallback
                        jsDragAndDropBy(locator, xOffset, yOffset);
                        log.info("JS dragBy successful");
                        return;

                    } catch (Exception ex2) {
                        log.error("All dragBy strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying dragBy: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to drag element by offset: " + locator);
    }

    /**
     * Performs a drag-and-drop action using JavaScript on the given element, moving it by specified offsets.
     *
     * @param locator The {@code By} locator of the element to be dragged.
     * @param xOffset The horizontal offset by which the element will be dragged.
     * @param yOffset The vertical offset by which the element will be dragged.
     */
    public static void jsDragAndDropBy(By locator, int xOffset, int yOffset) {

        WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                element
        );

        js.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                        "var startX = rect.left + rect.width/2;" +
                        "var startY = rect.top + rect.height/2;" +

                        "var endX = startX + arguments[1];" +
                        "var endY = startY + arguments[2];" +

                        "var dataTransfer = new DataTransfer();" +

                        "arguments[0].dispatchEvent(new MouseEvent('mousedown', {clientX:startX, clientY:startY, bubbles:true}));" +
                        "document.dispatchEvent(new MouseEvent('mousemove', {clientX:endX, clientY:endY, bubbles:true}));" +
                        "document.dispatchEvent(new MouseEvent('mouseup', {clientX:endX, clientY:endY, bubbles:true}));",

                element, xOffset, yOffset
        );

        log.info("JS drag by offset executed: {} ({}, {})", locator, xOffset, yOffset);
    }

    /**
     * Attempts to perform a hover action on a web element located by the given locator.<br>
     * This method uses a combination of strategies to accomplish the hover, including:<br>
     * - Actions API for normal hover.<br>
     * - Offset movements as a fallback.<br>
     * - JavaScript simulation of mouseover as a last resort.<br>
     * The method retries the hover action up to 3 times if necessary.
     *
     * @param locator  The {@link By} locator used to find the web element to hover over.
     * @param strategy The {@link WaitStrategy} defining how to wait for the element to become interactable.
     * @throws RuntimeException If all hover strategies fail after the allowed attempts.
     */
    public static void hoverOverElement(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll into view (center)
                scrollToElement(locator);

                // 1️⃣ Actions hover
                new Actions(DriverManager.getDriver())
                        .moveToElement(element)
                        .pause(Duration.ofMillis(300)) // allow UI to react
                        .perform();

                log.info("Hover successful: {}", locator);
                return;

            } catch (MoveTargetOutOfBoundsException | ElementNotInteractableException e) {
                log.warn("Hover failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Move using offset (fallback)
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    new Actions(DriverManager.getDriver())
                            .moveToElement(element, 5, 5)
                            .perform();

                    log.info("Hover with offset successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Offset hover failed: {}", locator);

                    try {
                        // 3️⃣ JS hover (simulate mouseover)
                        jsHover(locator);
                        log.info("JS hover successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All hover strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying hover: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to hover over element: " + locator);
    }

    /**
     * Performs a hover action over a web element using JavaScript.
     * This method scrolls the element into view and simulates a mouse hover event.
     *
     * @param locator the locator used to identify the web element to hover over
     */
    public static void jsHover(By locator) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    element
            );

            js.executeScript(
                    "var event = new MouseEvent('mouseover', {bubbles: true});" +
                            "arguments[0].dispatchEvent(event);",
                    element
            );

            log.info("JS hover executed: {}", locator);

        } catch (Exception e) {
            log.error("JS hover failed: {}", locator, e);
            throw new RuntimeException("Unable to perform JS hover: " + locator);
        }
    }

    /**
     * Attempts to focus on a specific web element identified by a locator using
     * a combination of strategies. The method will try up to three focus attempts,
     * employing successive strategies: direct click, Actions API, and JavaScript,
     * until one succeeds or all attempts fail.
     *
     * @param locator  The {@code By} locator used to identify the WebElement to focus.
     * @param strategy The wait strategy to use for waiting until the element becomes interactable.
     *                 This helps ensure the element is in a usable state before attempting focus.
     */
    public static void focusElement(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll into view (center)
                scrollToElement(locator);

                // 1️⃣ Click to focus (most reliable)
                element.click();

                log.info("Element focused using click: {}", locator);
                return;

            } catch (ElementNotInteractableException e) {
                log.warn("Click focus failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Actions focus
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    new Actions(DriverManager.getDriver())
                            .moveToElement(element)
                            .click()
                            .perform();

                    log.info("Focus using Actions successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Actions focus failed: {}", locator);

                    try {
                        // 3️⃣ JS focus
                        jsFocus(locator);
                        log.info("JS focus successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All focus strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying focus: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to focus element: " + locator);
    }

    /**
     * Executes JavaScript commands to scroll the specified element into view and focus on it.
     * This method ensures that the element is both visible and focused.
     *
     * @param locator The locator used to find the element to be scrolled and focused.
     *                It must be a valid {@link By} object.
     *                Example locators include By.id, By.xpath, By.cssSelector, etc.
     */
    public static void jsFocus(By locator) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            // Scroll to center
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    element
            );

            // Focus element
            js.executeScript("arguments[0].focus();", element);

            log.info("JS focus executed: {}", locator);

        } catch (Exception e) {
            log.error("JS focus failed: {}", locator, e);
            throw new RuntimeException("Unable to focus element: " + locator);
        }
    }

    /**
     * Attempts to remove focus from an element located by the specified {@code locator}.
     * This method performs up to three retry attempts to blur the element using different strategies:
     * clicking on the body, sending a TAB key, or using a JavaScript blur.
     *
     * @param locator  the {@link By} locator used to find the target element to blur
     * @param strategy the {@link WaitStrategy} used to wait for the element before attempting to blur
     * @throws RuntimeException if all blur strategies fail after three attempts
     */
    public static void blurElement(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // 1️⃣ Click somewhere else (body) to remove focus
                DriverManager.getDriver().findElement(By.tagName("body")).click();

                log.info("Blur successful using body click: {}", locator);
                return;

            } catch (Exception e) {
                log.warn("Body click blur failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Use TAB key to shift focus
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    element.sendKeys(Keys.TAB);

                    log.info("Blur using TAB key successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("TAB blur failed: {}", locator);

                    try {
                        // 3️⃣ JS blur (direct)
                        jsBlur(locator);
                        log.info("JS blur successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All blur strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to blur element: " + locator);
    }

    /**
     * Executes a JavaScript `blur()` operation on the specified web element,
     * causing the element to lose focus.
     *
     * @param locator A {@link By} locator used to locate the target web element.
     *                The element must be present in the DOM for the operation to succeed.
     */
    public static void jsBlur(By locator) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("arguments[0].blur();", element);

            log.info("JS blur executed: {}", locator);

        } catch (Exception e) {
            log.error("JS blur failed: {}", locator, e);
            throw new RuntimeException("Unable to blur element: " + locator);
        }
    }

    /**
     * Presses a specified key on a given web element using multiple strategies if necessary.
     * The method attempts up to three strategies: direct element interaction,
     * Actions API-based key press, and JavaScript-based key press.
     * It retries the operation up to three times in case of failure due to conditions like
     * an element not being interactable or stale references.
     *
     * @param locator  The {@code By} locator used to identify the web element.
     * @param key      The {@code Keys} object representing the key to be pressed.
     * @param strategy The wait strategy to ensure the element is ready for interaction.
     */
    public static void pressKey(By locator, Keys key, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Ensure focus
                focusElement(locator, strategy);

                // 1️⃣ Normal key press
                element.sendKeys(key);

                log.info("Key '{}' pressed on element: {}", key, locator);
                return;

            } catch (ElementNotInteractableException e) {
                log.warn("Normal key press failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Actions key press
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    new Actions(DriverManager.getDriver())
                            .moveToElement(element)
                            .click()
                            .sendKeys(key)
                            .perform();

                    log.info("Actions key press successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Actions key press failed: {}", locator);

                    try {
                        // 3️⃣ JS key press
                        jsPressKey(locator, key);
                        log.info("JS key press successful: {}", locator);
                        return;

                    } catch (Exception ex2) {
                        log.error("All key press strategies failed (attempt {}): {}", attempts + 1, locator);
                    }
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying key press: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to press key on element: " + locator);
    }

    /**
     * Simulates a key press on a web element using JavaScript.
     *
     * @param locator The {@code By} object used to locate the web element.
     * @param key     The {@code Keys} constant representing the key to be pressed (e.g., ENTER, TAB).
     */
    public static void jsPressKey(By locator, Keys key) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, WaitStrategy.PRESENCE);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            String keyValue = key.toString(); // e.g. "Enter"

            js.executeScript(
                    "var event = new KeyboardEvent('keydown', {" +
                            "key: arguments[1], bubbles: true" +
                            "}); arguments[0].dispatchEvent(event);",
                    element, keyValue
            );

            log.info("JS key press executed: {}", locator);

        } catch (Exception e) {
            log.error("JS key press failed: {}", locator, e);
            throw new RuntimeException("Unable to perform JS key press: " + locator);
        }
    }

    /**
     * Simulates the pressing of a global key using the Selenium Actions API.
     *
     * @param key the key to be pressed, represented as an instance of the {@code Keys} enum.
     * @throws RuntimeException if an error occurs while attempting to press the key.
     */
    public static void pressKey(Keys key) {
        try {
            new Actions(DriverManager.getDriver())
                    .sendKeys(key)
                    .perform();

            log.info("Global key pressed: {}", key);

        } catch (Exception e) {
            throw new RuntimeException("❌ Unable to press global key: " + key);
        }
    }

    /**
     * Simulates a keyboard "key down" action on a specified web element or the global context.
     *
     * @param locator  The {@code By} locator used to find the target web element.
     * @param key      The {@code Keys} value representing the key to press and hold.
     * @param strategy The wait strategy to use for locating and interacting with the web element.
     */
    public static void keyDown(By locator, Keys key, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Ensure focus
                focusElement(locator, strategy);

                // 1️⃣ Actions keyDown
                new Actions(DriverManager.getDriver())
                        .moveToElement(element)
                        .click()
                        .keyDown(key)
                        .perform();

                log.info("KeyDown '{}' performed on: {}", key, locator);
                return;

            } catch (ElementNotInteractableException e) {
                log.warn("keyDown failed (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Global keyDown fallback
                    new Actions(DriverManager.getDriver())
                            .keyDown(key)
                            .perform();

                    log.info("Global keyDown successful: {}", key);
                    return;

                } catch (Exception ex1) {
                    log.error("All keyDown strategies failed: {}", locator);
                }

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying keyDown: {}", locator);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to perform keyDown: " + key);
    }

    /**
     * Simulates the release of a given key during a keyboard interaction.
     *
     * @param key the key to be released, represented by the {@code Keys} enumeration
     */
    public static void keyUp(Keys key) {
        try {
            new Actions(DriverManager.getDriver())
                    .keyUp(key)
                    .perform();

            log.info("KeyUp '{}' performed", key);

        } catch (Exception e) {
            log.error("KeyUp failed: {}", key, e);
            throw new RuntimeException("❌ Unable to perform keyUp: " + key);
        }
    }

    /**
     * Simulates releasing a key action on the specified web element.
     *
     * @param locator  The {@code By} locator used to identify the target web element.
     * @param key      The {@code Keys} value representing the key to release.
     * @param strategy The {@code WaitStrategy} used to wait for the element to become available.
     */
    public static void keyUp(By locator, Keys key, WaitStrategy strategy) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, strategy);

            // Ensure focus
            focusElement(locator, strategy);

            new Actions(DriverManager.getDriver())
                    .moveToElement(element)
                    .click()
                    .keyUp(key)
                    .perform();

            log.info("KeyUp '{}' performed on: {}", key, locator);

        } catch (Exception e) {
            log.error("KeyUp failed on element: {}", locator, e);
            throw new RuntimeException("❌ Unable to perform keyUp: " + key);
        }
    }

    /**
     * Executes a key combination on a specified web element located by the given locator.
     *
     * @param locator  The {@code By} locator used to identify the web element.
     * @param key      The {@code Keys} value representing the keyboard key to be pressed down.
     * @param value    The string value to be sent as input while the key is held down.
     * @param strategy The {@code WaitStrategy} to be used for locating and waiting for the element.
     */
    public static void performKeyCombo(By locator, Keys key, String value, WaitStrategy strategy) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, strategy);

            focusElement(locator, strategy);

            new Actions(DriverManager.getDriver())
                    .keyDown(key)
                    .sendKeys(element, value)
                    .keyUp(key)
                    .perform();

            log.info("Key combo '{}' + '{}' executed on {}", key, value, locator);

        } catch (Exception e) {
            throw new RuntimeException("❌ Key combo failed: " + key);
        }
    }

    /**
     * Uploads a file to a web application using the specified element locator, file path,
     * and wait strategy. The method supports both input elements of type "file" and
     * custom upload buttons that trigger OS file dialogs.
     *
     * @param locator  The locator used to find the file upload element on the webpage.
     *                 This could be an ID, CSS selector, XPath, or other supported types.
     * @param filePath The absolute file path of the file to upload. This file must exist
     *                 on the system where the script is executed.
     * @param strategy The wait strategy used to wait for the file upload element to become
     *                 available for interaction. Common strategies include visibility,
     *                 presence, or clickable state.
     */
    public static void uploadFile(By locator, String filePath, WaitStrategy strategy) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, strategy);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            String tagName = element.getTagName();
            String type = element.getAttribute("type");

            // =========================
            // 1. INPUT TYPE="FILE" — direct sendKeys (headless-safe)
            // =========================
            if ("input".equalsIgnoreCase(tagName) && "file".equalsIgnoreCase(type)) {
                makeElementInteractable(js, element);
                element.sendKeys(filePath);
                log.info("File uploaded via input[type=file] sendKeys: {}", filePath);
                return;
            }

            // =========================
            // 2. CUSTOM BUTTON (e.g. dummyUpload) — find the hidden
            //    input[type=file] in the surrounding DOM and use sendKeys.
            //    Works in headless mode; no OS dialog required.
            // =========================
            log.info("Custom upload button detected — searching for associated input[type=file] near: {}", locator);

            WebElement fileInput = (WebElement) js.executeScript(
                    "var btn = arguments[0];" +
                    "var el = btn.parentElement;" +
                    "while (el) {" +
                    "  var fi = el.querySelector('input[type=\"file\"]');" +
                    "  if (fi) return fi;" +
                    "  el = el.parentElement;" +
                    "}" +
                    "return null;",
                    element
            );

            if (fileInput != null) {
                log.info("Found hidden input[type=file] — uploading via sendKeys (headless-safe): {}", filePath);
                makeElementInteractable(js, fileInput);
                fileInput.sendKeys(filePath);
                log.info("File uploaded successfully: {}", filePath);
                return;
            }

            // =========================
            // 3. FALLBACK — OS dialog (not headless-safe)
            // =========================
            log.warn("No input[type=file] found near element — falling back to OS dialog: {}", locator);
            click(locator, strategy);
            handleOSFileUpload(filePath);

        } catch (Exception e) {
            log.error("File upload failed for locator {}: {}", locator, e.getMessage(), e);
            throw new RuntimeException("❌ Upload failed: " + filePath);
        }
    }

    /**
     * Makes a hidden file input element interactable by removing common CSS hiding techniques.
     * Required before calling {@code sendKeys} on file inputs that are hidden via
     * {@code display:none}, {@code visibility:hidden}, {@code opacity:0}, or the {@code hidden} attribute.
     *
     * @param js      the {@link JavascriptExecutor} instance
     * @param element the element to make interactable
     */
    private static void makeElementInteractable(JavascriptExecutor js, WebElement element) {
        js.executeScript(
                "arguments[0].style.display='block';" +
                "arguments[0].style.visibility='visible';" +
                "arguments[0].style.opacity='1';" +
                "arguments[0].removeAttribute('hidden');",
                element
        );
    }

    /**
     * Handles file uploads across different operating systems by determining the OS at runtime
     * and utilizing appropriate methods to perform the upload.
     *
     * @param filePath The absolute path of the file to be uploaded. This path should point to a
     *                 valid file on the system to ensure successful upload.
     */
    public static void handleOSFileUpload(String filePath) {

        String os = System.getProperty("os.name").toLowerCase();

        log.info("Detected OS: {}", os);

        try {
            if (os.contains("win")) {
                uploadUsingRobot(filePath);

            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                uploadUsingRobot(filePath); // works for Linux too

            } else if (os.contains("mac")) {
                uploadUsingRobotMac(filePath);

            } else {
                throw new RuntimeException("Unsupported OS: " + os);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ OS upload failed for: " + os);
        }
    }

    /**
     * Automates the file upload process using the Robot class to simulate keyboard events.
     * This method copies the specified file path to the system clipboard, simulates
     * pasting the file path using CTRL+V, and then presses ENTER to confirm the upload.
     *
     * @param filePath the absolute file path of the file to be uploaded
     * @throws Exception if any exception occurs during the Robot operations or thread sleep
     */
    public static void uploadUsingRobot(String filePath) throws Exception {

        StringSelection selection = new StringSelection(filePath);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

        Robot robot = new Robot();

        Thread.sleep(500);

        // CTRL + V
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);

        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        Thread.sleep(500);

        // ENTER
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        log.info("File uploaded using Robot (Windows/Linux): {}", filePath);
    }

    /**
     * Automates the process of uploading a file on macOS by simulating keyboard actions
     * using the Robot class. This method is specifically designed to handle file uploads
     * by pasting the file path into a file selection dialog.
     *
     * @param filePath the absolute path of the file to be uploaded
     * @throws Exception if an error occurs during the robot operation or the thread sleep
     */
    public static void uploadUsingRobotMac(String filePath) throws Exception {

        StringSelection selection = new StringSelection(filePath);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

        Robot robot = new Robot();

        Thread.sleep(500);

        // CMD + SHIFT + G (Go to folder)
        robot.keyPress(KeyEvent.VK_META);
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_G);

        robot.keyRelease(KeyEvent.VK_G);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_META);

        Thread.sleep(500);

        // CMD + V
        robot.keyPress(KeyEvent.VK_META);
        robot.keyPress(KeyEvent.VK_V);

        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_META);

        Thread.sleep(500);

        // ENTER
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        Thread.sleep(500);

        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        log.info("File uploaded using Robot (Mac): {}", filePath);
    }

    /**
     * Selects an option from a dropdown menu (identified by the given locator)
     * by matching the visible text of the option. Retries up to three times in
     * case of failures. If the dropdown is stale or not interactable, it falls
     * back to clicking the element and using sendKeys as a workaround.
     *
     * @param locator     the {@code By} locator to identify the dropdown element.
     * @param visibleText the visible text of the option to be selected from the dropdown.
     * @param strategy    the waiting strategy to be used while waiting for the dropdown
     *                    element to become available.
     * @throws RuntimeException if the dropdown is not a select element, or if the
     *                          option cannot be selected after retries, or if the fallback
     *                          strategy fails.
     */
    public static void selectByVisibleText(By locator, String visibleText, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll to element
                scrollToElement(locator);

                // Ensure it's a <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select> dropdown: " + locator);
                }

                Select select = new Select(element);
                select.selectByVisibleText(visibleText);

                log.info("Selected '{}' from dropdown: {}", visibleText, locator);
                return;

            } catch (NoSuchElementException e) {
                log.error("Option '{}' not found in dropdown: {}", visibleText, locator);
                throw new RuntimeException("❌ Option not found: " + visibleText);

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying select: {}", locator);

            } catch (ElementNotInteractableException e) {
                log.warn("Dropdown not interactable (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Fallback: click + sendKeys
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    element.click();
                    element.sendKeys(visibleText);

                    log.info("Fallback select using sendKeys successful: {}", visibleText);
                    return;

                } catch (Exception ex1) {
                    log.warn("Fallback select failed");
                }

            } catch (Exception e) {
                log.error("Select by visible text failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to select value: " + visibleText);
    }

    /**
     * Selects an option from a dropdown based on the given value.
     * Attempts to locate the dropdown element using the provided locator,
     * verifies it is a select element, and selects the option with the specified value.
     *
     * @param locator  The {@code By} locator to identify the dropdown element.
     * @param value    The value of the option to select from the dropdown.
     * @param strategy The wait strategy to be used while attempting to locate the dropdown element.
     * @throws RuntimeException If the specified value is not found, the element is not a select,
     *                          the element becomes stale, or the dropdown is not interactable despite retries.
     */
    public static void selectByValue(By locator, String value, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll to element
                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);
                select.selectByValue(value);

                log.info("Selected value '{}' from dropdown: {}", value, locator);
                return;

            } catch (NoSuchElementException e) {
                log.error("Value '{}' not found in dropdown: {}", value, locator);
                throw new RuntimeException("❌ Value not found: " + value);

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying selectByValue: {}", locator);

            } catch (ElementNotInteractableException e) {
                log.warn("Dropdown not interactable (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Fallback → sendKeys (sometimes works)
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    element.click();
                    element.sendKeys(value);

                    log.info("Fallback select using sendKeys successful: {}", value);
                    return;

                } catch (Exception ex1) {
                    log.warn("Fallback failed for selectByValue");
                }

            } catch (Exception e) {
                log.error("selectByValue failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to select value: " + value);
    }

    /**
     * Selects an option from a dropdown element based on the specified index.
     * This method retries the selection process up to three times in the case of
     * transient issues such as stale or non-interactable elements. Additionally,
     * it validates whether the target element is select and ensures the
     * specified index is within the bounds of available options.
     *
     * @param locator  The {@code By} locator representing the dropdown element to interact with.
     * @param index    The zero-based index of the option to select within the dropdown.
     *                 Must be within the range of available options.
     * @param strategy The {@code WaitStrategy} to use for locating and interacting with the element.
     *                 Determines how the dropdown is waited for before performing actions.
     * @throws RuntimeException if the locator does not point to a select element, the index
     *                          is invalid, or if the index could not be selected after retries.
     */
    public static void selectByIndex(By locator, int index, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll into view
                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);

                // Validate index range
                int size = select.getOptions().size();

                if (index < 0 || index >= size) {
                    throw new RuntimeException("❌ Invalid index: " + index + " | Available: " + size);
                }

                select.selectByIndex(index);

                log.info("Selected index '{}' from dropdown: {}", index, locator);
                return;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying selectByIndex: {}", locator);

            } catch (ElementNotInteractableException e) {
                log.warn("Dropdown not interactable (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Fallback → manual option click
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    Select select = new Select(element);
                    select.getOptions().get(index).click();

                    log.info("Fallback select using option click successful: {}", index);
                    return;

                } catch (Exception ex1) {
                    log.warn("Fallback failed for selectByIndex");
                }

            } catch (Exception e) {
                log.error("selectByIndex failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to select index: " + index);
    }

    /**
     * Deselects an option in a multi-select dropdown by its visible text.
     * This method ensures that the target element is a valid multi-select dropdown
     * and attempts to deselect the specified visible text up to three times.
     * If the element is stale or not interactable, retry and fallback mechanisms are employed.
     *
     * @param locator     The {@code By} locator used to locate the multi-select dropdown element.
     * @param visibleText The visible text of the option to be deselected.
     * @param strategy    The wait strategy to be applied for locating the element before interaction.
     */
    public static void deselectByVisibleText(By locator, String visibleText, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);

                // Validate multi-select
                if (!select.isMultiple()) {
                    throw new RuntimeException("❌ Dropdown is not multi-select: " + locator);
                }

                select.deselectByVisibleText(visibleText);

                log.info("Deselected '{}' from dropdown: {}", visibleText, locator);
                return;

            } catch (NoSuchElementException e) {
                log.error("Option '{}' not found for deselection: {}", visibleText, locator);
                throw new RuntimeException("❌ Option not found: " + visibleText);

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying deselect: {}", locator);

            } catch (ElementNotInteractableException e) {
                log.warn("Dropdown not interactable (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Fallback → manual option click (toggle)
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    Select select = new Select(element);

                    for (WebElement option : select.getOptions()) {
                        if (option.getText().equalsIgnoreCase(visibleText) && option.isSelected()) {
                            option.click();
                            log.info("Fallback deselect successful: {}", visibleText);
                            return;
                        }
                    }

                } catch (Exception ex1) {
                    log.warn("Fallback deselect failed");
                }

            } catch (Exception e) {
                log.error("deselectByVisibleText failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to deselect: " + visibleText);
    }

    /**
     * Deselects all selected options in a dropdown menu specified by the given locator.
     * This method validates that the target element is a multi-select dropdown before attempting
     * to deselect all options. If the element is stale or not interactable, retries are attempted
     * or a fallback mechanism is used to manually deselect each selected option.
     *
     * @param locator  The {@code By} locator used to identify the multi-select dropdown element.
     * @param strategy The {@code WaitStrategy} used to wait for the element to be available and ready for interaction.
     *                 Common strategies include explicit waits for visibility or clickability.
     * @throws RuntimeException if the specified element is not a select tag, is not a multi-select dropdown,
     *                          or if all attempts to deselect options fail.
     */
    public static void deselectAll(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                // Scroll into view
                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);

                // Validate multi-select
                if (!select.isMultiple()) {
                    throw new RuntimeException("❌ Dropdown is not multi-select: " + locator);
                }

                // Perform deselect all
                select.deselectAll();

                log.info("All options deselected successfully: {}", locator);
                return;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying deselectAll: {}", locator);

            } catch (ElementNotInteractableException e) {
                log.warn("Dropdown not interactable (attempt {}): {}", attempts + 1, locator);

                try {
                    // 2️⃣ Fallback → manually deselect each selected option
                    WebElement element = WaitUtil.waitForElement(locator, strategy);

                    Select select = new Select(element);

                    for (WebElement option : select.getAllSelectedOptions()) {
                        option.click(); // toggle off
                    }

                    log.info("Fallback deselectAll successful: {}", locator);
                    return;

                } catch (Exception ex1) {
                    log.warn("Fallback deselectAll failed");
                }

            } catch (Exception e) {
                log.error("deselectAll failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to deselect all options: " + locator);
    }

    /**
     * Retrieves the text of the currently selected option from a dropdown element.
     *
     * @param locator  The {@link By} object representing the location of the dropdown element.
     * @param strategy The wait strategy to be used for locating the dropdown element.
     * @return The text of the currently selected option in the dropdown element.
     * @throws RuntimeException If the located element is not a dropdown select
     *                          or if no option is selected, or if the method fails
     *                          after multiple attempts.
     */
    public static String getSelectedOption(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);

                WebElement selectedOption = select.getFirstSelectedOption();

                String text = selectedOption.getText();

                log.info("Selected option for {} is '{}'", locator, text);

                return text;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying getSelectedOption: {}", locator);

            } catch (NoSuchElementException e) {
                log.error("No selected option found: {}", locator);
                throw new RuntimeException("❌ No selected option");

            } catch (Exception e) {
                log.error("getSelectedOption failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to fetch selected option: " + locator);
    }

    /**
     * Retrieves all options' visible text from a dropdown element identified by
     * the given locator after applying the specified wait strategy.
     * <p>
     * This method retries fetching the options up to three times in case of stale
     * element issues or other transient errors. If the specified element is not a
     * dropdown (select tag), a runtime exception is thrown.
     *
     * @param locator  the {@link By} locator used to identify the dropdown element
     * @param strategy the {@link WaitStrategy} to apply for waiting until the
     *                 element is ready for interaction
     * @return a {@link List} of strings containing the visible text of all options
     * in the dropdown
     * @throws RuntimeException if the element is not a select tag, or if options
     *                          cannot be fetched after retries
     */
    public static List<String> getAllOptions(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                // Validate <select>
                if (!element.getTagName().equalsIgnoreCase("select")) {
                    throw new RuntimeException("Element is not a <select>: " + locator);
                }

                Select select = new Select(element);

                List<String> options = select.getOptions()
                        .stream()
                        .map(WebElement::getText)
                        .collect(Collectors.toList());

                log.info("Fetched {} options from dropdown: {}", options.size(), locator);

                return options;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying getAllOptions: {}", locator);

            } catch (Exception e) {
                log.error("getAllOptions failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to fetch dropdown options: " + locator);
    }

    /**
     * Evaluates whether the web element matching the specified locator is selected.
     * This method attempts to handle stale element references and retries the operation
     * up to three times before failing.
     *
     * @param locator  the {@code By} object used to locate the web element.
     * @param strategy the wait strategy to be applied while locating the element.
     * @return {@code true} if the element is selected; {@code false} otherwise, or if an error occurs.
     */
    public static boolean isSelected(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                boolean status = element.isSelected();

                log.info("Element selected status for {} is {}", locator, status);

                return status;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying isSelected: {}", locator);

            } catch (Exception e) {
                log.error("isSelected failed: {}", locator, e);
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Determines whether the specified web element, located by the given locator,
     * is currently enabled within the web page.
     *
     * @param locator  the locator used to find the desired web element
     * @param strategy the wait strategy to be applied for locating the element
     * @return true if the web element is enabled, false otherwise
     */
    public static boolean isEnabled(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement element = WaitUtil.waitForElement(locator, strategy);

                scrollToElement(locator);

                boolean status = element.isEnabled();

                log.info("Element enabled status for {} is {}", locator, status);

                return status;

            } catch (StaleElementReferenceException e) {
                log.warn("Stale element, retrying isEnabled: {}", locator);

            } catch (Exception e) {
                log.error("isEnabled failed: {}", locator, e);
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Verifies if a web element identified by the specified locator is clickable within a defined timeout period.
     * The method ensures the element is both visible and enabled on the page.
     *
     * @param locator  An instance of {@code By} used to locate the web element.
     * @param strategy A {@code WaitStrategy} defining how to wait for the element (not directly used in this implementation).
     * @return {@code true} if the element is clickable (visible and enabled);
     * {@code false} otherwise, or if a timeout or any other exception occurs during the check.
     */
    public static boolean isClickable(By locator, WaitStrategy strategy) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            WebElement element = wait.until(
                    ExpectedConditions.elementToBeClickable(locator)
            );

            scrollToElement(locator);

            boolean status = element.isDisplayed() && element.isEnabled();

            log.info("Element clickable status for {} is {}", locator, status);

            return status;

        } catch (TimeoutException e) {
            log.warn("Element not clickable (timeout): {}", locator);
            return false;

        } catch (Exception e) {
            log.error("isClickable failed: {}", locator, e);
            return false;
        }
    }

    /**
     * Checks whether a specified element is clickable by attempting to wait for the element
     * to become clickable over several attempts. Handles potential stale element references
     * and returns a boolean indicating the result.
     *
     * @param locator The {@code By} locator used to identify the element to be checked.
     * @return {@code true} if the element is clickable within the allotted attempts and timeout;
     * {@code false} otherwise.
     */
    public static boolean isClickableAdvanced(By locator) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebDriverWait wait = new WebDriverWait(
                        DriverManager.getDriver(),
                        Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
                );

                wait.until(ExpectedConditions.elementToBeClickable(locator));

                return true;

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying isClickable due to stale element: {}", locator);

            } catch (TimeoutException e) {
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Waits for a web element identified by the given locator to disappear from the DOM
     * or become invisible on the page within the specified explicit wait timeout.
     *
     * @param locator the {@link By} locator used to identify the web element that needs to disappear
     * @return {@code true} if the element disappears or becomes invisible within the timeout,
     * {@code false} otherwise (e.g., if the timeout is reached or there is an error)
     */
    public static boolean waitForElementToDisappear(By locator) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            boolean result = wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(locator)
            );

            log.info("Element disappeared successfully: {}", locator);

            return result;

        } catch (TimeoutException e) {
            log.warn("Element still visible after timeout: {}", locator);
            return false;

        } catch (Exception e) {
            log.error("waitForElementToDisappear failed: {}", locator, e);
            return false;
        }
    }

    /**
     * Waits for the specified web element to disappear from the DOM or become invisible within a predefined timeout period.
     * The method will make up to three attempts in case of a stale element reference exception before failing.
     *
     * @param locator the locator used to find the web element (e.g., By.id, By.xpath, etc.).
     * @return true if the element disappears within the timeout period; false otherwise.
     */
    public static boolean waitForElementToDisappearAdvanced(By locator) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebDriverWait wait = new WebDriverWait(
                        DriverManager.getDriver(),
                        Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
                );

                return wait.until(
                        ExpectedConditions.invisibilityOfElementLocated(locator)
                );

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying due to stale element: {}", locator);

            } catch (TimeoutException e) {
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Waits for a specific text to be present in a web element located by the given locator.
     * Returns true if the expected text is found within the specified timeout; otherwise, returns false.
     *
     * @param locator      The locator used to find the web element.
     * @param expectedText The text that is expected to be present in the specified web element.
     * @return true if the text is found within the timeout duration, false otherwise.
     */
    public static boolean waitForTextToBePresent(By locator, String expectedText) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            boolean result = wait.until(
                    ExpectedConditions.textToBePresentInElementLocated(locator, expectedText)
            );

            log.info("Text '{}' is present in element: {}", expectedText, locator);

            return result;

        } catch (TimeoutException e) {
            log.warn("Text '{}' not found within timeout in element: {}", expectedText, locator);
            return false;

        } catch (Exception e) {
            log.error("waitForTextToBePresent failed: {}", locator, e);
            return false;
        }
    }

    /**
     * Waits for the specified text to be present in the element located by the given locator.
     * Performs up to three retry attempts in case of a StaleElementReferenceException, and
     * uses an explicit wait to check the presence of the expected text in the targeted element.
     *
     * @param locator      the By locator identifying the desired element to monitor.
     * @param expectedText the text that is expected to be present in the element.
     * @return true if the expected text is found in the element within the timeout period;
     * false otherwise, including in the event of timeouts or failures to locate/validate the element.
     */
    public static boolean waitForTextToBePresentAdvanced(By locator, String expectedText) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebDriverWait wait = new WebDriverWait(
                        DriverManager.getDriver(),
                        Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
                );

                return wait.until(
                        ExpectedConditions.textToBePresentInElementLocated(locator, expectedText)
                );

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying due to stale element: {}", locator);

            } catch (TimeoutException e) {
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Waits for a specific attribute of a web element to contain a specified value within a defined timeout period.
     *
     * @param locator   The locator of the web element whose attribute is to be checked.
     * @param attribute The attribute of the web element to monitor for the specified value.
     * @param value     The value that the attribute is expected to contain.
     * @return true if the attribute contains the specified value within the timeout period, otherwise false.
     */
    public static boolean waitForAttributeToContain(By locator, String attribute, String value) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            boolean result = wait.until(
                    ExpectedConditions.attributeContains(locator, attribute, value)
            );

            log.info("Attribute '{}' contains '{}' for element: {}", attribute, value, locator);

            return result;

        } catch (TimeoutException e) {
            log.warn("Attribute '{}' did NOT contain '{}' within timeout for: {}", attribute, value, locator);
            return false;

        } catch (Exception e) {
            log.error("waitForAttributeToContain failed: {}", locator, e);
            return false;
        }
    }

    /**
     * Waits for an element's attribute to contain a specific value. This method attempts to
     * handle stale element references by retrying up to three times before returning a result.
     *
     * @param locator   the {@code By} locator used to identify the target element
     * @param attribute the name of the attribute to be monitored
     * @param value     the expected value that the attribute should contain
     * @return {@code true} if the attribute contains the specified value within the explicit wait time;
     * {@code false} otherwise
     */
    public static boolean waitForAttributeToContainAdvanced(By locator, String attribute, String value) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebDriverWait wait = new WebDriverWait(
                        DriverManager.getDriver(),
                        Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
                );

                return wait.until(
                        ExpectedConditions.attributeContains(locator, attribute, value)
                );

            } catch (StaleElementReferenceException e) {
                log.warn("Retrying due to stale element: {}", locator);

            } catch (TimeoutException e) {
                return false;
            }

            attempts++;
        }

        return false;
    }

    /**
     * Waits for a specific attribute of a web element to have the desired value.
     *
     * @param locator   the locator used to identify the web element.
     * @param attribute the attribute of the web element to be checked.
     * @param value     the expected value of the specified attribute.
     * @return true if the attribute reaches the expected value within the defined wait time, false otherwise.
     */
    public static boolean waitForAttributeToBe(By locator, String attribute, String value) {

        WebDriverWait wait = new WebDriverWait(
                DriverManager.getDriver(),
                Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
        );

        return wait.until(
                ExpectedConditions.attributeToBe(locator, attribute, value)
        );
    }

    /**
     * Waits for the page title to contain the expected title substring within a specified timeout.
     * Logs an information message if the expected title is found in the page title.
     * Logs a warning message if the timeout is reached and the title does not contain the expected substring.
     * Logs an error message if any unexpected exception occurs during this operation.
     *
     * @param expectedTitle The substring of the page title to wait for. The method will wait
     *                      until the title of the current page contains this string.
     */
    public static void waitForTitle(String expectedTitle) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            boolean result = wait.until(
                    ExpectedConditions.titleContains(expectedTitle)
            );

            log.info("Page title contains: '{}'", expectedTitle);

        } catch (TimeoutException e) {
            log.warn("Title did NOT contain '{}' within timeout", expectedTitle);

        } catch (Exception e) {
            log.error("waitForTitle failed", e);
        }
    }

    /**
     * Waits for the current URL of the browser to contain the specified substring.
     * This method uses an explicit wait to verify if the expected part of the URL
     * exists within the current URL of the application. If the condition is met
     * within the specified timeout, the method returns true. Otherwise, it returns false.
     *
     * @param expectedUrlPart The substring of the URL to be matched.
     * @return true if the current URL contains the specified substring within the timeout;
     * false otherwise.
     */
    public static boolean waitForUrl(String expectedUrlPart) {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            boolean result = wait.until(
                    ExpectedConditions.urlContains(expectedUrlPart)
            );

            log.info("URL contains: '{}'", expectedUrlPart);

            return result;

        } catch (TimeoutException e) {
            log.warn("URL did NOT contain '{}' within timeout", expectedUrlPart);
            return false;

        } catch (Exception e) {
            log.error("waitForUrl failed", e);
            return false;
        }
    }

    /**
     * Switches the WebDriver's context to the specified frame.
     * The method attempts to locate the frame using the provided locator and wait strategy,
     * retries up to three times in case of transient issues such as stale elements or frame not being immediately available.
     * Throws a RuntimeException if switching to the frame fails after the allowed attempts.
     *
     * @param locator  The By locator used to identify the frame element.
     * @param strategy The wait strategy defining how to wait for the frame element to appear.
     */
    public static void switchToFrame(By locator, WaitStrategy strategy) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                WebElement frame = WaitUtil.waitForElement(locator, strategy);

                DriverManager.getDriver().switchTo().frame(frame);

                log.info("Switched to frame using locator: {}", locator);
                return;

            } catch (NoSuchFrameException e) {
                log.error("Frame not found: {}", locator);
                throw new RuntimeException("❌ Frame not found");

            } catch (StaleElementReferenceException e) {
                log.warn("Stale frame, retrying: {}", locator);

            } catch (Exception e) {
                log.error("switchToFrame failed: {}", locator, e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to switch to frame: " + locator);
    }

    /**
     * Switches the WebDriver's context to the specified iframe using its index.
     *
     * @param index the zero-based index of the iframe to switch to
     * @throws RuntimeException if the switch to the specified frame fails
     */
    public static void switchToFrame(int index) {

        try {
            DriverManager.getDriver().switchTo().frame(index);
            log.info("Switched to frame using index: {}", index);

        } catch (Exception e) {
            log.error("Failed to switch to frame index: {}", index, e);
            throw new RuntimeException("❌ Frame index not found: " + index);
        }
    }

    /**
     * Switches the context of the WebDriver to the specified frame using its name or ID.
     * This allows interaction with elements within the target frame.
     *
     * @param nameOrId the name or ID of the frame to switch to
     *                 (provided as a String, must match the frame's identifier in the DOM).
     * @throws RuntimeException if the frame with the given name or ID cannot be found or switched to
     */
    public static void switchToFrame(String nameOrId) {

        try {
            DriverManager.getDriver().switchTo().frame(nameOrId);
            log.info("Switched to frame using name/id: {}", nameOrId);

        } catch (Exception e) {
            log.error("Failed to switch to frame: {}", nameOrId, e);
            throw new RuntimeException("❌ Frame not found: " + nameOrId);
        }
    }

    /**
     * Switches the WebDriver's focus to the default content of the current page.
     * This method is typically used to exit from a frame or iframe back to the main document.
     * <p>
     * Logs an informational message upon successful execution.
     * If an exception occurs, logs an error message and throws a RuntimeException.
     * <p>
     * Throws:
     * RuntimeException - if switching to default content fails.
     */
    public static void switchToDefaultContent() {

        try {
            DriverManager.getDriver().switchTo().defaultContent();
            log.info("Switched to default content");

        } catch (Exception e) {
            log.error("Failed to switch to default content", e);
            throw new RuntimeException("❌ Unable to switch to default content");
        }
    }

    /**
     * Switches the current context of the WebDriver to the parent frame.
     * <p>
     * This method is used when the driver is currently focused on an iframe
     * and needs to navigate back to the parent frame in the DOM hierarchy.
     * Logs a message on successful switch or an error message if the operation fails.
     *
     * @throws RuntimeException if an error occurs during the frame switch operation.
     */
    public static void switchToParentFrame() {

        try {
            DriverManager.getDriver().switchTo().parentFrame();
            log.info("Switched to parent frame");

        } catch (Exception e) {
            log.error("Failed to switch to parent frame", e);
            throw new RuntimeException("❌ Unable to switch to parent frame");
        }
    }

    /**
     * Switches the driver's focus to a browser window that matches the specified title.
     * If no such window is found, an exception is thrown, and the focus reverts to the initial window.
     *
     * @param expectedTitle the title of the browser window to switch to.
     *                      The method searches for a window containing the specified title in its name.
     * @throws RuntimeException if no window with the matching title is found or if an error occurs during the switch.
     */
    public static void switchToWindow(String expectedTitle) {

        String currentWindow = DriverManager.getDriver().getWindowHandle();

        try {
            Set<String> allWindows = DriverManager.getDriver().getWindowHandles();

            for (String window : allWindows) {

                DriverManager.getDriver().switchTo().window(window);

                String title = DriverManager.getDriver().getTitle();

                log.info("Checking window: {} with title: {}", window, title);

                assert title != null;
                if (title.contains(expectedTitle)) {
                    log.info("Switched to window with title: {}", expectedTitle);
                    return;
                }
            }

            // fallback → switch back
            DriverManager.getDriver().switchTo().window(currentWindow);

            throw new RuntimeException("❌ No window found with title: " + expectedTitle);

        } catch (Exception e) {
            log.error("switchToWindow failed for title: {}", expectedTitle, e);
            throw new RuntimeException("❌ Window switch failed");
        }
    }

    /**
     * Switches the driver's focus to the window identified by the specified index.
     *
     * @param index the zero-based index of the target window in the list of open window handles
     *              currently managed by the WebDriver. Must be within the bounds of the list.
     * @throws RuntimeException if the provided index is invalid or an error occurs
     *                          while attempting to switch to the specified window.
     */
    public static void switchToWindow(int index) {

        try {
            List<String> windows = new ArrayList<>(DriverManager.getDriver().getWindowHandles());

            if (index >= windows.size()) {
                throw new RuntimeException("Invalid window index: " + index);
            }

            DriverManager.getDriver().switchTo().window(windows.get(index));

            log.info("Switched to window index: {}", index);

        } catch (Exception e) {
            log.error("Failed to switch window index: {}", index, e);
            throw new RuntimeException("❌ Window switch failed");
        }
    }

    /**
     * Closes the currently active browser window and, if applicable, switches the WebDriver context to
     * another available window. If no other windows are available, the WebDriver context remains unset.
     * <p>
     * This method uses the {@code DriverManager} class to retrieve the current WebDriver instance
     * and attempts the following steps:<p>
     * 1. Retrieves the handle for the current active window and logs it.<p>
     * 2. Retrieves all available window handles before closing the current window for logging purposes.<p>
     * 3. Closes the currently active window.<p>
     * 4. Attempts to switch to another available window if one exists. If no other windows are found,
     * a warning is logged.
     * <p>
     * In case of exceptions during the process, logs the error and throws a {@link RuntimeException}.
     * <p>
     * Note: Ensure the WebDriver session is properly managed using the {@code DriverManager} utility.
     * <p>
     * Throws:
     * - {@link RuntimeException} if closing the window or switching to another window fails.
     */
    public static void closeCurrentWindow() {

        WebDriver driver = DriverManager.getDriver();

        String currentWindow = driver.getWindowHandle();

        try {
            Set<String> allWindows = driver.getWindowHandles();

            log.info("Current window: {}", currentWindow);
            log.info("Total windows before close: {}", allWindows.size());

            // Close current window
            driver.close();

            log.info("Closed current window successfully");

            // Switch to another available window
            for (String window : allWindows) {
                if (!window.equals(currentWindow)) {
                    driver.switchTo().window(window);
                    log.info("Switched to window: {}", window);
                    return;
                }
            }

            log.warn("No other window found after closing");

        } catch (Exception e) {
            log.error("Failed to close current window", e);
            throw new RuntimeException("❌ Unable to close current window");
        }
    }

    /**
     * Closes the current browser window if multiple windows are present.
     * <br>
     * This method retrieves the current WebDriver instance, checks the number
     * of open browser windows, and safely closes the current window only
     * if more than one window is open. If there is only one window, the
     * method logs a warning and does not perform any action.
     * <br>
     * This prevents unintended termination of the browser session when only
     * a single window is active.
     */
    public static void closeCurrentWindowSafely() {

        WebDriver driver = DriverManager.getDriver();

        Set<String> windows = driver.getWindowHandles();

        if (windows.size() <= 1) {
            log.warn("Only one window present. Not closing.");
            return;
        }

        closeCurrentWindow();
    }

    /**
     * Accepts a browser alert if present.
     * This method attempts to switch to the active browser alert and accept it.
     * The method retries up to three times in case of transient issues or
     * an unhandled alert. If no alert is present or the operation fails after
     * three attempts, an exception is thrown.
     * <p>
     * Logging provides additional information about the workflow.<br>
     * - Logs the alert text before accepting it.<br>
     * - Logs warnings if no alert is present or if an unhandled alert is encountered.<br>
     * - Logs errors and additional details in case of unexpected exceptions.<br>
     *
     * @throws RuntimeException if unable to accept the alert after three attempts.
     */
    public static void acceptAlert() {

        int attempts = 0;

        while (attempts < 3) {
            try {
                Alert alert = DriverManager.getDriver().switchTo().alert();

                log.info("Accepting alert with text: {}", alert.getText());

                alert.accept();

                log.info("Alert accepted successfully");
                return;

            } catch (NoAlertPresentException e) {
                log.warn("No alert present to accept");

            } catch (UnhandledAlertException e) {
                log.warn("Unhandled alert, retrying accept");

            } catch (Exception e) {
                log.error("acceptAlert failed", e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to accept alert");
    }

    /**
     * Attempts to dismiss an active JavaScript alert in the current web driver context.
     * The method retries up to 3 times if the operation fails due to an `UnhandledAlertException`
     * or other exceptions. If no alert is present or the retries are exhausted, an appropriate
     * warning is logged, and a runtime exception is thrown.<br>
     * <br>
     * Behavior:
     * <br>
     * - Switches to the active alert using the WebDriver's alert handling capabilities.<br>
     * - Logs the alert text (if present) and dismisses the alert.<br>
     * - Retries dismissing the alert up to 3 times if it encounters issues.<br>
     * - Throws a `RuntimeException` if unable to dismiss the alert after 3 attempts.<br>
     * <br>
     * Exceptions handled internally:
     * <br>
     * - `NoAlertPresentException`: Triggered when there is no active alert to handle.<br>
     * - `UnhandledAlertException`: Triggered when the alert is in a state that temporarily<br>
     * prevents it from being dismissed.<br>
     * - Generic `Exception`: Captures other unexpected errors during the dismissal process.<br>
     * <p>
     * Logging:
     * <br>
     * - Logs the text of the alert being dismissed.<br>
     * - Logs information, warnings, or errors depending on the result of each attempt.
     * <br>
     * Throws:
     * <br>
     * - `RuntimeException` if the alert cannot be dismissed after 3 attempts.<br>
     */
    public static void dismissAlert() {

        int attempts = 0;

        while (attempts < 3) {
            try {
                Alert alert = DriverManager.getDriver().switchTo().alert();

                log.info("Dismissing alert with text: {}", alert.getText());

                alert.dismiss();

                log.info("Alert dismissed successfully");
                return;

            } catch (NoAlertPresentException e) {
                log.warn("No alert present to dismiss");

            } catch (UnhandledAlertException e) {
                log.warn("Unhandled alert, retrying dismiss");

            } catch (Exception e) {
                log.error("dismissAlert failed", e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to dismiss alert");
    }

    /**
     * Retrieves the text content of a currently displayed alert box within the web
     * driver session. Attempts to fetch the alert text up to three times before
     * throwing a runtime exception if no alert is present or an error occurs.
     *
     * @return The text content of the alert, if successfully retrieved.
     * @throws RuntimeException If unable to retrieve the alert text after multiple attempts.
     */
    public static String getAlertText() {

        int attempts = 0;

        while (attempts < 3) {
            try {
                Alert alert = DriverManager.getDriver().switchTo().alert();

                String text = alert.getText();

                log.info("Alert text fetched: {}", text);

                return text;

            } catch (NoAlertPresentException e) {
                log.warn("No alert present to get text");

            } catch (Exception e) {
                log.error("getAlertText failed", e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to get alert text");
    }

    /**
     * Sends the specified text value to an alert if it is present and interactable.
     * The method attempts to locate and interact with the alert up to three times
     * before throwing a RuntimeException if unsuccessful.
     *
     * @param value the text value to be sent to the alert
     *              (e.g., for a prompt alert that accepts text input)
     * @throws RuntimeException if the text cannot be sent to the alert after three attempts
     */
    public static void sendKeysToAlert(String value) {

        int attempts = 0;

        while (attempts < 3) {
            try {
                Alert alert = DriverManager.getDriver().switchTo().alert();

                log.info("Sending text '{}' to alert", value);

                alert.sendKeys(value);

                log.info("Text sent to alert successfully");
                return;

            } catch (NoAlertPresentException e) {
                log.warn("No alert present to send keys");

            } catch (ElementNotInteractableException e) {
                log.warn("Alert not interactable (maybe not prompt)");

            } catch (Exception e) {
                log.error("sendKeysToAlert failed", e);
            }

            attempts++;
        }

        throw new RuntimeException("❌ Unable to send keys to alert");
    }

    /**
     * Waits for an alert to be present within a specified timeout duration.
     * If an alert is detected, logs the presence of the alert and returns true.
     * If no alert is detected within the timeout, logs a warning and returns false.
     *
     * @return true if the alert is present within the timeout period, false otherwise.
     */
    public static boolean waitForAlert() {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            wait.until(ExpectedConditions.alertIsPresent());

            log.info("Alert is present");

            return true;

        } catch (TimeoutException e) {
            log.warn("Alert not present within timeout");
            return false;
        }
    }

    /**
     * Scrolls the current browser window to the top of the page using JavaScript execution.
     * <br>
     * The method utilizes the JavaScriptExecutor to execute a script that scrolls
     * the window to the top position (x = 0, y = 0). If an exception occurs during
     * the operation, it logs an error message and rethrows the exception as a RuntimeException.
     * <br>
     * This method is useful for scenarios where automated scrolling to the top of the page
     * is required in web automation tests.
     * <br>
     * Throws:<br>
     * - RuntimeException: If the scrolling operation fails.
     */
    public static void scrollToTop() {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("window.scrollTo(0, 0);");

            log.info("Scrolled to TOP of the page");

        } catch (Exception e) {
            log.error("scrollToTop failed", e);
            throw new RuntimeException("❌ Unable to scroll to top");
        }
    }

    /**
     * Scrolls the current page to the bottom using JavaScript execution.
     * This method utilizes the {@code JavascriptExecutor} to programmatically scroll
     * to the bottom of the page represented by the {@code document.body.scrollHeight}.
     * <br>
     * If the operation fails, an error is logged and a {@code RuntimeException} is thrown.
     *
     * @throws RuntimeException if the scroll operation is unsuccessful.
     */
    public static void scrollToBottom() {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

            log.info("Scrolled to BOTTOM of the page");

        } catch (Exception e) {
            log.error("scrollToBottom failed", e);
            throw new RuntimeException("❌ Unable to scroll to bottom");
        }
    }

    /**
     * Scrolls the current browser window by the specified number of pixels along the X and Y axes.
     *
     * @param x the number of horizontal pixels to scroll. Positive values scroll to the right, and negative values scroll to the left.
     * @param y the number of vertical pixels to scroll. Positive values scroll down, and negative values scroll up.
     * @throws RuntimeException if the scrolling operation fails.
     */
    public static void scrollByPixel(int x, int y) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);

            log.info("Scrolled by pixels → X: {}, Y: {}", x, y);

        } catch (Exception e) {
            log.error("scrollByPixel failed", e);
            throw new RuntimeException("❌ Unable to scroll by pixels");
        }
    }

    /**
     * Scrolls the webpage horizontally by the specified number of pixels.
     *
     * @param x the number of pixels to scroll horizontally. Positive values scroll to the right,
     *          while negative values scroll to the left.
     * @throws RuntimeException if the scrolling operation fails.
     */
    public static void scrollHorizontally(int x) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("window.scrollBy(arguments[0], 0);", x);

            log.info("Scrolled horizontally by X: {}", x);

        } catch (Exception e) {
            log.error("scrollHorizontally failed", e);
            throw new RuntimeException("❌ Unable to scroll horizontally");
        }
    }

    /**
     * Adjusts the zoom level of the current web page by increasing it to the specified percentage.
     *
     * @param percentage the zoom level to set, represented as a percentage (e.g., 100 for 100%, 150 for 150%)
     */
    public static void zoomIn(int percentage) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("document.body.style.zoom='" + percentage + "%'");

            log.info("Zoomed IN to {}%", percentage);

        } catch (Exception e) {
            log.error("zoomIn failed", e);
            throw new RuntimeException("❌ Unable to zoom in");
        }
    }

    /**
     * Adjusts the zoom level of the web page by reducing it to the specified percentage.
     *
     * @param percentage the zoom-out level as an integer percentage. For example, passing 50 will zoom out to 50% of the original scale.
     *                   The value should be greater than 0 and less than or equal to 100.
     * @throws RuntimeException if an error occurs while executing the zoom-out operation.
     */
    public static void zoomOut(int percentage) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript("document.body.style.zoom='" + percentage + "%'");

            log.info("Zoomed OUT to {}%", percentage);

        } catch (Exception e) {
            log.error("zoomOut failed", e);
            throw new RuntimeException("❌ Unable to zoom out");
        }
    }

    /**
     * Scrolls the page until the specified element becomes visible or a maximum number of scroll attempts is reached.
     * If the element does not become visible within the specified attempts, an exception is thrown.
     *
     * @param locator The {@code By} locator of the element to scroll into view.
     *                This determines the target element whose visibility will be checked during the scrolling process.
     * @throws RuntimeException if the element does not become visible after the maximum scroll attempts.
     */
    public static void scrollUntilElementVisible(By locator) {

        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

        for (int i = 0; i < 10; i++) {

            if (isDisplayed(locator, WaitStrategy.NONE)) {
                log.info("Element became visible: {}", locator);
                return;
            }

            js.executeScript("window.scrollBy(0, 500)");
        }

        throw new RuntimeException("❌ Element not found after scrolling: " + locator);
    }

    /**
     * Refreshes the current page in the web browser using the WebDriver instance.
     * This method attempts to reload the current page and logs the status of the operation.
     * <br>
     * On successful execution, an informational log message is recorded.
     * If page refresh fails, an error is logged, and a RuntimeException is thrown.
     * <br>
     * Throws:<br>
     * - RuntimeException if the page cannot be refreshed due to any underlying issue.
     */
    public static void refreshPage() {

        try {
            DriverManager.getDriver().navigate().refresh();

            log.info("Page refreshed successfully");

        } catch (Exception e) {
            log.error("refreshPage failed", e);
            throw new RuntimeException("❌ Unable to refresh page");
        }
    }

    /**
     * Navigates the web driver to the previous page in the browser's history.
     * <br>
     * This method utilizes the driver's navigate().back() function to perform the operation.
     * It logs a success message if the navigation is successful or logs an error and
     * throws a RuntimeException in case of failure.
     * <br>
     * Exceptions:<br>
     * - Throws RuntimeException if an error occurs while attempting to navigate back.
     * <br>
     * Logs:<br>
     * - Logs an informational message upon successful navigation.<br>
     * - Logs an error message if the operation fails.
     */
    public static void navigateBack() {

        try {
            DriverManager.getDriver().navigate().back();

            log.info("Navigated BACK successfully");

        } catch (Exception e) {
            log.error("navigateBack failed", e);
            throw new RuntimeException("❌ Unable to navigate back");
        }
    }

    /**
     * Navigates the browser to the next page in the session history, if available.
     * <br>
     * This method leverages to navigate().forward() function from the WebDriver to
     * move to the next page in the browser's history. It logs a success message
     * upon successful navigation and throws a RuntimeException in case of any failure.
     * <br><br>
     * Exceptions are caught and logged with an appropriate error message.
     * <br><br>
     * Throws:<br>
     * - RuntimeException: If the forward navigation operation fails.
     */
    public static void navigateForward() {

        try {
            DriverManager.getDriver().navigate().forward();

            log.info("Navigated FORWARD successfully");

        } catch (Exception e) {
            log.error("navigateForward failed", e);
            throw new RuntimeException("❌ Unable to navigate forward");
        }
    }

    /**
     * Opens the specified URL in the current web driver instance.
     *
     * @param url the URL to be opened; must be a valid and well-formed URL string.
     * @throws RuntimeException if the URL cannot be opened due to an error.
     */
    public static void openUrl(String url) {

        try {
            DriverManager.getDriver().get(url);

            log.info("Opened URL: {}", url);

        } catch (Exception e) {
            log.error("openUrl failed for: {}", url, e);
            throw new RuntimeException("❌ Unable to open URL: " + url);
        }
    }

    /**
     * Retrieves the current URL of the active browser session.
     *
     * @return the current URL as a String
     * @throws RuntimeException if unable to retrieve the current URL
     */
    public static String getCurrentUrl() {

        try {
            String url = DriverManager.getDriver().getCurrentUrl();

            log.info("Current URL: {}", url);

            return url;

        } catch (Exception e) {
            log.error("getCurrentUrl failed", e);
            throw new RuntimeException("❌ Unable to get current URL");
        }
    }

    /**
     * Retrieves the title of the current page from the web driver.
     *
     * @return The title of the current page as a String.
     * @throws RuntimeException if an error occurs while retrieving the page title.
     */
    public static String getPageTitle() {

        try {
            String title = DriverManager.getDriver().getTitle();

            log.info("Page Title: {}", title);

            return title;

        } catch (Exception e) {
            log.error("getPageTitle failed", e);
            throw new RuntimeException("❌ Unable to get page title");
        }
    }

    /**
     * Opens the specified URL in the browser and waits until the page loading is complete.
     *
     * @param url the URL to be opened in the browser
     */
    public static void openUrlAndWait(String url) {

        DriverManager.getDriver().get(url);

        new WebDriverWait(
                DriverManager.getDriver(),
                Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
        ).until(
                webDriver -> Objects.equals(((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState"), "complete")
        );

        log.info("Page loaded completely: {}", url);
    }

    /**
     * Safely refreshes the current page by invoking the refresh operation and optionally
     * stabilizing the page's state by waiting for a title match.
     * <br>
     * The method first refreshes the current page using the `refreshPage` utility,
     * then optionally invokes a stabilization mechanism by waiting for the page's title
     * to match a specified condition (in this case, an empty string). This is useful in
     * scenarios where delays or asynchronous operations may require additional stabilization
     * after a page refresh.
     * <br>
     * Note: Ensure that the `refreshPage` and `waitForTitle` methods are correctly implemented
     * and handle any potential exceptions or timeouts appropriately to avoid unexpected behavior.
     */
    public static void safeRefresh() {

        refreshPage();

        waitForTitle(""); // optional stabilization
    }

    /**
     * Prints the current browser's information, including the URL and page title.
     * This method retrieves the current URL and page title of the active browser session
     * and logs them using the application's logging mechanism.
     * <br>
     * The following information is logged:<br>
     * - Current URL<br>
     * - Page Title
     */
    public static void printBrowserInfo() {

        log.info("URL: {}", getCurrentUrl());
        log.info("TITLE: {}", getPageTitle());
    }

    /**
     * Highlights a web element on the page by applying a red border and yellow background
     * using JavaScript. This method is useful for debugging or visually identifying elements.
     *
     * @param locator  The {@code By} locator used to find the element to highlight.
     * @param strategy The {@code WaitStrategy} used to determine the waiting mechanism
     *                 before locating the web element.
     * @throws RuntimeException if the element cannot be highlighted due to an error.
     */
    public static void highlightElement(By locator, WaitStrategy strategy) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, strategy);

            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript(
                    "arguments[0].style.border='3px solid red'; arguments[0].style.background='yellow';",
                    element
            );

            log.info("Highlighted element: {}", locator);

        } catch (Exception e) {
            log.error("highlightElement failed: {}", locator, e);
            throw new RuntimeException("❌ Unable to highlight element");
        }
    }

    /**
     * Removes any applied highlight (such as border or background styling) from the specified web element
     * located via the given locator and wait strategy.
     *
     * @param locator  the {@link By} locator used to find the web element.
     * @param strategy the wait strategy to use for waiting until the element is visible or interactable.
     */
    public static void removeHighlight(By locator, WaitStrategy strategy) {

        try {
            WebElement element = WaitUtil.waitForElement(locator, strategy);

            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            js.executeScript(
                    "arguments[0].style.border=''; arguments[0].style.background='';",
                    element
            );

            log.info("Removed highlight from element: {}", locator);

        } catch (Exception e) {
            log.error("removeHighlight failed: {}", locator, e);
            throw new RuntimeException("❌ Unable to remove highlight");
        }
    }

    /**
     * Highlights a web element located by the given locator, waits for the specified
     * amount of time, and then removes the highlight.
     *
     * @param locator  The locator used to identify the web element.
     * @param strategy The waiting strategy to ensure the element is ready for interaction.
     * @param millis   The number of milliseconds to wait after highlighting the element.
     */
    public static void highlightAndWait(By locator, WaitStrategy strategy, int millis) {

        try {
            highlightElement(locator, strategy);

            Thread.sleep(millis);

            removeHighlight(locator, strategy);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Applies a blink effect on the identified web element by temporarily changing its background color.
     *
     * @param locator  The locator used to identify the element to be blinked.
     * @param strategy The waiting strategy to locate and handle the element before applying the effect.
     */
    public static void blinkElement(By locator, WaitStrategy strategy) {

        WebElement element = WaitUtil.waitForElement(locator, strategy);

        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

        for (int i = 0; i < 3; i++) {
            js.executeScript("arguments[0].style.background='yellow'", element);
            js.executeScript("arguments[0].style.background=''", element);
        }

        log.info("Blink effect applied on: {}", locator);
    }

    /**
     * Verifies if the text of a web element located by the given locator matches the expected text.
     *
     * @param locator  The locator used to find the web element whose text needs to be verified.
     * @param expected The expected text value to be compared with the actual text of the web element.
     * @param strategy The wait strategy to be applied while waiting for the web element to appear.
     * @return true if the actual text of the located element equals the expected text after trimming;
     * false otherwise or if an exception occurs during the process.
     */
    public static boolean verifyTextEquals(By locator, String expected, WaitStrategy strategy) {

        try {
            String actual = WaitUtil.waitForElement(locator, strategy).getText();

            log.info("Verifying TEXT EQUALS | Expected: '{}' | Actual: '{}'", expected, actual);

            return actual.trim().equals(expected.trim());

        } catch (Exception e) {
            log.error("verifyTextEquals failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies if the text of the web element located by the specified {@code locator} contains the {@code expected} text
     * after applying the given {@code strategy} for waiting.
     *
     * @param locator  The {@code By} locator used to find the target web element.
     * @param expected The expected text that should be contained in the element's actual text.
     * @param strategy The {@code WaitStrategy} to be employed for locating and waiting for the element.
     * @return {@code true} if the actual text of the found element contains the expected text; {@code false} otherwise.
     */
    public static boolean verifyTextContains(By locator, String expected, WaitStrategy strategy) {

        try {
            String actual = WaitUtil.waitForElement(locator, strategy).getText();

            log.info("Verifying TEXT CONTAINS | Expected: '{}' | Actual: '{}'", expected, actual);

            return actual.trim().contains(expected.trim());

        } catch (Exception e) {
            log.error("verifyTextContains failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies that the specified attribute of a web element matches the expected value.
     *
     * @param locator   the locator used to find the web element
     * @param attribute the name of the attribute to verify
     * @param expected  the expected value of the specified attribute
     * @param strategy  the wait strategy used to wait for the element to be available
     * @return true if the actual value of the attribute matches the expected value, otherwise false
     */
    public static boolean verifyAttributeEquals(By locator, String attribute, String expected, WaitStrategy strategy) {

        try {
            String actual = WaitUtil.waitForElement(locator, strategy).getAttribute(attribute);

            log.info("Verifying ATTRIBUTE EQUALS | Attr: {} | Expected: '{}' | Actual: '{}'",
                    attribute, expected, actual);

            return actual != null && actual.trim().equals(expected.trim());

        } catch (Exception e) {
            log.error("verifyAttributeEquals failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies if a web element is present on the page based on the given locator.
     *
     * @param locator the locator used to identify the web element
     * @return true if the element is present, false otherwise
     */
    public static boolean verifyElementPresent(By locator) {

        try {
            boolean isPresent = !DriverManager.getDriver().findElements(locator).isEmpty();

            log.info("Verifying ELEMENT PRESENT: {} | Result: {}", locator, isPresent);

            return isPresent;

        } catch (Exception e) {
            log.error("verifyElementPresent failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies that the specified element is not present on the page.
     *
     * @param locator the locator of the element to check
     * @return true if the element is not present, false if the element is present or if an exception occurs
     */
    public static boolean verifyElementNotPresent(By locator) {

        try {
            boolean isNotPresent = DriverManager.getDriver().findElements(locator).isEmpty();

            log.info("Verifying ELEMENT NOT PRESENT: {} | Result: {}", locator, isNotPresent);

            return isNotPresent;

        } catch (Exception e) {
            log.error("verifyElementNotPresent failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies if the web element located by the specified locator is enabled.
     * Utilizes the provided wait strategy to wait for the element's presence or state before checking.
     *
     * @param locator  the {@code By} locator used to identify the web element
     * @param strategy the {@code WaitStrategy} to be applied for waiting on the element
     * @return {@code true} if the element is enabled, {@code false} otherwise
     */
    public static boolean verifyElementEnabled(By locator, WaitStrategy strategy) {

        try {
            boolean enabled = WaitUtil.waitForElement(locator, strategy).isEnabled();

            log.info("Verifying ELEMENT ENABLED: {} | Result: {}", locator, enabled);

            return enabled;

        } catch (Exception e) {
            log.error("verifyElementEnabled failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Verifies if the specified element is disabled on the web page based on the provided selector
     * and wait strategy.
     *
     * @param locator  the locator to identify the web element
     * @param strategy the wait strategy to apply before checking the element's state
     * @return true if the element is disabled, false otherwise or if an error occurs
     */
    public static boolean verifyElementDisabled(By locator, WaitStrategy strategy) {

        try {
            boolean disabled = !WaitUtil.waitForElement(locator, strategy).isEnabled();

            log.info("Verifying ELEMENT DISABLED: {} | Result: {}", locator, disabled);

            return disabled;

        } catch (Exception e) {
            log.error("verifyElementDisabled failed for: {}", locator, e);
            return false;
        }
    }

    /**
     * Waits for the current web page to fully load by monitoring its document.readyState.
     * The method repeatedly checks the page load state until it equals "complete",
     * indicating that the page has finished loading.
     * <br>
     * If the page does not load within the configured explicit wait timeout,
     * a RuntimeException will be thrown.
     * <br>
     * The method relies on WebDriver, JavascriptExecutor, and the explicit wait duration
     * defined by the application framework settings.
     * <br>
     * Logging is used to indicate the success or failure of the page load operation.
     * In case of failure, an error is logged and the method throws a RuntimeException.
     * <br><br>
     * Preconditions:<br>
     * - The WebDriver instance must be initialized and available through DriverManager.<br>
     * - FrameworkConstants.EXPLICIT_WAIT must be properly set as a duration in seconds.<br>
     * <br>
     * Postconditions:<br>
     * - If successful, the method guarantees that the page has completely loaded.<br>
     * - If unsuccessful, the method will terminate by throwing a RuntimeException.<br>
     * <br>
     * Exceptions:<br>
     * - RuntimeException if the page does not load within the defined wait time.<br>
     */
    public static void waitForPageLoad() {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            wait.until(driver ->
                    Objects.equals(((JavascriptExecutor) driver)
                            .executeScript("return document.readyState"), "complete")
            );

            log.info("Page load completed successfully");

        } catch (Exception e) {
            log.error("waitForPageLoad failed", e);
            throw new RuntimeException("❌ Page did not load completely");
        }
    }

    /**
     * Waits for all active AJAX calls to complete using jQuery.
     * <p>
     * This method uses a WebDriverWait with a configurable timeout to ensure
     * that no active AJAX requests are in progress before proceeding. It relies
     * on the presence of jQuery on the page for detecting active AJAX requests.
     * If jQuery is not present or an exception occurs, a warning is logged and
     * the method execution is skipped.
     * <br>
     * Handles potential exceptions gracefully, logging a warning in case of any
     * failure or if jQuery is unavailable.
     */
    public static void waitForAjaxComplete() {

        try {
            WebDriverWait wait = new WebDriverWait(
                    DriverManager.getDriver(),
                    Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT)
            );

            wait.until(driver -> {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                return (Boolean) js.executeScript("return jQuery.active == 0");
            });

            log.info("AJAX calls completed");

        } catch (Exception e) {
            log.warn("AJAX wait skipped or failed (jQuery may not exist)");
        }
    }

    /**
     * Waits for the DOM (Document Object Model) to reach a stable "complete" state.
     * This method checks the `document.readyState` using JavaScript execution
     * and waits until the state is "complete", indicating that the page has finished
     * loading all resources. An additional stabilization buffer is added after the
     * main check to ensure that the DOM is fully stable.
     * <br>
     * The method uses an explicit wait mechanism with a timeout defined in the
     * application framework constants. If the DOM does not become stable within
     * the specified time, an error is logged.
     * <br>
     * Any interruption during the stabilization process will properly reset the
     * thread's interrupted status. Any exceptions during execution are logged for
     * debugging purposes.
     * <br>
     * This utility ensures that operations dependent on a fully loaded and stable
     * DOM are not executed prematurely.
     * <br><br>
     * Thread safety: This method is not thread-safe and assumes a single-threaded
     * execution context for the WebDriver instance.
     */
    public static void waitForDomStable() {

        try {
            WebDriver driver = DriverManager.getDriver();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(FrameworkConstants.EXPLICIT_WAIT));

            wait.until(d -> {
                String state = (String) js.executeScript("return document.readyState");
                assert state != null;
                return state.equals("complete");
            });

            // extra stabilization buffer
            Thread.sleep(500);

            log.info("DOM is stable");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("waitForDomStable failed", e);
        }
    }

    /**
     * Executes the provided JavaScript code in the context of the current browser session.
     *
     * @param script the JavaScript code to be executed as a String
     * @param args   optional arguments to be passed to the script
     * @return the result of the script execution, which can be of various types depending on the script's output,
     * or null if the script does not return a value
     * @throws RuntimeException if there is an error during script execution
     */
    public static Object executeJS(String script, Object... args) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            Object result = js.executeScript(script, args);

            log.info("Executed JS script successfully");

            return result;

        } catch (Exception e) {
            log.error("executeJS failed: {}", script, e);
            throw new RuntimeException("❌ JS execution failed");
        }
    }

    /**
     * Executes an asynchronous JavaScript code snippet in the context of the currently loaded web page.
     * The method leverages the WebDriver's JavascriptExecutor to execute the provided script.
     *
     * @param script The JavaScript code to be executed asynchronously. The script must contain a callback function
     *               that signals completion of execution.
     * @param args   Optional arguments to be passed to the JavaScript code. These arguments can be accessed within
     *               the script's execution context.
     * @return The result of the script execution, as returned by the callback in the JavaScript code.
     * The type of the result depends on the value returned from the script.
     * @throws RuntimeException If the script execution fails, an exception is thrown with an error message.
     */
    public static Object executeAsyncJS(String script, Object... args) {

        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();

            Object result = js.executeAsyncScript(script, args);

            log.info("Executed Async JS successfully");

            return result;

        } catch (Exception e) {
            log.error("executeAsyncJS failed: {}", script, e);
            throw new RuntimeException("❌ Async JS execution failed");
        }
    }

    /**
     * Waits for the web page to be fully ready by ensuring the following conditions are met:<br>
     * 1. The page load process is complete.<br>
     * 2. The DOM has reached a stable state.<br>
     * 3. All ongoing AJAX requests have been completed.<br>
     */
    public static void waitForPageReady() {
        waitForPageLoad();
        waitForDomStable();
        waitForAjaxComplete();
    }


}

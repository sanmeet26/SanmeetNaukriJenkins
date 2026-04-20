package com.framework.base;

import com.framework.driver.DriverManager;
import com.framework.enums.WaitStrategy;
import com.framework.utils.ElementActions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * @author Harshal.Thitame
 * @implNote BasePage acts as a wrapper over ElementActions.
 * It provides reusable methods for all Page Objects.
 */

public class BasePage {

    private final Logger log = LogManager.getLogger(BasePage.class);

    // ================= CLICK =================

    protected void click(By locator, WaitStrategy strategy) {
        ElementActions.click(locator, strategy);
    }

    // ================= SEND KEYS =================

    protected void sendKeys(By locator, String value, WaitStrategy strategy) {
        ElementActions.sendKeys(locator, value, strategy);
    }

    protected void jsSendKeys(By locator, String value) {
        ElementActions.jsSendKeys(locator, value);
    }

    // ================= GET TEXT =================

    protected String getText(By locator, WaitStrategy strategy) {
        return ElementActions.getText(locator, strategy);
    }

    // ================= VERIFICATIONS =================

    protected boolean isDisplayed(By locator, WaitStrategy strategy) {
        return ElementActions.isDisplayed(locator, strategy);
    }

    protected boolean isEnabled(By locator, WaitStrategy strategy) {
        return ElementActions.isEnabled(locator, strategy);
    }

    protected boolean verifyTextEquals(By locator, String expected, WaitStrategy strategy) {
        return ElementActions.verifyTextEquals(locator, expected, strategy);
    }

    protected boolean verifyTextContains(By locator, String expected, WaitStrategy strategy) {
        return ElementActions.verifyTextContains(locator, expected, strategy);
    }

    protected boolean verifyAttributeEquals(By locator, String attribute, String expected, WaitStrategy strategy) {
        return ElementActions.verifyAttributeEquals(locator, attribute, expected, strategy);
    }

    // ================= DROPDOWN =================

    protected void selectByVisibleText(By locator, String text, WaitStrategy strategy) {
        ElementActions.selectByVisibleText(locator, text, strategy);
    }

    protected void selectByValue(By locator, String value, WaitStrategy strategy) {
        ElementActions.selectByValue(locator, value, strategy);
    }

    protected void selectByIndex(By locator, int index, WaitStrategy strategy) {
        ElementActions.selectByIndex(locator, index, strategy);
    }

    // ================= ALERT =================

    protected void acceptAlert() {
        ElementActions.acceptAlert();
    }

    protected void dismissAlert() {
        ElementActions.dismissAlert();
    }

    protected String getAlertText() {
        return ElementActions.getAlertText();
    }

    protected void sendKeysToAlert(String text) {
        ElementActions.sendKeysToAlert(text);
    }

    // ================= WINDOW / FRAME =================

    protected void switchToFrame(By locator, WaitStrategy strategy) {
        ElementActions.switchToFrame(locator, strategy);
    }

    protected void switchToDefaultContent() {
        ElementActions.switchToDefaultContent();
    }

    protected void switchToParentFrame() {
        ElementActions.switchToParentFrame();
    }

    protected void switchToWindow(String title) {
        ElementActions.switchToWindow(title);
    }

    protected void closeCurrentWindow() {
        ElementActions.closeCurrentWindow();
    }

    // ================= NAVIGATION =================

    protected void openUrl(String url) {
        ElementActions.openUrl(url);
    }

    protected void refreshPage() {
        ElementActions.refreshPage();
    }

    protected void navigateBack() {
        ElementActions.navigateBack();
    }

    protected void navigateForward() {
        ElementActions.navigateForward();
    }

    protected String getCurrentUrl() {
        return ElementActions.getCurrentUrl();
    }

    protected String getPageTitle() {
        return ElementActions.getPageTitle();
    }

    // ================= WAIT HELPERS =================

    protected void waitForPageLoad() {
        ElementActions.waitForPageLoad();
    }

    protected void waitForAjaxComplete() {
        ElementActions.waitForAjaxComplete();
    }

    protected void waitForDomStable() {
        ElementActions.waitForDomStable();
    }

    protected boolean waitForTextToBePresent(By locator, String text) {
        return ElementActions.waitForTextToBePresent(locator, text);
    }

    protected boolean waitForAttributeToContain(By locator, String attribute, String value) {
        return ElementActions.waitForAttributeToContain(locator, attribute, value);
    }


    protected void checkIfCloseButtonVisible(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(3));
            wait.until(driver -> driver.findElement(locator).isDisplayed());
            click(locator, WaitStrategy.VISIBLE);
            log.info("Close button is visible and click on it");
        } catch (Exception e) {
            log.info("Close button is not visible");
        }
    }
}
package com.framework.pages;

import com.framework.base.BasePage;
import com.framework.enums.WaitStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : LoginPage
 * Description :
 * Page Object class representing the Login page of the Naukri application.
 * <p>
 * Encapsulates all UI interactions required to authenticate a user, including:
 * - Opening the login dialog from the home page
 * - Entering credentials (username and password)
 * - Submitting the login form
 * - Verifying successful login via page title
 * - Navigating to the user profile from the post-login menu
 * <p>
 * Design Notes:
 * - Extends {@link BasePage} to inherit reusable WebDriver interaction helpers
 * (click, sendKeys, waitForPageLoad, etc.).
 * - All locators are declared as {@code private final} fields to keep them
 * encapsulated and prevent external modification.
 * - Password values are never logged in plain text — masked as "****" for security.
 * <p>
 * Typical Usage Flow:
 * <pre>
 *   LoginPage loginPage = new LoginPage();
 *   loginPage.clickHomeLoginButton();
 *   loginPage.enterUsername("user@example.com");
 *   loginPage.enterPassword("secret");
 *   loginPage.clickLoginButton();
 *   Assert.assertTrue(loginPage.isLoggedInSuccessfully());
 * </pre>
 * =================================================================================================
 */
public class LoginPage extends BasePage {

    private static final Logger log = LogManager.getLogger(LoginPage.class);

    private final By homeLoginButton = By.xpath("//a[@id='login_Layer']");
    private final By usernameText = By.xpath("//input[@placeholder='Enter your active Email ID / Username']");
    private final By passwordText = By.xpath("//input[@placeholder='Enter your password']");
    private final By loginButton = By.xpath("//button[@type='submit']");
    private final By profileButton = By.xpath("//img[@alt='naukri user profile img']");
    private final By viewAndUpdateProfileButton = By.xpath("//a[normalize-space()='View & Update Profile']");

    public LoginPage() {
        super();
    }

    // ================= LOGIN =================

    /**
     * Clicks the Login button on the home/landing page to open the login dialog.
     * Waits until the element is clickable before interacting.
     */
    public void clickHomeLoginButton() {
        log.info("Clicking Home Login button to open login dialog");
        click(homeLoginButton, WaitStrategy.CLICKABLE);
    }

    /**
     * Enters the provided username into the username/email input field.
     * Waits until the field is visible before typing.
     *
     * @param username the email ID or username to enter
     */
    public void enterUsername(String username) {
        log.info("Entering username: {}", username);
        sendKeys(usernameText, username, WaitStrategy.VISIBLE);
    }

    /**
     * Enters the provided password into the password input field.
     * Waits until the field is visible before typing.
     * The password is intentionally masked in logs for security.
     *
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        log.info("Entering password: ****");
        sendKeys(passwordText, password, WaitStrategy.VISIBLE);
    }

    /**
     * Clicks the Submit/Login button to trigger form submission.
     * Waits until the button is clickable before interacting.
     */
    public void clickLoginButton() {
        log.info("Clicking Login (submit) button");
        click(loginButton, WaitStrategy.CLICKABLE);
    }

    // ================= PROFILE =================

    /**
     * Clicks the user profile avatar/icon that appears after successful login,
     * typically opening the account dropdown menu.
     * Waits until the element is clickable before interacting.
     */
    public void clickProfileButton() {
        log.info("Clicking user profile icon to open account menu");
        click(profileButton, WaitStrategy.CLICKABLE);
    }

    /**
     * Clicks the "View & Update Profile" link from the account dropdown menu,
     * navigating the user to their profile page.
     * Waits until the element is clickable before interacting.
     */
    public void clickViewAndUpdateProfileButton() {
        log.info("Clicking 'View & Update Profile' link");
        click(viewAndUpdateProfileButton, WaitStrategy.CLICKABLE);
    }

    /**
     * Verifies whether the login was successful by checking the current page title.
     * Waits for the page to fully load before reading the title.
     *
     * @return {@code true} if the page title contains "home" (case-insensitive),
     * indicating a successful redirect to the home/dashboard page;
     * {@code false} otherwise
     */
    public boolean isLoggedInSuccessfully() {
        log.info("Verifying login success via page title");
        waitForPageLoad();
        String title = getPageTitle().toLowerCase();
        boolean isLoggedIn = title.contains("home");
        log.info("Login verification result — page title: '{}' | logged in: {}", title, isLoggedIn);
        return isLoggedIn;
    }
}

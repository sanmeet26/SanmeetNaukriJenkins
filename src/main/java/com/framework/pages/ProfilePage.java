package com.framework.pages;

import com.framework.base.BasePage;
import com.framework.constants.FrameworkConstants;
import com.framework.enums.WaitStrategy;
import com.framework.utils.ElementActions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

/**
 * @author Harshal.Thitame
 * @implNote Class Name: ProfilePage
 * <p>
 * Description:<br>
 * Page Object class representing the User Profile page of the Naukri application.
 * <p>
 * Encapsulates all UI interactions available on the profile page, including:
 * - Verifying the profile page has loaded successfully
 * - Uploading / updating the user's resume file
 * - Editing the resume headline
 * - Editing key skills
 * - Editing the profile summary
 * - Editing the career profile section
 * - Saving changes and closing edit drawers
 * <p>
 * Design Notes:
 * - Extends {@link BasePage} to inherit reusable WebDriver interaction helpers
 * (click, sendKeys, waitForPageLoad, etc.).
 * - All locators are declared as {@code private final} fields to keep them
 * encapsulated and prevent external modification.
 * - Resume upload path is sourced from {@link FrameworkConstants#RESUME_PATH}
 * to keep the file path centralised and configurable.
 * <p>
 * Typical Usage Flow:
 * <pre>
 *   ProfilePage profilePage = new ProfilePage();
 *   Assert.assertTrue(profilePage.isProfilePageLoaded());
 *   profilePage.clickEditResumeHeadlineButton();
 *   profilePage.enterResumeHeadline("Senior QA Engineer | Selenium | Java");
 *   profilePage.clickSaveButton();
 * </pre>
 *
 */
public class ProfilePage extends BasePage {

    private static final Logger log = LogManager.getLogger(ProfilePage.class);

    private final By updateResumeButton = By.xpath("//input[@value='Update resume']");
    private final By editPersonalDetailsButton = By.xpath("//em[contains(@class,'icon edit')]");
    private final By editResumeHeadlineButton = By.xpath("//span[contains(text(),'headline')]/following-sibling::span[@class='edit icon']");
    private final By resumeHeadlineTextarea = By.xpath("//textarea[@id='resumeHeadlineTxt']");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By closeButton = By.xpath("//div[@class='lightbox profileEditDrawer profileUpdatedProLayer model_open flipOpen']//span[@class='icon'][normalize-space()='CrossLayer']");
    private final By editKeySkillsButton = By.xpath("//span[contains(text(),'skills')]/following-sibling::span[@class='edit icon']");
   
    public ProfilePage() {
        super();
    }

    // ==================== CHECK PAGE LOADED =====================
    public boolean isProfilePageLoaded() {
        log.info("Verifying Profile page is loaded via page title");
        waitForPageLoad();
        String title = getPageTitle().toLowerCase();
        boolean isLoaded = title.contains("profile");
        log.info("Profile page load check — title: '{}' | loaded: {}", title, isLoaded);
        return isLoaded;
    }

    // ==================== UPDATE PERSONAL DETAILS =====================

    public void updatePersonalDetails() {
        click(editPersonalDetailsButton, WaitStrategy.VISIBLE);
    }

    // ==================== UPDATE RESUME =====================

    public void updateResume() {
        log.info("Uploading resume from path: {}", FrameworkConstants.RESUME_PATH);
        ElementActions.uploadFile(updateResumeButton, FrameworkConstants.RESUME_PATH, WaitStrategy.VISIBLE);
        log.info("Resume upload triggered successfully");
    }

    // ==================== EDIT HEADLINE =====================

    public void clickEditResumeHeadlineButton() {
        log.info("Clicking edit icon for Resume Headline section");
        click(editResumeHeadlineButton, WaitStrategy.CLICKABLE);
    }

    public void enterResumeHeadline(String headline) {
        log.info("Entering resume headline: {}", headline);
        sendKeys(resumeHeadlineTextarea, headline, WaitStrategy.VISIBLE);
    }

    public void clickSaveButton() {
        log.info("Clicking Save button to persist changes");
        click(saveButton, WaitStrategy.CLICKABLE);
    }

    public void clickCloseButton() {
        checkIfCloseButtonVisible(closeButton);
        log.info("Clicking Close button to dismiss edit drawer");
//        click(closeButton, WaitStrategy.CLICKABLE);
    }

    // ==================== EDIT KEY SKILLS =====================

    public void clickEditKeySkillsButton() {
        log.info("Clicking edit icon for Key Skills section");
        click(editKeySkillsButton, WaitStrategy.CLICKABLE);
    }


}

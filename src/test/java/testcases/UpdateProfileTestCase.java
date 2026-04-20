package testcases;

import base.BaseTest;
import com.framework.pages.LoginPage;
import com.framework.pages.ProfilePage;
import dataprovider.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;


public class UpdateProfileTestCase extends BaseTest {

    @Test
    public void loginNaukri() {
        LoginPage loginPage = new LoginPage();
        ProfilePage profilePage = new ProfilePage();

        //Login to naukri
        loginPage.clickHomeLoginButton();
        loginPage.enterUsername(TestData.USERNAME);
        loginPage.enterPassword(TestData.PASSWORD);
        loginPage.clickLoginButton();
        Assert.assertTrue(loginPage.isLoggedInSuccessfully());
        loginPage.clickProfileButton();
        loginPage.clickViewAndUpdateProfileButton();

        //Update profile
        Assert.assertTrue(profilePage.isProfilePageLoaded());
        profilePage.updatePersonalDetails();
        profilePage.clickSaveButton();
        profilePage.clickCloseButton();


        profilePage.updateResume();
        profilePage.clickEditResumeHeadlineButton();
//        profilePage.enterResumeHeadline(TestData.RESUME_HEADLINE);
        profilePage.clickSaveButton();
        profilePage.clickCloseButton();

        profilePage.clickEditKeySkillsButton();
        profilePage.clickSaveButton();
        profilePage.clickCloseButton();

    }
}

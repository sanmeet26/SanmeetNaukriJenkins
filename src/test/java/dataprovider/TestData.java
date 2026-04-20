package dataprovider;

import com.framework.utils.ConfigReader;

public final class TestData {


    // ===================== USER DATA =====================
    public static final String USERNAME = ConfigReader.getCredential("SANMEET_EMAIL");
    public static final String PASSWORD = ConfigReader.getCredential("SANMEET_PASSWORD");
    // ===================== RESUME DATA =====================
    public static final String RESUME_HEADLINE = "I am an Automation Test Engineer with 2+ years of experience in Selenium, Java, and test framework development for quality assurance.";
    public static final String PROFILE_SUMMARY = "I have 2+ years of experience as an Automation Test Engineer, specializing in Selenium, Java, and test framework development. Skilled in creating robust automated scripts, streamlining testing processes, and integrating them into CI/CD pipelines. I excel at ensuring software quality, identifying bugs early, and improving release efficiency. A team player with strong problem-solving skills, I look forward to opportunities where I can contribute to innovative testing solutions and enhance user experience in my next role.";

    private TestData() {

    }
}

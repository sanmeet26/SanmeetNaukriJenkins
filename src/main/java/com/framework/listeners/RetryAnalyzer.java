package com.framework.listeners;

import com.framework.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * @author Harshal.Thitame
 * @implNote =================================================================================================
 * Class Name : RetryAnalyzer
 * Description :
 * This class retries failed test cases based on retry count defined in constants.
 * <p>
 * =================================================================================================
 */

public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LogManager.getLogger();

    private int count = 0;

    @Override
    public boolean retry(ITestResult result) {

        if (count < FrameworkConstants.RETRY_COUNT) {
            count++;
            logger.warn("Retrying test '{}' — attempt {}/{}",
                    result.getMethod().getMethodName(), count, FrameworkConstants.RETRY_COUNT);
            return true;
        }

        logger.error("Test '{}' failed after {} retry attempt(s). No more retries.",
                result.getMethod().getMethodName(), FrameworkConstants.RETRY_COUNT);
        return false;
    }
}

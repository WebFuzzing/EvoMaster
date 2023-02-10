package org.evomaster.client.java.controller.api;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions used in the generated tests to handle browser operations with Selenium
 */
public class SeleniumEMUtils {

    public static boolean waitForPageToLoad(WebDriver driver, int timeoutSecond) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.of(timeoutSecond, ChronoUnit.SECONDS));

        //keep executing the given JS till it returns "true", when page is fully loaded and ready
        try {
            return wait.until((ExpectedCondition<Boolean>) input -> {
                String res = jsExecutor.executeScript("return document.readyState === 'complete';").toString();
                return Boolean.parseBoolean(res);
            });
        }catch (TimeoutException e){
            return false;
        }
    }

    public static void clickAndWaitPageLoad(WebDriver driver, String cssSelector){
        WebElement element = driver.findElement(By.cssSelector(cssSelector));
        element.click();
        //TODO can we do better here than waiting a hard-coded timeout?
        try{Thread.sleep(50);} catch (Exception e){}
        waitForPageToLoad(driver, 2);
        //TODO will need to check if JS executing in background
    }
}

package org.evomaster.test.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility functions used in the generated tests to handle browser operations with Selenium
 */
public class SeleniumEMUtils {

    public static final String TESTCONTAINERS_HOST = "host.testcontainers.internal";

    public static String combineBaseUrlAndUrlPath(String base, String path){

        Objects.requireNonNull(base);
        Objects.requireNonNull(path);

        if(base.endsWith("/") && path.startsWith("/")){
            return base + path.substring(1,path.length());
        } else if(!base.endsWith("/") && !path.startsWith("/")){
            return base + "/" + path;
        } else {
            return base + path;
        }
    }

    public static String validateAndGetUrlOfStartingPageForDocker(String base, String path, boolean modifyLocalHost){
        return validateAndGetUrlOfStartingPageForDocker(combineBaseUrlAndUrlPath(base,path),modifyLocalHost);
    }

    public static String validateAndGetUrlOfStartingPageForDocker(String url, boolean modifyLocalHost){
        if(url.isEmpty()){
            throw new IllegalArgumentException("Starting page is not defined");
        }
        /*
            hmmm... why did I use a URI instead of URL here???
            was it to avoid hostname resolving?
         */
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e){
            throw new IllegalArgumentException("Provided Home Page link is not a valid URL: " + e.getMessage());
        }

        int port = uri.getPort();
        if(port < 0){
            //infer from protocol
            String protocol = uri.getScheme();
            if("http".equals(protocol)){
                port = 80;
            } else if("https".equals(protocol)){
                port = 443;
            } else {
                throw new IllegalArgumentException("Cannot infer port number from url: " + url);
            }
        }

        //see https://www.testcontainers.org/modules/webdriver_containers/
        Testcontainers.exposeHostPorts(port);

        String host = uri.getHost();
        if(host == null){
            throw new IllegalArgumentException("Cannot infer host from url: " + url);
        }

        if(modifyLocalHost && uri.getHost().equalsIgnoreCase( "localhost")) {
            try {
                uri = new URI(
                        uri.getScheme().toLowerCase(Locale.US),
                        uri.getUserInfo(),
                        TESTCONTAINERS_HOST,
                        uri.getPort(),
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return uri.toString();
    }

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
        WebElement element;
        try {
            element = driver.findElement(By.cssSelector(cssSelector));
        } catch (NoSuchElementException e){
            throw new RuntimeException("Cannot locate element with '"+cssSelector+"'." +
                    "\nCurrent URL is: " + driver.getCurrentUrl() +
                    "\nCurrent page is: " + driver.getPageSource());
        }

        //TODO should make sure to scroll to it if not visible
        // https://stackoverflow.com/questions/45183797/element-not-interactable-exception-in-selenium-web-automation
        //if this throws an exception, could be issue with JS loading, or bug in EM about how we select elements
        element.click();

        //TODO can we do better here than waiting a hard-coded timeout?
        try{Thread.sleep(50);} catch (Exception e){}
        waitForPageToLoad(driver, 2);
        //TODO will need to check if JS executing in background
    }

    public static void goToPage(WebDriver driver, String pageURL, int timeoutSeconds){
        driver.get(pageURL);
        waitForPageToLoad(driver, timeoutSeconds);
    }
}

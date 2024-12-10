package org.evomaster.core.problem.webfrontend.service

import org.evomaster.test.utils.SeleniumEMUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.webfrontend.BrowserActionBuilder
import org.evomaster.core.problem.webfrontend.WebUserInteraction
import org.evomaster.core.remote.SutProblemException
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer


/**
 * Class used to control a browser.
 * This could be started for example via Docker.
 */
class BrowserController {

    private val  chrome : BrowserWebDriverContainer<*> =  BrowserWebDriverContainer()
        .withCapabilities(ChromeOptions())
        .withAccessToHost(true)
    private lateinit var  driver : RemoteWebDriver
    private lateinit var urlOfStartingPage : String


    /**
     * Might need to modify hostname, eg when dealing with browser running inside Docker
     */
    fun initUrlOfStartingPage(url: String, modifyLocalHost: Boolean) : String{
        urlOfStartingPage = SeleniumEMUtils.validateAndGetUrlOfStartingPageForDocker(url, modifyLocalHost)
        return urlOfStartingPage
    }

    fun startChromeInDocker(){
        try {
            if(!chrome.isRunning) {
                chrome.start() //TODO check if we need explicit stop, eg in JVM shutdown hook
            }
        }catch (e : Exception){
            LoggingUtil.getInfoLogger().error("It was not possible to start Chrome in Docker." +
                    " Make sure you have Docker installed, and that it is up and running, with a working internet connection",
            e)
            throw e
        }
        driver = RemoteWebDriver(chrome.seleniumAddress, ChromeOptions())
    }

    fun stopChrome(){
        chrome.stop()
    }

    fun cleanBrowser(){
        //TODO clean cookies
    }

    fun goToStartingPage(){
        try {
            driver.get(urlOfStartingPage)
        } catch (e :Exception){
            throw SutProblemException("Failed to open home page at '$urlOfStartingPage' : ${e.message}")
        }
        val timeoutSecs = 10 // first time can wait a bit, eg if need to download quite a few resources
        val loaded = SeleniumEMUtils.waitForPageToLoad(driver, timeoutSecs)
        if(!loaded){
            throw SutProblemException("Failed to load home page within $timeoutSecs seconds")
        }
    }

    fun computePossibleUserInteractions() : List<WebUserInteraction>{
        return BrowserActionBuilder.computePossibleUserInteractions(getCurrentPageSource())
    }

    fun getCurrentPageSource(): String {
        return driver.pageSource
    }

    fun getCurrentUrl(): String{
        return driver.currentUrl
    }

    fun clickAndWaitPageLoad(cssSelector: String){
        SeleniumEMUtils.clickAndWaitPageLoad(driver, cssSelector)
    }

    fun goBack(){
        driver.navigate().back()
        SeleniumEMUtils.waitForPageToLoad(driver,2)
    }
}
package org.evomaster.core.problem.webfrontend.service

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.webfrontend.BrowserActionBuilder
import org.evomaster.core.problem.webfrontend.WebUserInteraction
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BrowserWebDriverContainer
import java.net.MalformedURLException
import java.net.URI
import java.util.*


/**
 * Class used to control a browser.
 * This could be started for example via Docker.
 */
class BrowserController {

    companion object{
        const val TESTCONTAINERS_HOST = "host.testcontainers.internal"
    }


    private val  chrome : BrowserWebDriverContainer<*> =  BrowserWebDriverContainer()
        .withCapabilities(ChromeOptions())
        .withAccessToHost(true)
    private lateinit var  driver : RemoteWebDriver
    private lateinit var urlOfStartingPage : String


    /**
     * Might need to modify hostname, eg when dealing with browser running inside Docker
     */
    fun initUrlOfStartingPage(url: String, modifyLocalHost: Boolean) : String{
        if(url.isEmpty()){
            throw IllegalArgumentException("Starting page is not defined")
        }
        var uri = try {
            URI(url)
        } catch (e: MalformedURLException){
            throw IllegalArgumentException("Provided Home Page link is not a valid URL: ${e.message}")
        }
        //see https://www.testcontainers.org/modules/webdriver_containers/
        Testcontainers.exposeHostPorts(uri.port);

        if(modifyLocalHost && uri.host == "localhost") {
            uri = URI(
                uri.scheme.lowercase(Locale.US),
                uri.userInfo,
                TESTCONTAINERS_HOST,
                uri.port,
                uri.path,
                uri.query,
                uri.fragment
            )
        }
        urlOfStartingPage = uri.toString()
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

        driver.get(urlOfStartingPage)
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
}
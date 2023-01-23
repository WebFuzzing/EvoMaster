package org.evomaster.core.problem.webfrontend.service

import org.evomaster.core.problem.webfrontend.WebUserInteraction
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.BrowserWebDriverContainer

/**
 * Class used to control a browser.
 * This could be started for example via Docker.
 */
class BrowserController {

    var urlOfStartingPage : String = ""

   // val  chrome : BrowserWebDriverContainer<*> =  BrowserWebDriverContainer().withCapabilities(ChromeOptions())

    fun startChromeInDocker(){
        //TODO
    }

    fun goToStartingPage(){
        if(urlOfStartingPage.isEmpty()){
            throw IllegalStateException("Starting page is not defined")
        }
    //TODO
    }

    fun computePossibleUserInteractions() : List<WebUserInteraction>{
        return listOf() // TODO
    }
}
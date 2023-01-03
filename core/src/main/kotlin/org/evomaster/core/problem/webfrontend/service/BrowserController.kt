package org.evomaster.core.problem.webfrontend.service

import org.evomaster.core.problem.webfrontend.WebUserInteraction

/**
 * Class used to control a browser.
 * This could be started for example via Docker.
 */
class BrowserController {

    var urlOfStartingPage : String = ""

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
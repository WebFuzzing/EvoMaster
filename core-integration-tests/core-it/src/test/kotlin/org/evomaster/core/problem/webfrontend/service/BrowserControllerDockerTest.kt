package org.evomaster.core.problem.webfrontend.service

import org.evomaster.core.problem.webfrontend.UserActionType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication

internal class BrowserControllerDockerTest{

    companion object{

        val browser = BrowserController()
        val spring = SpringApplication.run(HttpStaticServer::class.java, "--server.port=0");

        @JvmStatic
        @BeforeAll
        fun beforeAll(): Unit {
            browser.startChromeInDocker()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(): Unit {
            browser.stopChrome()
            spring.close()
        }

        fun getPort(): Int {
            return (spring.environment.propertySources.get("server.ports").source as Map<*, *>)["local.server.port"] as Int
        }
    }

    @BeforeEach
    fun init(){
        browser.cleanBrowser()
    }


    @Test
    fun testBase(){
        browser.initUrlOfStartingPage("http://localhost:${getPort()}/frontend/base/index.html",true)
        browser.goToStartingPage()

        val actions = browser.computePossibleUserInteractions()
        assertEquals(0, actions.size)
    }

    @Test
    fun testAlinks(){

        browser.initUrlOfStartingPage("http://localhost:${getPort()}/frontend/alinks/index.html",true)
        browser.goToStartingPage()

        var actions = browser.computePossibleUserInteractions()
        assertEquals(1, actions.size)
        val homePage = browser.getCurrentUrl()
        assertTrue(homePage.endsWith("index.html"), homePage)
        val a = actions[0]
        assertEquals(UserActionType.CLICK, a.userActionType)

        browser.clickAndWaitPageLoad(a.cssSelector)
        val aPage = browser.getCurrentUrl()
        assertTrue(aPage.endsWith("a.html"), aPage)

        actions = browser.computePossibleUserInteractions()
        assertEquals(2, actions.size)
        val backHome = actions.first { it.cssSelector.contains("#backhome") }
        browser.clickAndWaitPageLoad(backHome.cssSelector)
        assertEquals(homePage, browser.getCurrentUrl())

        browser.clickAndWaitPageLoad(a.cssSelector)
        val tob = actions.first { it.cssSelector.contains("#tob") }
        browser.clickAndWaitPageLoad(tob.cssSelector)
        val bPage = browser.getCurrentUrl()
        assertTrue(bPage.endsWith("b.html"), bPage)
        actions = browser.computePossibleUserInteractions()
        assertEquals(0, actions.size)

        browser.goBack()
        assertEquals(aPage, browser.getCurrentUrl())
    }
}
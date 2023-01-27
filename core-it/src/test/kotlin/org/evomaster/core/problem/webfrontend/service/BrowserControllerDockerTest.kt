package org.evomaster.core.problem.webfrontend.service

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
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

}
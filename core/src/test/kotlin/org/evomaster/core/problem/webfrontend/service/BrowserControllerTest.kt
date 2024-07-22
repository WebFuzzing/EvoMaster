package org.evomaster.core.problem.webfrontend.service

import org.evomaster.test.utils.SeleniumEMUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BrowserControllerTest{

    @Test
    fun testInitUrlOfStartingPage(){

        val pre = "http://"
        val localhost = "localhost"
        val post = ":8080/frontend/base/index.html"

        val original = "$pre$localhost$post"
        assertEquals(original, BrowserController().initUrlOfStartingPage(original,false))

        val modified = "$pre${SeleniumEMUtils.TESTCONTAINERS_HOST}$post"
        assertEquals(modified, BrowserController().initUrlOfStartingPage(original,true))
    }
}
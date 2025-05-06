package org.evomaster.core.problem.rest.links

import bar.examples.it.spring.links.LinksApplication
import bar.examples.it.spring.links.LinksController
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.link.BackwardLinkReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LinksTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(LinksController())
        }
    }

    @Test
    fun testLink(){

        val pirTest = getPirToRest()

        val randomId = "f4rtgrfgbdfvg"

        val wrongPost = pirTest.fromVerbPath("post", "/api/auth/gdgferg")!!
        val get = pirTest.fromVerbPath("get", "/api/users/$randomId")!!
        val link = BackwardLinkReference(wrongPost.id, wrongPost.links[0].id)
        get.backwardLinkReference = link

        val x = createIndividual(listOf(wrongPost, get))
        var resGet = x.evaluatedMainActions()[1].result as RestCallResult

        assertEquals(404, resGet.getStatusCode())
        //no side-effects
        assertEquals(get.resolvedPath(), "/api/users/$randomId")


        val correctPost = pirTest.fromVerbPath("post", "/api/auth/${LinksApplication.secretCode}")!!
        val y = createIndividual(listOf(correctPost, get))
        resGet = y.evaluatedMainActions()[1].result as RestCallResult
        assertEquals(200, resGet.getStatusCode())
        //side-effects
        assertEquals(get.resolvedPath(), "/api/users/${LinksApplication.secretId}")
        assertEquals("OK", resGet.getBody())
    }

}
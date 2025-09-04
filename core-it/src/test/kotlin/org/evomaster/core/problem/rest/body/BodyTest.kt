package org.evomaster.core.problem.rest.body

import bar.examples.it.spring.body.BodyController
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BodyTest : IntegrationTestRestBase(){


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BodyController())
        }
    }

    @Test
    fun testEmptyObject() {

        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/body", jsonBodyPayload = "{}")!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        //there is chance randomized get right values for a E
        //assertEquals(400, res.getStatusCode())
        val payload = post.parameters.find { it is BodyParam } as BodyParam

        val data = payload.primaryGene()
        assertTrue(data is ObjectGene)
        val text = data.getValueAsRawString()

        val mapper = ObjectMapper()
        val tree = mapper.readTree(text)

        assertEquals(2, tree.size())
        assertTrue(tree.fields().asSequence().any{it.key == "rb"})
        assertTrue(tree.fields().asSequence().any{it.key == "ri"})
    }

    @Test
    fun testOnlyRequired(){

        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/body",
            jsonBodyPayload = """ 
                {
                    "rb": true,
                    "ri": 42
                }
            """.trimIndent())!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        assertEquals(200, res.getStatusCode())
        assertEquals("E", res.getBody())
    }
}
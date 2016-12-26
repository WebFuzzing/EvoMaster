package org.evomaster.core.problem.rest.service

import io.swagger.parser.SwaggerParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RestSamplerTest{

    @Test
    fun testCreateActions(){

        val swagger = SwaggerParser().read("/positive_integer_swagger.json")

        val sampler = RestSampler()
        sampler.createActions(swagger)

        val actions = sampler.seeAvailableActions()
        assertEquals(2, actions.size)
    }

}
package org.evomaster.core.problem.rest.service

import io.swagger.parser.SwaggerParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
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


    @Disabled("This is a bug in Swagger Core, reported at https://github.com/swagger-api/swagger-core/issues/2100")
    @Test
    fun testFeaturesServicesNull(){

        val swagger = SwaggerParser().read("/features_service_null.json")

        val sampler = RestSampler()
        sampler.createActions(swagger)

        val actions = sampler.seeAvailableActions()
        assertEquals(18, actions.size)
    }

    @Test
    fun testFeaturesServices(){

        val swagger = SwaggerParser().read("/features_service.json")

        val sampler = RestSampler()
        sampler.createActions(swagger)

        val actions = sampler.seeAvailableActions()
        assertEquals(18, actions.size)
    }

    @Test
    fun testScoutApi(){

        val swagger = SwaggerParser().read("/scout-api.json")

        val sampler = RestSampler()
        sampler.createActions(swagger)

        val actions = sampler.seeAvailableActions()
        assertEquals(49, actions.size)
    }

}
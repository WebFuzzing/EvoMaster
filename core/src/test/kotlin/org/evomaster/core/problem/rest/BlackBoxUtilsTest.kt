package org.evomaster.core.problem.rest

import io.mockk.every
import io.mockk.mockk
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class BlackBoxUtilsTest{

    @Test
    fun testTargetUrl(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.bbTargetUrl = target
        config.bbSwaggerUrl = "$target/swagger.json"

        assertEquals(target, BlackBoxUtils.targetUrl(config))
    }


    @Test
    fun testTargetUrlDifferentSwagger(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.bbTargetUrl = target
        config.bbSwaggerUrl = "http://192.168.1.42:12345/swagger.json"

        assertEquals(target, BlackBoxUtils.targetUrl(config))
    }


    @Test
    fun testTargetUrlMissing(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.problemType = EMConfig.ProblemType.REST
        config.bbTargetUrl = ""
        config.bbSwaggerUrl = "$target/swagger.json"

        val sampler = mockk<AbstractRestSampler>()
        every{sampler.schemaHolder} returns RestSchema( SchemaOpenAPI("",OpenAPI(), SchemaLocation.ofRemote(config.bbSwaggerUrl)))
        assertEquals(target, BlackBoxUtils.targetUrl(config, sampler))
    }

    @Test
    fun testTargetUrlMissingNoPort(){

        val target = "http://localhost"

        val config = EMConfig()
        config.problemType = EMConfig.ProblemType.REST
        config.bbTargetUrl = ""
        config.bbSwaggerUrl = "$target/swagger.json"

        val sampler = mockk<AbstractRestSampler>()
        every{sampler.schemaHolder} returns RestSchema(SchemaOpenAPI("", OpenAPI(), SchemaLocation.ofRemote(config.bbSwaggerUrl)))

        assertEquals(target, BlackBoxUtils.targetUrl(config,sampler))
    }
}
package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BlackBoxUtilsTest{

    @Test
    fun testTargetUrl(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.bbTargetUrl = target
        config.bbSwaggerUrl = "$target/swagger.json"

        assertEquals(target, BlackBoxUtils.restUrl(config))
    }


    @Test
    fun testTargetUrlDifferentSwagger(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.bbTargetUrl = target
        config.bbSwaggerUrl = "http://192.168.1.42:12345/swagger.json"

        assertEquals(target, BlackBoxUtils.restUrl(config))
    }


    @Test
    fun testTargetUrlMissing(){

        val target = "http://localhost:8080"

        val config = EMConfig()
        config.bbTargetUrl = ""
        config.bbSwaggerUrl = "$target/swagger.json"

        assertEquals(target, BlackBoxUtils.restUrl(config))
    }

    @Test
    fun testTargetUrlMissingNoPort(){

        val target = "http://localhost"

        val config = EMConfig()
        config.bbTargetUrl = ""
        config.bbSwaggerUrl = "$target/swagger.json"

        assertEquals(target, BlackBoxUtils.restUrl(config))
    }
}
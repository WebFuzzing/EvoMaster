package org.evomaster.core.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfigUtilTest{

    private val basePath = "src/test/resources/config"

    @Test
    fun testBase(){

        val path = "${basePath}/base.toml"
        val config = ConfigUtil.readFromToml(path)

        assertEquals(1, config.configs.size)
        assertTrue(config.configs.containsKey("blackBox"))
    }
}
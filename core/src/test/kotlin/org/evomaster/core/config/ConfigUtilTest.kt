package org.evomaster.core.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths

class ConfigUtilTest{

    private val basePath = "src/test/resources/config"

    @ParameterizedTest
    @ValueSource(strings = ["config.toml","config.yaml"])
    fun testWriteAndReadConfigFile(fileName: String) {
        val path = "./target/tmp/$fileName"
        Files.deleteIfExists(Paths.get(path))

        val cff = ConfigsFromFile()
        cff.configs["foo"] = "x"
        cff.configs["bar"] = "y"

        ConfigUtil.createConfigFileTemplate(path,cff)

        val back = ConfigUtil.readFromFile(path)
        assertEquals(0, back.configs.size) // should be commented out
    }


    @ParameterizedTest
    @ValueSource(strings = ["base.toml","base.yaml"])
    fun testBase(fileName: String){
        val path = "$basePath/$fileName"
        val config = ConfigUtil.readFromFile(path)

        assertEquals(1, config.configs.size)
        assertTrue(config.configs.containsKey("blackBox"))
    }


    @Test
    fun testAuthCookie(){
        val path = "${basePath}/auth_cookie.toml"
        val config = ConfigUtil.readFromFile(path)

        assertEquals(2, config.auth.size)
        assertTrue(config.auth.any { it.loginEndpointAuth.payloadUserPwd.username == "first" })
        assertTrue(config.auth.any { it.loginEndpointAuth.payloadUserPwd.username == "second" })

        assertTrue(config.auth.any { it.loginEndpointAuth.payloadUserPwd.usernameField == "x" })
        assertTrue(config.auth.any { it.loginEndpointAuth.payloadUserPwd.usernameField == null })
    }

    @Test
    fun testWrong(){
        val path = "${basePath}/wrong.toml"
        assertThrows(Exception::class.java){
            ConfigUtil.readFromFile(path)
        }
    }
}
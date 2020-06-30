package com.foo.base

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths


class BaseIT {


    companion object {
        private val driver = BaseExternalDriver()
        private val starter = InstrumentedSutStarter(driver)
        private lateinit var remote: RemoteController

        private fun setupJarAgent(){

            val path = File("../client-java/instrumentation/target").walk()
                    .filter { it.name.endsWith(".jar") }
                    .find {
                        it.name.matches(Regex("evomaster-client-java-instrumentation-\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?\\.jar"))
                    }!!
                    .absolutePath

            System.setProperty("evomaster.instrumentation.jar.path", path)
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            setupJarAgent()
            driver.controllerPort = 0
            starter.start()
            remote = RemoteController("localhost", driver.controllerServerPort, false, false)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            starter.stop()
        }
    }

    @Test
    fun testConnectionToDriver(){
       remote.checkConnection()
    }

    @Test
    fun testSearchCommands(){
        remote.startANewSearch()
        remote.startSUT()

        remote.registerNewAction(ActionDto().apply { index = 0 })

        val results = remote.getTestResults()
        Assertions.assertNotNull(results)
        Assertions.assertEquals(0, results!!.targets.size)
    }

    @Test
    fun testRestart(){

        //make sure it is started
        remote.startSUT()
        var info = remote.getSutInfo()
        assertNotNull(info)

        //stop it
        remote.stopSUT()
        info = remote.getSutInfo()
        assertNull(info)

        //start it again
        remote.startSUT()
        info = remote.getSutInfo()
        assertNotNull(info)

        //stop it
        remote.stopSUT()
        info = remote.getSutInfo()
        assertNull(info)

        //start it again
        remote.startSUT()
        info = remote.getSutInfo()
        assertNotNull(info)
    }
}
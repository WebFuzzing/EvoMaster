package com.foo.base

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.e2etests.utils.CIUtils
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
            //Travis does not like spawn processes... :(
            CIUtils.skipIfOnTravis()

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
        assertNotNull(results)
        assertEquals(0, results!!.targets.size)
    }

    @Test
    fun testRestart(){

        //make sure it is started
        assertTrue(remote.startSUT())
        var info = remote.getSutInfo()
        assertNotNull(info)

        //stop it
        assertTrue(remote.stopSUT())
        info = remote.getSutInfo()
        assertNull(info)

        //start it again
        assertTrue(remote.startSUT())
        info = remote.getSutInfo()
        assertNotNull(info)

        //stop it
        assertTrue(remote.stopSUT())
        info = remote.getSutInfo()
        assertNull(info)

        //start it again
        assertTrue(remote.startSUT())
        info = remote.getSutInfo()
        assertNotNull(info)
    }
}
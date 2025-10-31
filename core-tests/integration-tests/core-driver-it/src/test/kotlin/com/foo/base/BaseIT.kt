package com.foo.base

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File


class BaseIT {


    companion object {
        private val driver = BaseExternalDriver()
        private val starter = InstrumentedSutStarter(driver)
        private lateinit var remote: RemoteController


        private fun setupJarAgent(){

            val folder = File("../../../client-java/instrumentation/target").absoluteFile
            if(!folder.exists()){
                throw IllegalStateException("Target folder does not exist: ${folder.absolutePath}")
            }

            val files = folder.listFiles()

            val path = files
                    .filter { it.name.endsWith(".jar") }
                    .find {
                        it.name.matches(Regex("evomaster-client-java-instrumentation-\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?\\.jar"))
                    }?.absolutePath
            if(path == null) {
                val names = files.map { it.name }
                throw IllegalStateException("evomaster-client-java-instrumentation jar file not found in target folder: ${folder.absolutePath}." +
                            " | Content: ${names.joinToString(", ")}")
            }

            System.setProperty("evomaster.instrumentation.jar.path", path)
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            setupJarAgent()
            driver.controllerPort = 0
            starter.start()
            remote = RemoteControllerImplementation("localhost", driver.controllerServerPort, false, false)
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
        val startedNewSearch = remote.startANewSearch()
        assertTrue(startedNewSearch, "Failed to start new search")

        val startedSUT = remote.startSUT()
        assertTrue(startedSUT, "Failed to start SUT")

        val actionRegistered = remote.registerNewAction(ActionDto().apply { index = 0 })
        assertTrue(actionRegistered, "Failed to register action")

        val results = remote.getTestResults()
        assertNotNull(results, "Failed to get results")
        assertEquals(0, results!!.targets.size)
    }

    @Test
    fun testRestart(){

        //make sure it is started
        assertTrue(remote.startSUT(), "Failed to restart SUT")
        var info = remote.getSutInfo()
        assertNotNull(info, "Failed to get SUT info")

        //stop it
        assertTrue(remote.stopSUT(), "Failed to stop SUT")
        info = remote.getSutInfo()
        assertNull(info, "Failed to get SUT info after stop")


        //start it again
        driver.sutPort++ //let's try to avoid issue with TCP port taking too long to be released
        assertTrue(remote.startSUT(), "Failed to re-start SUT")
        info = remote.getSutInfo()
        assertNotNull(info, "Failed to get SUT info after re-start")

        //stop it
        assertTrue(remote.stopSUT(), "Failed to re-stop SUT")
        info = remote.getSutInfo()
        assertNull(info, "Failed to get SUT info after re-stop")

        //start it again
        driver.sutPort++
        assertTrue(remote.startSUT(), "Failed to re-start SUT in 3rd time")
        info = remote.getSutInfo()
        assertNotNull(info)
    }
}
package com.foo.base

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File


@Disabled("No CI (Travis, CircleCI and GitHub) likes this test... :( ")
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
            //Travis and CircleCI do not like this test...
            CIUtils.skipIfOnTravis()
            CIUtils.skipIfOnCircleCI()

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
        driver.sutPort++ //let's try to avoid issue with TCP port taking too long to be released
        assertTrue(remote.startSUT())
        info = remote.getSutInfo()
        assertNotNull(info)

        //stop it
        assertTrue(remote.stopSUT())
        info = remote.getSutInfo()
        assertNull(info)

        //start it again
        driver.sutPort++
        assertTrue(remote.startSUT())
        info = remote.getSutInfo()
        assertNotNull(info)
    }
}
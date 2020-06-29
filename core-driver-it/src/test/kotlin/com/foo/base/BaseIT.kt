package com.foo.base

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class BaseIT {


    companion object {
        private val driver = BaseExternalDriver()
        private val starter = InstrumentedSutStarter(driver)
        private lateinit var remote: RemoteController

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
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
    fun testSearchCommands(){
        remote.startANewSearch()
        remote.startSUT()

        remote.registerNewAction(ActionDto().apply { index = 0 })

        val results = remote.getTestResults()
        Assertions.assertNotNull(results)
        Assertions.assertEquals(0, results!!.targets.size)
    }
}
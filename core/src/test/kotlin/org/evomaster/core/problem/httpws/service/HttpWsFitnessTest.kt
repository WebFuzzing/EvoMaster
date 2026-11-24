package org.evomaster.core.problem.httpws.service

import io.mockk.*
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.service.fitness.RestFitness
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Verify driver-controlled lifecycle preparation without changing black-box targets. */
class HttpWsFitnessTest {
    private class TestFitness : RestFitness() {
        fun closeClient() = client.close()
    }

    private fun fitness(controller: RemoteController, blackBox: Boolean = false): TestFitness {
        val fitness = TestFitness()
        for ((name, value) in listOf("rc" to controller, "config" to EMConfig().apply { this.blackBox = blackBox })) {
            var owner: Class<*> = fitness.javaClass
            while (owner.declaredFields.none { it.name == name }) owner = owner.superclass
            owner.getDeclaredField(name).apply { isAccessible = true }.set(fitness, value)
        }
        return fitness
    }

    @Test
    fun restartRefreshesTheBaseUrlAndInfoBeforeDetection() {
        val controller = mockk<RemoteController>()
        val freshInfo = SutInfoDto().apply { baseUrlOfSUT = "http://localhost:12345" }
        every { controller.stopSUT() } returns true
        every { controller.checkConnection() } just Runs
        every { controller.startSUT() } returns true
        every { controller.getSutInfo() } returns freshInfo
        val fitness = fitness(controller)
        fitness.infoDto = SutInfoDto().apply { baseUrlOfSUT = "http://localhost:54321" }
        fitness.prepareForFlakinessDetection()
        try {
            assertSame(freshInfo, fitness.infoDto)
            verifyOrder {
                controller.stopSUT()
                controller.checkConnection()
                controller.startSUT()
                controller.getSutInfo()
            }
        } finally {
            fitness.closeClient()
        }
    }

    @Test
    fun failedStopDoesNotSilentlyProceedWithDirtyState() {
        val controller = mockk<RemoteController>()
        every { controller.stopSUT() } returns false
        assertThrows(SutProblemException::class.java) { fitness(controller).prepareForFlakinessDetection() }
        verify(exactly = 0) { controller.startSUT() }
    }

    @Test
    fun blackBoxTargetsAreNotStopped() {
        val controller = mockk<RemoteController>()
        fitness(controller, blackBox = true).prepareForFlakinessDetection()
        verify { controller wasNot Called }
    }
}

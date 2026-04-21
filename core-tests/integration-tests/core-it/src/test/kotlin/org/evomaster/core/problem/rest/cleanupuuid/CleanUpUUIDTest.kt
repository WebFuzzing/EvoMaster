package org.evomaster.core.problem.rest.cleanupuuid

import bar.examples.it.spring.cleanupuuid.CleanUpUUIDApplication
import bar.examples.it.spring.cleanupuuid.CleanUpUUIDController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CleanUpUUIDTest: IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CleanUpUUIDController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        CleanUpUUIDApplication.reset()
        recreateInjectorForBlack(listOf("--blackBoxCleanUp","true"))
    }


    @Test
    fun testPutDelete() {

        assertEquals(0, CleanUpUUIDApplication.numberExistingData())

        val pirTest = getPirToRest()

        val id = UUID.randomUUID().toString()

        val put = pirTest.fromVerbPath("put", "/api/resources/$id")!!

        val x = createIndividual(listOf(put), SampleType.RANDOM)
        val resDel = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, resDel.getStatusCode())

        assertEquals(1, x.evaluatedMainActions().size)
        assertEquals(1, x.individual.seeMainExecutableActions().size)
        assertEquals(1, x.individual.seeCleanUpActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpUUIDApplication.numberExistingData())
    }
}
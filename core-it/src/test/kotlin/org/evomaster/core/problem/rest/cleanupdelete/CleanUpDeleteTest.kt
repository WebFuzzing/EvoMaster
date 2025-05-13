package org.evomaster.core.problem.rest.cleanupdelete

import bar.examples.it.spring.cleanupcreate.CleanUpDeleteApplication
import bar.examples.it.spring.cleanupcreate.CleanUpDeleteController
import bar.examples.it.spring.nonworkingdelete.NonWorkingDeleteApplication
import bar.examples.it.spring.nonworkingdelete.NonWorkingDeleteController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.oracle.HttpSemanticsOracle
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CleanUpDeleteTest: IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CleanUpDeleteController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        CleanUpDeleteApplication.reset()
        recreateInjectorForBlack(listOf("--blackBoxCleanUp","true"))
    }


    @Test
    fun testDelete() {

        assertEquals(0, CleanUpDeleteApplication.numberExistingData())

        val pirTest = getPirToRest()

        val id = 42

        val put = pirTest.fromVerbPath("put", "/api/resources/$id")!!

        val x = createIndividual(listOf(put), SampleType.RANDOM)
        val resDel = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, resDel.getStatusCode())

        assertEquals(1, x.evaluatedMainActions().size)

        //delete should had been automatically added
        assertEquals(0, CleanUpDeleteApplication.numberExistingData())

    }

    @Disabled("Tricky. In the end, we made not possible to send empty path elements. " +
            "See discussion in action builder.")
    @Test
    fun testEmpty(){

        assertEquals(0, CleanUpDeleteApplication.numberExistingData())

        val pirTest = getPirToRest()

        val fail = pirTest.fromVerbPath("put", "/api/resources")
        assertNull(fail)

        val put = pirTest.fromVerbPath("put", "/api/resources/")!!
        val delete = pirTest.fromVerbPath("delete", "/api/resources/")!!  //explicit delete
        val x = createIndividual(listOf(put,delete), SampleType.RANDOM)
        val resPut = x.evaluatedMainActions()[0].result as RestCallResult
        val resDel = x.evaluatedMainActions()[1].result as RestCallResult
        assertEquals(200, resPut.getStatusCode())
        assertEquals(204, resDel.getStatusCode())

        assertEquals(0, CleanUpDeleteApplication.numberExistingData())

        val y = createIndividual(listOf(put), SampleType.RANDOM)
        val resPutY = y.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, resPutY.getStatusCode())

        assertEquals(0, CleanUpDeleteApplication.numberExistingData())
    }
}
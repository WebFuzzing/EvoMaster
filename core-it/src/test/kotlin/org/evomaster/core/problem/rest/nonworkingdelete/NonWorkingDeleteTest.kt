package org.evomaster.core.problem.rest.nonworkingdelete

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
import org.junit.jupiter.api.Test

class NonWorkingDeleteTest: IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NonWorkingDeleteController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        NonWorkingDeleteApplication.reset()
        getEMConfig().security = false
        getEMConfig().schemaOracles = false
        getEMConfig().httpOracles = true
    }


    @Test
    fun testDelete() {

        val pirTest = getPirToRest()

        val id = 42

        val put = pirTest.fromVerbPath("put", "/api/resources/$id")!!
        val get = pirTest.fromVerbPath("get", "/api/resources/$id")!!
        val delete = pirTest.fromVerbPath("delete", "/api/resources/$id")!!

        val x = createIndividual(listOf(put, get, delete), SampleType.HTTP_SEMANTICS)
        val resDel = x.evaluatedMainActions()[2].result as RestCallResult
        assertEquals(204, resDel.getStatusCode())


        var res =  HttpSemanticsOracle.hasNonWorkingDelete(x.individual, x.seeResults())
        assertFalse(res.checkingDelete)
        assertFalse(res.nonWorking)

        val repeatedGet = pirTest.fromVerbPath("get", "/api/resources/$id")!!
        val y = createIndividual(listOf(put, get, delete, repeatedGet), SampleType.HTTP_SEMANTICS)

        res =  HttpSemanticsOracle.hasNonWorkingDelete(y.individual, y.seeResults())
        assertTrue(res.checkingDelete)
        assertTrue(res.nonWorking)

        val ar = y.evaluatedMainActions()[2].result as RestCallResult
        assertEquals(1, ar.getFaults().size)
        assertEquals(FaultCategory.HTTP_NONWORKING_DELETE, ar.getFaults()[0].category)
    }
}
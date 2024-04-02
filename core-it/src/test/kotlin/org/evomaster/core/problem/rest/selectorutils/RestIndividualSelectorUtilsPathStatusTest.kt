package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.pathstatus.PathStatusController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.RestPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RestIndividualSelectorUtilsPathStatusTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PathStatusController())
        }
    }


    @Test
    fun testPathStatus(){

        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/pathstatus/byStatus/{status}")
        val others = RestPath("/api/pathstatus/others/{x}")

        val s200 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val s400 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/400")!!
        val o200 = pirTest.fromVerbPath("get", "/api/pathstatus/others/200")!!
        val o500 = pirTest.fromVerbPath("get", "/api/pathstatus/others/500")!!

        val x0 = createIndividual(listOf(s200))
        val x1 = createIndividual(listOf(s400))
        val x2 = createIndividual(listOf(o200))
        val x3 = createIndividual(listOf(o500))

        val individuals = listOf(x0,x1,x2,x3)

        val r0 = RestIndividualSelectorUtils.findIndividuals(individuals, HttpVerb.GET, byStatus, 200)
        assertEquals(1, r0.size)

        val r1 = RestIndividualSelectorUtils.findIndividuals(individuals, HttpVerb.GET, byStatus, 500)
        assertEquals(0, r1.size)
    }

}
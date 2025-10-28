package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.pathstatus.PathStatusController
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        val r2 = RestIndividualSelectorUtils.findIndividuals(individuals, HttpVerb.GET, others, 200)
        assertEquals(2, r2.size)
    }

    @Test
    fun testIndex(){

        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/pathstatus/byStatus/{status}")
        val others = RestPath("/api/pathstatus/others/{x}")

        val s200 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val s400 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/400")!!
        val o200 = pirTest.fromVerbPath("get", "/api/pathstatus/others/200")!!

        val x = createIndividual(listOf(s200,s400,o200,s200.copy() as RestCallAction))

        assertEquals(2, x.individual.getActionIndex(HttpVerb.GET, others))
        assertTrue(x.individual.getActionIndex(HttpVerb.POST, others) < 0)

        assertEquals(0, x.individual.getActionIndex(HttpVerb.GET, byStatus))
    }


    @Test
    fun testFindAction() {

        val pirTest = getPirToRest()

        val others = RestPath("/api/pathstatus/others/{x}")

        // create 10 actions
        val action1 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val action2 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/201")!!
        val action3 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/202")!!
        val action4 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/204")!!
        val action5 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/301")!!
        val action6 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/302")!!
        val action7 = pirTest.fromVerbPath("get", "/api/pathstatus/others/304")!!
        val action8 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/401")!!
        val action9 = pirTest.fromVerbPath("get", "/api/pathstatus/others/402")!!
        val action10 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/404")!!

        val createdIndividualFirst = createIndividual(listOf(action1, action2, action3, action4, action5))
        val createdIndividualSecond = createIndividual(listOf(action6, action7, action8, action9, action10))

        val listOfIndividuals = listOf(createdIndividualFirst, createdIndividualSecond)

        // find action with GET request
        val actionWithGet = RestIndividualSelectorUtils.findAction(listOfIndividuals, HttpVerb.GET) as RestCallAction
        assertTrue(actionWithGet.verb == HttpVerb.GET)

        // find action with get request having path as others and status code as 200
        val eval = RestIndividualSelectorUtils.findEvaluatedAction(listOfIndividuals, HttpVerb.GET, others, 200 )
        val actionWithPathOthers = eval!!.action as RestCallAction
        val actionWithPathOthersResult = eval.result as RestCallResult

        assertTrue(actionWithPathOthers.verb == HttpVerb.GET)
        assertTrue(actionWithPathOthersResult.getStatusCode() == 200)
    }

}
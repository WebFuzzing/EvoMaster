package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.pathstatus.PathStatusController
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class RestIndividualSelectorUtilsPathStatusTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PathStatusController())
        }
    }


    /**
     * Tests for methods inside RestIndividualSelectorUtils.kt
     *
     * 1. findAction based on VERB only
     * 2. findAction based on PATH only
     * 3. findAction based on
     */




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
    }


    @Test
    fun testSliceBasic(){

        // create 5 actions
        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/pathstatus/byStatus/{status}")

        val action1 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val action2 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/201")!!
        val action3 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/402")!!
        val action4 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/404")!!
        val action5 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/300")!!


        val currentIndividual = createIndividual(listOf(action1, action2, action3, action4, action5))

        // slice from index 2, in this case it should contain action1, action2, and action3
        val slicedIndividualT1 = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividual.individual, 2)

        Assert.assertEquals(3, slicedIndividualT1.seeMainExecutableActions().size)

        // the first item should be GET with 200 status code
        val action1OfT1 = slicedIndividualT1.seeMainExecutableActions()[0]
        val action2OfT1 = slicedIndividualT1.seeMainExecutableActions()[1]
        val action3OfT1 = slicedIndividualT1.seeMainExecutableActions()[2]

        val newIndividualT1 = createIndividual(listOf(action1OfT1, action2OfT1, action3OfT1))

        val count1T1 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT1), HttpVerb.GET, byStatus, 200)
        Assert.assertEquals(1, count1T1.size)

        val count2T1 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT1), HttpVerb.GET, byStatus, 201)
        Assert.assertEquals(1, count2T1.size)

        val count3T1 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT1), HttpVerb.GET, byStatus, 402)
        Assert.assertEquals(1, count3T1.size)

        // check results of the evaluated action
        val res1T1 = newIndividualT1.evaluatedMainActions()[0].result as RestCallResult
        val res2T1 = newIndividualT1.evaluatedMainActions()[1].result as RestCallResult
        val res3T1 = newIndividualT1.evaluatedMainActions()[2].result as RestCallResult

        Assert.assertEquals(200, res1T1.getStatusCode())
        Assert.assertEquals(201, res2T1.getStatusCode())
        Assert.assertEquals(402, res3T1.getStatusCode())

        // slice from index 0, so only one of them should be there and do all the same things
        val slicedIndividualT2 = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividual.individual, 0)
        Assert.assertEquals(1, slicedIndividualT2.seeMainExecutableActions().size)

        // the first item should be GET with 200 status code
        val action1OfT2 = slicedIndividualT2.seeMainExecutableActions()[0]

        val newIndividualT2 = createIndividual(listOf(action1OfT2))

        val count1T2 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT2), HttpVerb.GET, byStatus, 200)
        Assert.assertEquals(1, count1T2.size)

        // check results of the evaluated action
        val res1T2 = newIndividualT2.evaluatedMainActions()[0].result as RestCallResult

        Assert.assertEquals(200, res1T2.getStatusCode())

        // slice from index 4 (the last index)
        val slicedIndividualT3 = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividual.individual, 4)
        Assert.assertEquals(5, slicedIndividualT3.seeMainExecutableActions().size)

        // the first item should be GET with 200 status code
        val action1OfT3 = slicedIndividualT3.seeMainExecutableActions()[0]
        val action2OfT3 = slicedIndividualT3.seeMainExecutableActions()[1]
        val action3OfT3 = slicedIndividualT3.seeMainExecutableActions()[2]
        val action4OfT3 = slicedIndividualT3.seeMainExecutableActions()[3]
        val action5OfT3 = slicedIndividualT3.seeMainExecutableActions()[4]

        val newIndividualT3 = createIndividual(listOf(action1OfT3, action2OfT3, action3OfT3, action4OfT3, action5OfT3))

        val count1T3 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT3), HttpVerb.GET, byStatus, 200)
        Assert.assertEquals(1, count1T3.size)

        val count2T3 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT3), HttpVerb.GET, byStatus, 201)
        Assert.assertEquals(1, count2T3.size)

        val count3T3 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT3), HttpVerb.GET, byStatus, 402)
        Assert.assertEquals(1, count3T3.size)

        val count4T3 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT3), HttpVerb.GET, byStatus, 404)
        Assert.assertEquals(1, count4T3.size)

        val count5T3 =  RestIndividualSelectorUtils.findIndividuals(listOf(newIndividualT3), HttpVerb.GET, byStatus, 300)
        Assert.assertEquals(1, count5T3.size)

        // check results of the evaluated action
        val res1T3 = newIndividualT3.evaluatedMainActions()[0].result as RestCallResult
        val res2T3 = newIndividualT3.evaluatedMainActions()[1].result as RestCallResult
        val res3T3 = newIndividualT3.evaluatedMainActions()[2].result as RestCallResult
        val res4T3 = newIndividualT3.evaluatedMainActions()[3].result as RestCallResult
        val res5T3 = newIndividualT3.evaluatedMainActions()[4].result as RestCallResult

        Assert.assertEquals(200, res1T3.getStatusCode())
        Assert.assertEquals(201, res2T3.getStatusCode())
        Assert.assertEquals(402, res3T3.getStatusCode())
        Assert.assertEquals(404, res4T3.getStatusCode())
        Assert.assertEquals(300, res5T3.getStatusCode())


        // slice from index -1 (non-existent), this will remove all calls
        val slicedIndividualT4 = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividual.individual, -1)
        Assert.assertEquals(0, slicedIndividualT4.seeMainExecutableActions().size)

        // slice from index 7 (non-existent), this will include all calls
        val slicedIndividualT5 = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividual.individual, 7)
        Assert.assertEquals(5, slicedIndividualT5.seeMainExecutableActions().size)

    }

    @Test
    fun testFindAction() {

        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/pathstatus/byStatus/{status}")
        val others = RestPath("/api/pathstatus/others/{x}")

        // create 10 actions
        val action1 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val action2 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/201")!!
        val action3 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/202")!!
        val action4 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/204")!!
        val action5 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/301")!!
        val action6 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/302")!!
        val action7 = pirTest.fromVerbPath("get", "/api/pathstatus/others/304")!!

        //action7.auth = HttpWsAuthenticationInfo("auth1",
         //   listOf(AuthenticationHeader("header0", "name")), null, false)

        val action8 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/401")!!
        val action9 = pirTest.fromVerbPath("get", "/api/pathstatus/others/402")!!
        val action10 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/404")!!

        val createdIndividualFirst = createIndividual(listOf(action1, action2, action3, action4, action5))
        val createdIndividualSecond = createIndividual(listOf(action6, action7, action8, action9, action10))

        val listOfIndividuals = listOf(createdIndividualFirst, createdIndividualSecond)

        // find action with GET request
        val actionWithGet = RestIndividualSelectorUtils.findAction(listOfIndividuals, HttpVerb.GET) as RestCallAction
        Assert.assertTrue(actionWithGet.verb == HttpVerb.GET)

        // find action with get request having path as others and status code as 200
        val eval = RestIndividualSelectorUtils.findEvaluatedAction(listOfIndividuals, HttpVerb.GET, others, 200 )
        val actionWithPathOthers = eval!!.action as RestCallAction
        val actionWithPathOthersResult = eval!!.result as RestCallResult

        Assert.assertTrue(actionWithPathOthers.verb == HttpVerb.GET)
        Assert.assertTrue(actionWithPathOthersResult.getStatusCode() == 200)
        //Assert.assertTrue(actionWithPathOthersResult.getBody().equals("404"))




        // find actions with path given for the status

        // find actions with status code 201

        // find actions with status group 4xx

        // find authenticated actions with status code 403



    }


    /*
    @Test
    @Disabled
    fun testFindIndividuals() {

        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/pathstatus/byStatus/{status}")
        val others = RestPath("/api/pathstatus/others/{x}")

        // create 10 actions
        val action1 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/200")!!
        val action2 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/201")!!
        val action3 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/202")!!
        val action4 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/204")!!
        val action5 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/301")!!
        val action6 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/302")!!

        //action6.auth = HttpWsAuthenticationInfo("auth1",
        //    listOf(AuthenticationHeader("header0", "name")), null, false)

        val action7 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/401")!!
        val action8 = pirTest.fromVerbPath("get", "/api/pathstatus/others/402")!!
        val action9 = pirTest.fromVerbPath("get", "/api/pathstatus/byStatus/404")!!

        val createdIndividualFirst = createIndividual(listOf(action1, action2, action3, action4, action5))
        val createdIndividualSecond = createIndividual(listOf( action6, action7, action8, action9))

        val listOfIndividuals = listOf(createdIndividualFirst, createdIndividualSecond)

        // find individuals having authenticated action
        val individualsInList = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null, null, null,
            null, true)

        Assert.assertTrue(individualsInList.size == 1)

        // find individuals having the path of others
        val individualsWithOthers = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null, others)

        Assert.assertTrue(individualsWithOthers.size == 1)

        // find individuals having onw get request, which means both of them
        val individualsWithGet = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, HttpVerb.GET)

        Assert.assertTrue(individualsWithGet.size == 2)

    }


     */


}
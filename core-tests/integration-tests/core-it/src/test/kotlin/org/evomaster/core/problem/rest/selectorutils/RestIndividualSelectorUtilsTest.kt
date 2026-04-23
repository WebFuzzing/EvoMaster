package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.multipleendpoints.MultipleEndpointsController
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.problem.rest.service.RestIndividualBuilder
import org.evomaster.core.search.EvaluatedIndividual
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RestIndividualSelectorUtilsTest : IntegrationTestRestBase() {

    /**
     * Tests for methods inside RestIndividualSelectorUtils.kt
     *
     * 1. findAction based on VERB only DONE
     * 2. findAction based on PATH only
     * 3. findAction based on STATUS only
     * 4. findAction based on STATUS GROUP only
     * 5. findAction based on if the action is authenticated or not
     * 6. findAction based on VERB and PATH
     * 7. findAction based on VERB and STATUS
     * 8. findAction based on PATH and STATUS
     * 9. findAction based on PATH and STATUS GROUP
     * 10. findAction based on VERB, PATH, and STATUS
     * 11. findAction based on VERB, PATH, and STATUS GROUP
     * 12. findAction authenticated actions
     * 13. findAction non-authenticated actions
     *
     * 14. findEvaluated action for all above (13 test cases)
     * 15. findIndividuals with same 13 test cases. (13 test cases)
     * 16. test for get all action definitions
     * 17. 4 tests for findIndividualWithEndpointCreationForResource
     * 18. getIndexOfAction existing action
     * 19. getIndexOfAction non-existing action
     * 20. sliceAllCallsAfterIndividual start from beginning
     * 21. sliceAllCallsAfterIndividual startFromEnd
     * 22. sliceAllCallsAfterIndividual from middle
     * 23. sliceAllCallsAfterIndividual from index smaller than 0
     * 24. sliceAllCallsAfterIndividual from index greater than the number of actions.
     */


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MultipleEndpointsController())

        }
    }

    // a sample set of 4 individuals needed for tests
    private fun initializeIndividuals(individual1WithoutGet : Boolean = false) : List<EvaluatedIndividual<RestIndividual>> {

        val pirTest = getPirToRest()

        // first individual contains endpoint 2 GET, endpoint 1 POST, endpoint 3 PUT, endpoint 4 DELETE
        // and endpoint 5 authenticated GET with the status code
        val action1Ind1 = pirTest.fromVerbPath("GET", "/api/endpoint2/1000")!!
        val action2Ind1 = pirTest.fromVerbPath("POST", "/api/endpoint1")!!
        val action3Ind1 = pirTest.fromVerbPath("PUT", "/api/endpoint3/2000")!!
        val action4Ind1 = pirTest.fromVerbPath("DELETE", "/api/endpoint4/1005")!!
        val action5Ind1 = pirTest.fromVerbPath("GET", "/api/endpoint5/setStatus/402")!!

        val individual1 = if (!individual1WithoutGet) {
            createIndividual(listOf(action1Ind1, action2Ind1, action3Ind1, action4Ind1, action5Ind1))
        } else {
            createIndividual(listOf(action2Ind1, action3Ind1, action4Ind1))
        }

        // second individual contains endpoint 1 GET, endpoint 2 GET with status code, endpoint 3 POST, endpoint 4 PUT
        // and endpoint 5 authenticated DELETE
        val action1Ind2 = pirTest.fromVerbPath("GET", "/api/endpoint1/5000")!!
        val action2Ind2 = pirTest.fromVerbPath("POST", "/api/endpoint3")!!
        val action3Ind2 = pirTest.fromVerbPath("PUT", "/api/endpoint4/6000")!!
        val action4Ind2 = pirTest.fromVerbPath("DELETE", "/api/endpoint5/8000")!!
        action4Ind2.auth = HttpWsAuthenticationInfo("action4Ind2",
            listOf(AuthenticationHeader("name", "authentication")),
            null, false)
        val action5Ind2 = pirTest.fromVerbPath("GET", "/api/endpoint2/setStatus/403")!!

        val individual2 = createIndividual(listOf(action1Ind2, action2Ind2, action3Ind2, action4Ind2, action5Ind2))


        // third individual contains endpoint 1 POST, endpoint 2 PUT, endpoint 3 GET with authenticated, endpoint 4
        // GET with status code authenticated, endpoint 5 DELETE
        val action1Ind3 = pirTest.fromVerbPath("GET", "/api/endpoint3/8500")!!
        val action2Ind3 = pirTest.fromVerbPath("POST", "/api/endpoint1")!!
        val action3Ind3 = pirTest.fromVerbPath("PUT", "/api/endpoint2/9000")!!
        val action4Ind3 = pirTest.fromVerbPath("DELETE", "/api/endpoint5/8700")!!
        action1Ind3.auth = HttpWsAuthenticationInfo("action1Ind3",
            listOf(AuthenticationHeader("name", "authentication")),
            null, false)
        val action5Ind3 = pirTest.fromVerbPath("GET", "/api/endpoint4/setStatus/415")!!

        val individual3 = createIndividual(listOf(action1Ind3, action2Ind3, action3Ind3, action4Ind3, action5Ind3))

        // fourth individual contains endpoint 5 GET with authentication, endpoint 4 POST, endpoint 3 with DELETE
        // endpoint 2 with PUT, endpoint 1 with GET with status code.
        val action1Ind4 = pirTest.fromVerbPath("GET", "/api/endpoint5/8800")!!
        val action2Ind4 = pirTest.fromVerbPath("POST", "/api/endpoint4")!!
        val action3Ind4 = pirTest.fromVerbPath("PUT", "/api/endpoint2/9600")!!
        val action4Ind4 = pirTest.fromVerbPath("DELETE", "/api/endpoint3/1700")!!
        action1Ind4.auth = HttpWsAuthenticationInfo("action1Ind3",
            listOf(AuthenticationHeader("name", "authentication")),
            null, false)
        val action5Ind4 = pirTest.fromVerbPath("GET", "/api/endpoint4/setStatus/404")!!

        val individual4 = createIndividual(listOf(action1Ind4, action2Ind4, action3Ind4, action4Ind4, action5Ind4))

        return listOf(individual1, individual2, individual3, individual4)
    }

    @Test
    fun testFindActionVerbOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, HttpVerb.GET)

        // All the 4 individuals are selected since each has GET request
        Assertions.assertTrue(selectedIndividuals.size == 4)

        // now remove individual1 from the list

        // select create individuals without individual1 having GET
        val listOfIndividualsSecond = initializeIndividuals(true)

        val selectedIndividualsSecond = RestIndividualSelectorUtils.findIndividuals(listOfIndividualsSecond, HttpVerb.GET)

        // Now 3 individuals are selected since each has GET request
        Assertions.assertTrue(selectedIndividualsSecond.size == 3)

    }

    @Test
    fun testFindActionPathOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            RestPath("/api/endpoint5/setStatus/{status}")
        )

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 1)

        // now find all individuals with endpoint /api/endpoint2, this is none of the individuals
        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            RestPath("/api/endpoint2")
        )

        // Only 1 individual has a request with the path "/api/endpoint2"
        Assertions.assertTrue(secondSelectedIndividuals.isEmpty())

        // now find all individuals with endpoint /api/endpoint3, this is one of the individuals
        val thirdSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            RestPath("/api/endpoint3")
        )

        // Only 1 individual has a request with the path "/api/endpoint3",
        Assertions.assertTrue(thirdSelectedIndividuals.size == 1)
    }

    @Test
    fun testFindActionStatusOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, 201)

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 1)

        // now find all individuals with endpoint /api/endpoint2, this is none of the individuals
        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, 415)

        // Only 1 individual has a request with the path "/api/endpoint2"
        Assertions.assertTrue(secondSelectedIndividuals.size == 1)

        // now find all individuals with endpoint /api/endpoint3, this is one of the individuals
        val thirdSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, 499)

        // Only 1 individual has a request with the path "/api/endpoint3",
        Assertions.assertTrue(thirdSelectedIndividuals.isEmpty())
    }

    @Test
    fun testFindActionStatusGroupOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, null, StatusGroup.G_2xx)

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 4)

        // now find all individuals with endpoint /api/endpoint2, this is none of the individuals
        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, null, StatusGroup.G_4xx)

        // Only 1 individual has a request with the path "/api/endpoint2"
        Assertions.assertTrue(secondSelectedIndividuals.size == 4)

        // now find all individuals with endpoint /api/endpoint3, this is one of the individuals
        val thirdSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            null, null, StatusGroup.G_5xx)

        // Only 1 individual has a request with the path "/api/endpoint3",
        Assertions.assertTrue(thirdSelectedIndividuals.size == 4)
    }

    @Test
    fun testFindActionAuthenticatedActionsOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, authenticated = true
        )

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 3)
    }

    @Test
    fun testFindActionGroupsBasedOnVerbAndPath() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.DELETE,
            RestPath("/api/endpoint5/{endpointIdentifier}")
        )

        Assertions.assertTrue(selectedIndividuals.size == 2)

        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.POST,
            RestPath("/api/endpoint5/{endpointIdentifier}")
        )

        Assertions.assertTrue(secondSelectedIndividuals.isEmpty())
    }

    @Test
    fun testFindActionGroupsBasedOnVerbAndStatus() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.DELETE,
            null, 201
        )

        Assertions.assertTrue(selectedIndividuals.isEmpty())

        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.POST,
            null, 301
        )

        Assertions.assertTrue(secondSelectedIndividuals.size == 2)
    }

    @Test
    fun testFindActionGroupsBasedOnPathAndStatus() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, null, RestPath("/api/endpoint5/setStatus/{status}"), 402)

        Assertions.assertTrue(selectedIndividuals.size == 1)


    }

    @Test
    fun testFindActionGroupsBasedOnPathAndStatusGroup() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, null, RestPath("/api/endpoint3"), null,
            StatusGroup.G_3xx)

        Assertions.assertTrue(selectedIndividuals.size == 1)

    }

    @Test
    fun testFindActionGroupsBasedOnVerbPathStatus() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.GET, RestPath("/api/endpoint2/setStatus/{status}"), 403)

        Assertions.assertTrue(selectedIndividuals.size == 1)

    }

    @Test
    fun testFindActionGroupsBasedOnVerbPathStatusStatusGroup() {

        val listOfIndividuals = initializeIndividuals()

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            RestIndividualSelectorUtils.findIndividuals(
                listOfIndividuals, HttpVerb.GET, RestPath("/api/endpoint2/setStatus/{status}"),
                403, StatusGroup.G_4xx)
        }

    }

    @Test
    fun testFindActionGroupsBasedOnVerbPathStatusGroup() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.GET, RestPath("/api/endpoint2/setStatus/{status}"), null,
            StatusGroup.G_5xx)

        Assertions.assertTrue(selectedIndividuals.isEmpty())

    }

    @Test
    fun testFindAuthenticatedActions() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, authenticated = true)

        Assertions.assertTrue(selectedIndividuals.size == 3)

    }

    @Test
    fun testFindNonAuthenticatedActions() {

        val listOfIndividuals = initializeIndividuals()

        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, authenticated = false)

        Assertions.assertTrue(selectedIndividuals.size == 4)

    }

    /**
     * Find the first evaluated action with POST and /api/endpoint2
     */
    @Test
    fun testFindEvaluatedAction() {

        val listOfIndividuals = initializeIndividuals()

        val selectedAction = RestIndividualSelectorUtils.findEvaluatedAction(
            listOfIndividuals, HttpVerb.POST, RestPath("/api/endpoint3"), 303
        )

        Assertions.assertTrue((selectedAction?.action as RestCallAction).verb == HttpVerb.POST)
        Assertions.assertTrue((selectedAction?.action as RestCallAction).path == RestPath("/api/endpoint3"))
        Assertions.assertTrue((selectedAction?.result as RestCallResult).getStatusCode() == 303)

    }

    /**
     * Find the first evaluated action with POST and /api/endpoint2
     */
    @Test
    fun testFindEvaluatedActionNonExistent() {

        val listOfIndividuals = initializeIndividuals()

        val selectedAction = RestIndividualSelectorUtils.findEvaluatedAction(
            listOfIndividuals, HttpVerb.POST, RestPath("/api/endpoint3"), 404
        )

        Assertions.assertTrue( selectedAction == null)

    }

    /**
     * Test findAction
     */
    @Test
    fun testFindAction() {

        val listOfIndividuals = initializeIndividuals()

        val selectedAction = RestIndividualSelectorUtils.findAction(
            listOfIndividuals, HttpVerb.POST, RestPath("/api/endpoint3"), 303
        )

        Assertions.assertTrue( (selectedAction?.verb == HttpVerb.POST) )
        Assertions.assertTrue( (selectedAction?.path == RestPath("/api/endpoint3")) )

    }

    /**
     * Test getting indices of actions in individuals.
     */
    @Test
    fun testFindIndexOfAction() {

        val listOfIndividuals = initializeIndividuals()

        val actionIndex = RestIndividualSelectorUtils.getIndexOfAction(listOfIndividuals[0], HttpVerb.POST,
            RestPath("/api/endpoint1"), 301)

        Assertions.assertTrue(actionIndex == 1)

        val actionIndexSecond = RestIndividualSelectorUtils.getIndexOfAction(listOfIndividuals[2], HttpVerb.GET,
            RestPath("/api/endpoint4/setStatus/{status}"), 415)

        Assertions.assertTrue(actionIndexSecond == 4)

    }

    /**
     * Test getting indices of actions in individuals.
     */
    @Test
    fun testFindIndexOfActionNonExistent() {

        val listOfIndividuals = initializeIndividuals()

        val actionIndex = RestIndividualSelectorUtils.getIndexOfAction(listOfIndividuals[0], HttpVerb.POST,
            RestPath("/api/endpoint1"), 501)

        Assertions.assertTrue(actionIndex == -1)

        val actionIndexSecond = RestIndividualSelectorUtils.getIndexOfAction(listOfIndividuals[2], HttpVerb.GET,
            RestPath("/api/endpoint4/setStatus/{status}"), 417)

        Assertions.assertTrue(actionIndexSecond == -1)

    }

    @Test
    fun testSliceAllCallsInIndividualAfterAction() {
        val listOfIndividuals = initializeIndividuals()

        val newIndividual = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
            listOfIndividuals[1].individual, 2)

        // now check actions in the newIndividual
        Assertions.assertTrue(newIndividual.size() == 3)

        // check actions of slices
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[0].verb == HttpVerb.GET)
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[1].verb == HttpVerb.POST)
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[2].verb == HttpVerb.PUT)

        // check paths of slices
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[0].path == RestPath("/api/endpoint1/{endpointIdentifier}"))
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[1].path == RestPath("/api/endpoint3") )
        Assertions.assertTrue(newIndividual.seeMainExecutableActions()[2].path == RestPath("/api/endpoint4/{endpointIdentifier}"))


        // now try from index 1
        val newIndividualSecond = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
            listOfIndividuals[1].individual, 1)

        // now check actions in the newIndividual
        Assertions.assertTrue(newIndividualSecond.size() == 2)

        // check actions of slices
        Assertions.assertTrue(newIndividualSecond.seeMainExecutableActions()[0].verb == HttpVerb.GET)
        Assertions.assertTrue(newIndividualSecond.seeMainExecutableActions()[1].verb == HttpVerb.POST)


        // check paths of slices
        Assertions.assertTrue(newIndividualSecond.seeMainExecutableActions()[0].path == RestPath("/api/endpoint1/{endpointIdentifier}"))
        Assertions.assertTrue(newIndividualSecond.seeMainExecutableActions()[1].path == RestPath("/api/endpoint3") )

    }

    @Test
    fun testSliceAllCallsInIndividualAfterActionNonValidIndices() {
        val listOfIndividuals = initializeIndividuals()

        Assert.assertThrows(IllegalArgumentException::class.java) {
            RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                listOfIndividuals[1].individual, 10)
        }

        Assert.assertThrows(IllegalArgumentException::class.java) {
            RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                listOfIndividuals[1].individual, -10)
        }

    }



}
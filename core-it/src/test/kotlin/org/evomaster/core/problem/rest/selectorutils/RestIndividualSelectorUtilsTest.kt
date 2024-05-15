package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.multipleendpoints.MultipleEndpointsController
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.EvaluatedIndividual
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
            RestPath("/api/endpoint5/setStatus/{status}"))

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 1)

        // now find all individuals with endpoint /api/endpoint2, this is none of the individuals
        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            RestPath("/api/endpoint2"))

        // Only 1 individual has a request with the path "/api/endpoint2"
        Assertions.assertTrue(secondSelectedIndividuals.isEmpty())

        // now find all individuals with endpoint /api/endpoint3, this is one of the individuals
        val thirdSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(listOfIndividuals, null,
            RestPath("/api/endpoint3"))

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
    fun testFindActionGroupsWithAuthenticatedActionsOnly() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, null,
            null, null, null, true
        )

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 3)
    }

    @Test
    fun testFindActionGroupsBasedOnVerbAndPath() {

        val listOfIndividuals = initializeIndividuals()

        // in the beginning all the individuals are selected since each has GET request
        val selectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.DELETE,
            RestPath("/api/endpoint5/{endpointIdentifier}")
        )

        // Only 1 individual has a request with the path "/api/endpoint5/setStatus/{status}"
        Assertions.assertTrue(selectedIndividuals.size == 2)

        // now find all individuals with endpoint /api/endpoint2, this is none of the individuals
        val secondSelectedIndividuals = RestIndividualSelectorUtils.findIndividuals(
            listOfIndividuals, HttpVerb.POST,
            RestPath("/api/endpoint5/{endpointIdentifier}")
        )

        // Only 1 individual has a request with the path "/api/endpoint2"
        Assertions.assertTrue(secondSelectedIndividuals.isEmpty())


    }


}
package org.evomaster.core.problem.rest.service

import com.google.inject.Injector
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.CookieLogin
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution




/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    fun applySecurityPhase(injector : Injector): Solution<RestIndividual>{

        // This function will be called
        var rc = injector.getInstance(RemoteControllerImplementation::class.java)

        // check if the driver provides authentication info
        val sutInfo :SutInfoDto? = rc.getSutInfo()

        if (sutInfo != null && sutInfo.infoForAuthentication.isEmpty()) {
            LoggingUtil.getInfoLogger().info("The SUT does not contain any authenticationInfo, so no security tests are generated.")
            return Solution(mutableListOf(),"","",Termination.NONE, listOf())
        }
        else {

            LoggingUtil.getInfoLogger().info("The SUT contains authenticationInfo, so security tests can be generated.")

            // In the first test case, userA authenticates, creates a resource which only belongs to userA,
            // then reads that resource. userB tries to read the resource created by userA, which should fail
            //val testCaseReadDelete : RestIndividual = securityTestCreateAndRead(rc);

            // create one EvaluatedIndividual with endpoints, REST method and expected result

            // userA creates a resource and then deletes the same resource, tries to read it again
            val ind1 = securityTestCreateReadAndDeleteSameUser(rc)

            // userA creates a resource, user B tries to delete that resource
            val ind2 = securityTestCreateReadDifferentUser(rc)


            //val ind2 = securityTestCreateAndReadSecond(rc)





            var individuals = mutableListOf<EvaluatedIndividual<RestIndividual>>()

            //individuals.add(testCaseReadDelete)
            individuals.add(ind1)
            individuals.add(ind2)

            //return Solution(individuals,"","",Termination.NONE, listOf())
            return Solution(individuals,"securityTest","securityTest",Termination.NONE, listOf())
        }



        // get API endpoints and identify endpoints with POST

        // check if the driver provides authenticationInfo



        // authenticate two users

        // three cases:

        // let user A create a resource which can only be read by UserA

        // let user A read the resource, it should succeed

        // let user B read the resource, it should not succeed
        //val test1:RestIndividual = securityTestUserReadsResourceBelongingToOtherUser();


        // let User A create a resource, which can only read by UserA

        // let User A delete that resource

        // If user A tries to read that resource again, it fails

        // let User A create a resource, which can be read by everyone

        // let User B try to delete the resource belonging to User A, which should fail






        //val config = injector.getInstance(EMConfig::class.java)

        // create a solution in which one endpoint creates data, another endpoint reads the data

        // call actions include POST request for one endpoint among POST requests in the swagger using
        // authentication, then get request on the same endpoint by the same user who created it
        // this should succeed. The same post request using another user's authentication token should fail

        // injector
        //val injector = Main.init(args)

        // get endpoints from the swagger
        //val config = injector.getInstance(EMConfig::class.java)

        // configuration file
        // val config = injector.getInstance(EMConfig::class.java)

        // retrieve endpoints from swagger

        // check if any authentication info is provided along with the API

        //

        // find endpoints for POST which are used to create data

        // Create two users, A and B.

        // Let A create a resource


        //System.out.println(config)



        //var individual1 : RestIndividual = RestIndividual()

        System.out.println("HERE")





        //TODO
        return Solution(mutableListOf(),"","",Termination.NONE, listOf())
    }

    private fun authenticateTwoDistinctUsers(rc: RemoteControllerImplementation) {



        return

    }


    private fun securityTestCreateReadAndDeleteSameUser(rc : RemoteControllerImplementation) : EvaluatedIndividual<RestIndividual> {

        // authenticate two distinct users
        val cookieLogin1 = CookieLogin("UserA", "userA", "username", "password", "/login", HttpVerb.POST, ContentType.JSON)

        val info1 = HttpWsAuthenticationInfo("userAauth", mutableListOf(), cookieLogin1, null)

        val cookieLogin2 = CookieLogin("UserB", "userB", "username", "password", "/login", HttpVerb.POST, ContentType.JSON)

        val info2 = HttpWsAuthenticationInfo("userBauth", mutableListOf(), cookieLogin2, null)

        // fitness value 0.0
        val fv = FitnessValue(0.0)

        // rest actions
        val restActions = emptyList<RestCallAction>().toMutableList()

        val action1 = RestCallAction("1", HttpVerb.POST, RestPath("/userA"), mutableListOf(), info1)
        val action2 = RestCallAction("2", HttpVerb.GET, RestPath("/userA"), mutableListOf(), info1)
        val action3 = RestCallAction("3", HttpVerb.DELETE, RestPath("/userA"), mutableListOf(), info1)
        val action4 = RestCallAction("4", HttpVerb.GET, RestPath("/userA"), mutableListOf(), info1)

        restActions.add(action1)
        restActions.add(action2)
        restActions.add(action3)
        restActions.add(action4)

        // individual
        val individual = RestIndividual(restActions, SampleType.PREDEFINED)

        // results
        val results: MutableList<RestCallResult> = mutableListOf()

        val result1 = RestCallResult()
        result1.setStatusCode(200)

        val result2 = RestCallResult()
        result2.setStatusCode(200)

        val result3 = RestCallResult()
        result3.setStatusCode(200)

        val result4 = RestCallResult()
        result4.setStatusCode(401)

        results.add(result1)
        results.add(result2)
        results.add(result3)
        results.add(result4)
        //

        //var ind1 = EvaluatedIndividual(fv, individual, results)

        return EvaluatedIndividual(fv, individual, results)
    }


    private fun securityTestCreateReadDifferentUser(rc : RemoteControllerImplementation) : EvaluatedIndividual<RestIndividual> {

        // authenticate two distinct users
        val cookieLogin1 = CookieLogin("UserA", "userA", "username", "password", "/login", HttpVerb.POST, ContentType.JSON)

        val info1 = HttpWsAuthenticationInfo("userAauth", mutableListOf(), cookieLogin1, null)

        val cookieLogin2 = CookieLogin("UserB", "userB", "username", "password", "/login", HttpVerb.POST, ContentType.JSON)

        val info2 = HttpWsAuthenticationInfo("userBauth", mutableListOf(), cookieLogin2, null)

        // fitness value 0.0
        val fv = FitnessValue(0.0)

        // rest actions
        val restActions = emptyList<RestCallAction>().toMutableList()

        val action1 = RestCallAction("1", HttpVerb.POST, RestPath("/userA"), mutableListOf(), info1)
        val action2 = RestCallAction("2", HttpVerb.DELETE, RestPath("/userA"), mutableListOf(), info2)
        val action3 = RestCallAction("3", HttpVerb.DELETE, RestPath("/userA"), mutableListOf(), info1)

        restActions.add(action1)
        restActions.add(action2)
        restActions.add(action3)

        // individual
        val individual = RestIndividual(restActions, SampleType.PREDEFINED)

        // results
        val results: MutableList<RestCallResult> = mutableListOf()

        val result1 = RestCallResult()
        result1.setStatusCode(200)

        val result2 = RestCallResult()
        result2.setStatusCode(401)

        val result3 = RestCallResult()
        result3.setStatusCode(200)


        results.add(result1)
        results.add(result2)
        results.add(result3)
        //

        //var ind1 = EvaluatedIndividual(fv, individual, results)

        return EvaluatedIndividual(fv, individual, results)
    }


}
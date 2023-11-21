package org.evomaster.core.problem.rest.service

import com.google.inject.Injector
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchGlobalState


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

            var individuals = mutableListOf<EvaluatedIndividual<RestIndividual>>()

            //individuals.add(testCaseReadDelete)


            //return Solution(individuals,"","",Termination.NONE, listOf())
            return Solution(mutableListOf(),"","",Termination.NONE, listOf())
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

    /*
    private fun securityTestCreateAndRead(rc : RemoteControllerImplementation) : EvaluatedIndividual<RestIndividual> {

        // get user authentication information
        val sutInfo :SutInfoDto? = rc.getSutInfo()

        // randomly choose a user

        // create a resource with the randomly chosen user

        // read the resource with the same user, which should succeed.

        // read the resource with a different user (if there are more than 1 authenticated users), which should fail

        // if more than 1 users, try reading the deleted resource, which should fail

        var resourceCalls = mutableListOf<RestCallAction>()
       // resourceCalls.add(0, )

        val action1 : RestCallAction = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())
        resourceCalls.add(action1)

        //val resultingIndividual : EvaluatedIndividual<RestIndividual> = EvaluatedIndividual<RestIndividual>(resourceCalls, SampleType.PREDEFINED)

        //return resultingIndividual

        //val sampleType = SampleType.RANDOM
        //val action = RestCallAction("1", HttpVerb.GET, RestPath(""), mutableListOf())
        //val restActions = listOf(action).toMutableList()

        //return RestIndividual(restActions, sampleType)
    }
    */

}
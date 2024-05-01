package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import javax.annotation.PostConstruct

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.AuthSettings
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*

import org.evomaster.core.search.*
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness

//FIXME this needs to be cleaned-up

/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    /**
     * Archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: AbstractRestSampler

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var fitness: AbstractRestFitness

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>


    /**
     * Individuals in the solution.
     * Derived from archive.
     */
    private lateinit var individualsInSolution : List<EvaluatedIndividual<RestIndividual>>

    private lateinit var authSettings: AuthSettings

    /**
     * Function called after init. This function initializes REST sampler definitions
     * and authentication settings.
     */
    @PostConstruct
    private fun postInit(){


        // get action definitions
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>

        authSettings = sampler.authentications
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase() : Solution<RestIndividual>{

        // extract individuals from the archive
        val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution()
        individualsInSolution =  archivedSolution.individuals


        // we can see what is available from the schema, and then check if already existing a test for it in archive
        // newly generated tests will be added back to archive
        addForAccessControl()

        //TODO possible other kinds of tests here

        // just return the archive for solutions including the security tests.
        return archive.extractSolution()
    }

    private fun addForAccessControl() {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongo and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */

        accessControlBasedOnRESTGuidelines()
        //TODO implement access control based on database monitoring
        //accessControlBasedOnDatabaseMonitoring()
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines() {

        // quite a few rules here that can be defined
        handleForbiddenDeleteButOkPutOrPatch()
        //TODO other rules
    }



    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx) | or possibly via SQL insertions
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */


    private fun handleForbiddenDeleteButOkPutOrPatch() {

        /*
            what needs to be done here:
            - check if at least 2 users. if not, nothing to do
            - from schema, check all DELETE operations
            - from archive, search if there is any test with a DELETE returning a 403
            - do that for every different endpoint. There are 2 options:
            -- (1) there is such call,  then
            ---     make a copy of the individual
            ----    do a "slice" and remove all calls after the DELETE call (if any)
            -- (2) there is not. need to create it based on successful create resources with authenticated user
            ---    search for create resource for endpoint of DELETE
            ---    do slice and remove calls after create resource endpoint call
            ---    add a DELETE with different user (verify get a 403)
            - append new REST Call action (see mutator classes) for PATCH/PUT with different auth, based
              on action copies from action definitions. should be on same endpoint.
            - need to resolve path element parameters to point to same resolved endpoint as DELETE
            - as PUT/PATCH requires payloads and possible valid query parameters, search from archive an
              existing action that returns 2xx, and copy it to use as starting point
            - execute new test case with fitness function
            - create new testing targets based on status code of new actions
            - add to archive (if needed)
         */

        // Check if at least 2 users. if not, nothing to do
        if ( !checkForAtLeastNumberOfAuthenticatedUsers(2) ) {
            // nothing to test if there are not at least 2 users
            LoggingUtil.getInfoLogger().debug(
                "Security test handleForbiddenDeleteButOkPutOrPatch requires at least 2 authenticated users")
            return
        }

        // From schema, check all DELETE operations, in order to do that
        // obtain DELETE operations in the SUT according to the swagger
        val deleteOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.DELETE)

        // for each endpoint for which there is a DELETE operation
        deleteOperations.forEach { delete ->

            // from archive, search if there is any test with a DELETE returning a 403
            val existing403  =
                RestIndividualSelectorUtils.findIndividualsContainingActionsWithGivenParameters(individualsInSolution,
                                                                                                HttpVerb.DELETE,
                                                                                                delete.path,
                                                                                          "403",
                                                                                    true)
            // individual to choose for test, this is the individual we are going to manipulate
            val individualToChooseForTest : RestIndividual

            // if there is such an individual
            if (existing403.isNotEmpty()) {

                // current individual in the list of existing 403. Since the list is not empty,\
                // we can just get the first item
                // TODO fix methods here and add test for existing 403
                /*

                val currentIndividualWith403 = existing403[0]

                val deleteActionIndex = RestIndividualSelectorUtils.getIndexOfAction(currentIndividualWith403,HttpVerb.DELETE,delete.path, 403)

                val deleteAction = RestIndividualSelectorUtils.getActionWithIndex(currentIndividualWith403, deleteActionIndex)

                // slice the individual in a way that delete all calls after the DELETE request
                individualToChooseForTest = RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(currentIndividualWith403.individual, deleteAction)
                */
            } else {

                // there is not. need to create it based on successful create resources with authenticated user


                val creationPair = RestIndividualSelectorUtils.findIndividualWithEndpointCreationForResource(
                    individualsInSolution,
                    delete.path,
                    true)



                // if neither POST not PUT exists for the endpoint, we need to handle that case specifically

                /*
                if (creation == null) {
                    LoggingUtil.getInfoLogger().debug(
                        "The archive does not contain any successful PUT or POST requests to create for ${delete.path}")
                    return@forEach
                }

                 */

                //TODO fixme

                var actionIndexForCreation = -1

                // if we have already found resource creation pair
                if (creationPair != null) {

                    // find the index of the creation action
                    actionIndexForCreation = creationPair.first.individual.getActionIndex(
                        creationPair.second.verb,
                        creationPair.second.path)


                    // create a new individual with DELETE action with the same endpoint of PUT but different user
                    val individualWithDelete =
                        RestIndividualSelectorUtils.createIndividualWithAnotherActionAddedDifferentAuthRest(sampler,
                            creationPair.first.individual,
                            creationPair.first.individual.seeMainExecutableActions().get(actionIndexForCreation),
                            HttpVerb.DELETE,
                            "deleteActionAfterPut",
                            randomness )

                    // create a new individual from individual with delete that contains PUT with a different endpoint
                    val finalIndividual =
                        RestIndividualSelectorUtils.createIndividualWithAnotherActionAddedDifferentAuthRest(sampler,
                            individualWithDelete,
                            individualWithDelete.seeMainExecutableActions().get(actionIndexForCreation),
                            HttpVerb.PUT,
                            "putActionAfterDelete",
                            randomness )

                    val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(finalIndividual)

                    // add the evaluated individual to the archive
                    if (evaluatedIndividual != null) {
                        archive.addIfNeeded(evaluatedIndividual)
                    }
                }


                /*
                if (verbUsedForCreation != null) {

                    // so we found an individual with a successful PUT or POST,  we will slice all calls after PUT or POST
                    actionIndexForCreation = existingEndpointForCreation.individual.getActionIndex(
                        verbUsedForCreation,
                        delete.path)
                }

                 */

                // create a copy of the existingEndpointForCreation

                /*
                val existingEndpointForCreationCopy = existingEndpointForCreation.copy()
                val actionForCreation = RestIndividualSelectorUtils.getActionWithIndex(existingEndpointForCreation, actionIndexForCreation)

                //RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(existingEndpointForCreationCopy.individual, -1)//actionForCreation)

                // add a DELETE call with another user
                individualToChooseForTest =
                    RestIndividualSelectorUtils.createIndividualWithAnotherActionAddedDifferentAuthRest(sampler,
                                                                     existingEndpointForCreationCopy.individual,
                                                                                              actionForCreation,
                                                                                                HttpVerb.DELETE,
                                                                                                    randomness )

                 */
            }

            // After having a set of requests in which the last one is a DELETE call with another user, add a PUT
            // with another user
            //val deleteActionIndex = individualToChooseForTest.getActionIndex(HttpVerb.DELETE, delete.path)

            //val deleteAction = RestIndividualSelectorUtils.getActionWithIndexRestIndividual(individualToChooseForTest, deleteActionIndex)

            //var individualToAddToSuite = RestIndividualSelectorUtils.
            //createIndividualWithAnotherActionAddedDifferentAuthRest(sampler,
            //                                      individualToChooseForTest,
            //                                                   deleteAction,
            //                                                  HttpVerb.PUT,
            //                                                  randomness)

            // create an individual with the following
            // PUT/POST with one authenticated user userA
            //          For that, we need to find which request is used for resource creation
            // DELETE with the same user which succeeds (userA)
            //          For that, we need to find which request is used for resource deletion
            // PUT/POST with the authenticated user, userA
            //          For that, we just need to copy the previous one
            // DELETE with another user (userB) which should fail
            //          For that, we just need to replace the authenticated user
            // PUT with another user (userB) which should fail, if succeeds, that's a security issue.
            //          For that, we just need to replace the authenticated user

            /*
                FIXME fitness bean must be injected, and not instantiated directly.
                note, issue we have 2 different implementation, need to double-check
             */

            // TODO Fix how to perform fitness function evaluation here.

            // Then evaluate the fitness function to create evaluatedIndividual

            //val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(individualToAddToSuite)

            // add the evaluated individual to the archive
            //if (evaluatedIndividual != null) {
            //    archive.addIfNeeded(evaluatedIndividual)
           // }
        }

    }


    /**
     * Information leakage
     * accessing endpoint with id with not authorized should return 403, even if not exists.
     * otherwise, if returning 404, we can find out that the resource does not exist.
     */
    fun handleNotAuthorizedInAnyCase() {

        // There has to be at least two authenticated users
        if ( !checkForAtLeastNumberOfAuthenticatedUsers(2) ) {
            // nothing to test if there are not at least 2 users
            LoggingUtil.getInfoLogger().debug(
                "Security test handleNotAuthorizedInAnyCase requires at least 2 authenticated users")
            return
        }

        // get all endpoints each user has access to (this can be a function since we need this often)

        // among endpoints userA has access, those endpoints should give 2xx as response

        // find an endpoint userA does not have access but another user has access, this should give 403

        // try with an endpoint that does not exist, this should give 403 as well, not 404.


    }


    /**
     * Wrong Authorization
     *
     * User A, access endpoint X, but get 401 (instead of 403).
     * In theory, a bug. But, could be false positive if A is misconfigured.
     * How to check it?
     * See if on any other endpoint Y we get a 2xx with A.
     * But, maybe Y does not need authentication...
     * so, check if there is any test case for which on Y we get a 401 or 403.
     * if yes, then X is buggy, as should had rather returned 403 for A.
     * This seems to actually happen for familie-ba-sak, new NAV SUT.
     */
    fun handleUnauthorizedInsteadOfForbidden() {

    }

    /**
     *
     */
    private fun checkForAtLeastNumberOfAuthenticatedUsers(numberOfUsers : Int) : Boolean {

        // check the number of authenticated users
        if (authSettings.size(HttpWsAuthenticationInfo::class.java) < numberOfUsers) {

            LoggingUtil.getInfoLogger().debug(
                "Security test for this method requires at least $numberOfUsers authenticated users")
            return false
        }

        return true
    }

}
package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Sampler
import javax.annotation.PostConstruct


/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: Sampler<RestIndividual>

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>


    @PostConstruct
    private fun postInit(){
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase() : Solution<RestIndividual>{

        // we can see what is available from the schema, and then check if already existing a test for it in archive

        addForAccessControl()

        return archive.extractSolution()
    }

    private fun addForAccessControl() {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongon and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */

        accessControlBasedOnRESTGuidelines()
        //accessControlBasedOnDatabaseMonitoring() //TODO
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines() {

        // quite a few rules here that can be defined

        handleForbiddenDeleteButOkPutOrPatch()
        //TODO other
    }

    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenDeleteButOkPutOrPatch() {
        TODO("Not yet implemented")

        /*
            check if at least 2 users.
            here, need to go through archive, for all successful create resources with authenticated user.
            for each of them, do a DELETE with a new user.
            verify if get 403.
            if so, try a PUT and PATCH.
            when doing this, check from archive for test already doing it, as payloads and params of PUT/PATCH
            might have constraints.
            if 2xx, create new fault definition.
            add to archive the new test
         */
    }
}
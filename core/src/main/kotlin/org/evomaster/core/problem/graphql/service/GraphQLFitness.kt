package org.evomaster.core.problem.graphql.service

import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.service.AbstractRestFitness
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie


class GraphQLFitness : HttpWsFitness<GraphQLIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLFitness::class.java)
    }

    override fun doCalculateCoverage(individual: GraphQLIndividual, targets: Set<Int>): EvaluatedIndividual<GraphQLIndividual>? {
        rc.resetSUT()

        val cookies = getCookies(individual)

        //TODO
        //doInitializingActions(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()


        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            val a = individual.seeActions()[i]

            registerNewAction(a, i)

            var ok = false

            if (a is GraphQLAction) {
                ok = handleGraphQLCall(a, actionResults, cookies)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        //TODO
//        if(actionResults.any { it is RestCallResult && it.getTcpProblem() }){
//            /*
//                If there are socket issues, we avoid trying to compute any coverage.
//                The caller might restart the SUT and try again.
//                Hopefully, this should be just a glitch...
//                TODO if we see this happening often, we need to find a proper solution.
//                For example, we could re-run the test, and see if this one always fails,
//                while others in the archive do pass.
//                It could be handled specially in the archive.
//             */
//            return null
//        }

        val dto = updateFitnessAfterEvaluation(targets, individual, fv)
                ?: return null

        handleExtra(dto, fv)

        if (config.baseTaintAnalysisProbability > 0) {
            assert(actionResults.size == dto.additionalInfoList.size)
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return EvaluatedIndividual(fv, individual.copy() as GraphQLIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    private fun handleGraphQLCall(
            action: GraphQLAction,
            actionResults: MutableList<ActionResult>,
            cookies: Map<String, List<NewCookie>>
    ): Boolean {

        /*
            In GraphQL, there are 2 types of methods: Query and Mutation
            - Query can be on either a GET or POST HTTP call
            - Mutation must be on a POST (as changes are not idempotent)

            For simplicity, for now we just do POST for Query as well, as anyway URL have limitations
            on their length
         */

        val gqlcr = GraphQlCallResult()
        actionResults.add(gqlcr)

        /*
            TODO quite a lot of code here is the same as in Rest... need to refactor out into WsHttp
         */

        val response = try {
            createInvocation(action, cookies).invoke()
        } catch (e: ProcessingException) {

            /*
                this could have happened for example if call ends up in an infinite redirection loop.
                However, as we no longer follow 3xx automatically, it should not happen anymore
             */
            when {
                TcpUtils.isTooManyRedirections(e) -> {
                    gqlcr.setInfiniteLoop(true)
                    gqlcr.setErrorMessage(e.cause!!.message!!)
                    return false
                }
                TcpUtils.isTimeout(e) -> {
                    /*
                        This is very tricky. In theory it shouldn't happen that a REST call
                        does timeout (eg 10 seconds). But it might happen due to glitch,
                        or if very slow hardware. If it is a glitch, we do not want to
                        kill whole EM process, as might not happen again. If it is a
                        constant, we want to avoid using such test if possible, as it
                        would kill performance.
                        In any case, a generated test should never check assertions on time,
                        eg expect that a is SocketTimeoutException thrown. Not only because
                        maybe it was just a glitch, but also because the test might be run
                        on different machines (remote CI vs local development PC) with
                        different performance (and so the test would become flaky)
                     */
                    gqlcr.setTimedout(true)
                    statistics.reportTimeout()
                    return false
                }
                TcpUtils.isOutOfEphemeralPorts(e) -> {
                    /*
                        This could happen if for any reason we run out of ephemeral ports.
                        In such a case, we wait X seconds, and try again, as OS might have released ports
                        meanwhile.
                        And while we are at it, let's release any hanging network resource
                     */
                    client.close() //make sure to release any resource
                    client = ClientBuilder.newClient()

                    TcpUtils.handleEphemeralPortIssue()

                    createInvocation(action, cookies).invoke()
                }
                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    /*
                        This should not really happen... but it does :( at least on Windows...
                     */
                    log.warn("TCP connection to SUT problem: ${e.cause!!.message}")
                    gqlcr.setTcpProblem(true)
                    return false
                }
                else -> throw e
            }
        }

        gqlcr.setStatusCode(response.status)

        handlePossibleConnectionClose(response)

        try {
            if (response.hasEntity()) {
                if (response.mediaType != null) {
                    gqlcr.setBodyType(response.mediaType)
                }
                /*
                  Note: here we are always assuming a JSON, so reading as string should be fine
                 */
                val body = response.readEntity(String::class.java)

                if (body.length < configuration.maxResponseByteSize) {
                    gqlcr.setBody(body)
                } else {
                    LoggingUtil.uniqueWarn(log,
                            "A very large response body was retrieved from the action '${action.methodName}'." +
                                    " If that was expected, increase the 'maxResponseByteSize' threshold" +
                                    " in the configurations.")
                    gqlcr.setTooLargeBody(true)
                }
            }
        } catch (e: Exception) {

            if(e is ProcessingException && TcpUtils.isTimeout(e)){
                gqlcr.setTimedout(true)
                statistics.reportTimeout()
                return false
            } else {
               log.warn("Failed to parse HTTP response: ${e.message}")
            }
        }

        if (response.status == 401 && action.auth !is NoAuth) {
            //this would likely be a misconfiguration in the SUT controller
            log.warn("Got 401 although having auth for '${action.auth.name}'")
        }

        return true
    }


    fun createInvocation(a: GraphQLAction, cookies: Map<String, List<NewCookie>>): Invocation {
        val baseUrl = getBaseUrl()

        val path = "/graphql"

        val locationHeader = null

        val fullUri = EMTestUtils.resolveLocation(locationHeader, baseUrl + path)!!
                .let {
                    /*
                        TODO this will be need to be done properly, and check if
                        it is or not a valid char.
                        Furthermore, likely needed to be done in resolveLocation,
                        or at least check how RestAssured would behave
                     */
                    //it.replace("\"", "")
                    GeneUtils.applyEscapes(it, GeneUtils.EscapeMode.URI, configuration.outputFormat)
                }


        val builder = client.target(fullUri).request("application/json")

        a.auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val prechosenAuthHeaders = a.auth.headers.map { it.name }


        if (a.auth.cookieLogin != null) {
            val list = cookies[a.auth.cookieLogin!!.username]
            if (list == null || list.isEmpty()) {
                log.warn("No cookies for ${a.auth.cookieLogin!!.username}")
            } else {
                list.forEach {
                    builder.cookie(it.toCookie())
                }
            }
        }


        val returnGene = a.parameters.find { p -> p is GQReturnParam }?.gene
        //in GraphQL, there is ALWAYS a return type
                ?: throw RuntimeException("ERROR: not specified return type")
        val selection = GeneUtils.getBooleanSelection(returnGene)

        //this might be optional
        val inputGenes = a.parameters.filterIsInstance<GQInputParam>().map { it.gene }

        //TODO inputGenes
       val query = "{${selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)}}"

        val bodyEntity = Entity.json("""
            {"query" : "$query","variables":null}
        """.trimIndent())

        val invocation = builder.buildPost(bodyEntity)
        return invocation
    }

}

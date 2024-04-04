package org.evomaster.core.problem.graphql.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.graphql.*
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.NewCookie


open class GraphQLFitness : HttpWsFitness<GraphQLIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLFitness::class.java)
        private val mapper: ObjectMapper = ObjectMapper()
    }

    override fun doCalculateCoverage(
        individual: GraphQLIndividual,
        targets: Set<Int>,
        allCovered: Boolean
    ): EvaluatedIndividual<GraphQLIndividual>? {
        rc.resetSUT()

        val cookies = AuthUtils.getCookies(client, getBaseUrl(), individual)
        val tokens = AuthUtils.getTokens(client, getBaseUrl(), individual)

        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions().filterIsInstance<SqlAction>(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        val actions = individual.seeMainExecutableActions()

        //run the test, one action at a time
        for (i in actions.indices) {

            val a = actions[i]

            registerNewAction(a, i)

            val ok = handleGraphQLCall(a, actionResults, cookies, tokens)
            actionResults.filterIsInstance<GraphQlCallResult>()[i].stopping = !ok

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

        val dto = updateFitnessAfterEvaluation(targets, allCovered, individual, fv)
            ?: return null

        handleExtra(dto, fv)

        val graphQLActionResults = actionResults.filterIsInstance<GraphQlCallResult>()
        handleResponseTargets(fv, actions, graphQLActionResults, dto.additionalInfoList)

        if(!allCovered) {
            if (config.isEnabledTaintAnalysis()) {
                Lazy.assert { graphQLActionResults.size == dto.additionalInfoList.size }
                TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness, config.enableSchemaConstraintHandling)
            }
        }

        return EvaluatedIndividual(
            fv,
            individual.copy() as GraphQLIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    protected fun handleResponseTargets(
        fv: FitnessValue,
        actions: List<GraphQLAction>,
        actionResults: List<ActionResult>,
        additionalInfoList: List<AdditionalInfoDto>
    ) {

        (actionResults.indices)
            .filter { actions[it] is GraphQLAction }
            .filter { actionResults[it] is GraphQlCallResult }
            .forEach {
                val result = actionResults[it] as GraphQlCallResult
                val status = result.getStatusCode() ?: -1
                val name = actions[it].getName()

                //objective for HTTP specific status code
                val statusId = idMapper.handleLocalTarget("$status:$name")
                fv.updateTarget(statusId, 1.0, it)

                val location5xx: String? = getlocation5xx(status, additionalInfoList, it, result, name)

                handleAdditionalStatusTargetDescription(fv, status, name, it, location5xx)

                /*
                      we also have to check the actual response body...
                      but, unfortunately, currently there is no way to distinguish between user and server errors
                      https://github.com/graphql/graphql-spec/issues/698
                 */
                handleGraphQLErrors(fv, name, it, result, additionalInfoList)

            }
    }

    /**
     *  handle targets with whether there exist errors in a gql action
     */
    private fun handleGraphQLErrors(
        fv: FitnessValue,
        name: String,
        actionIndex: Int,
        result: GraphQlCallResult,
        additionalInfoList: List<AdditionalInfoDto>
    ) {
        val errorId = idMapper.handleLocalTarget(idMapper.getGQLErrorsDescriptiveWithMethodName(name))
        val okId = idMapper.handleLocalTarget(idMapper.getGQLNoErrors(name))

        val anyError = hasErrors(result)

        if (anyError) {


            fv.updateTarget(errorId, 1.0, actionIndex)
            fv.updateTarget(okId, 0.5, actionIndex)
            val graphQlError = getGraphQLErrorWithLineInfo(additionalInfoList, actionIndex, result, name)
            if (graphQlError != null) {
                val errorlineId = idMapper.handleLocalTarget(graphQlError)
                fv.updateTarget(errorlineId, 1.0, actionIndex)
            }

        } else {
            fv.updateTarget(okId, 1.0, actionIndex)
            fv.updateTarget(errorId, 0.5, actionIndex)
        }
    }

    /**
     * @return description for graphql error with lastExecutedStatement
     */
    open fun getGraphQLErrorWithLineInfo(
        additionalInfoList: List<AdditionalInfoDto>,
        indexOfAction: Int,
        result: GraphQlCallResult,
        name: String
    ): String? {

        // handle with last statement
        val last = additionalInfoList[indexOfAction].lastExecutedStatement ?: DEFAULT_FAULT_CODE
        result.setLastStatementWhenGQLErrors(last)
        // shall we add additional target with last?
        return idMapper.getGQLErrorsDescriptiveWithMethodNameAndLine(
            line = last,
            method = name
        )
    }

    private fun hasErrors(result: GraphQlCallResult): Boolean {

        val errors = extractBodyInGraphQlResponse(result)?.findPath("errors") ?: return false

        return !errors.isEmpty || !errors.isMissingNode
    }

    private fun extractBodyInGraphQlResponse(result: GraphQlCallResult): JsonNode? {
        return try {
            result.getBody()?.run { mapper.readTree(result.getBody()) }
        } catch (e: JsonProcessingException) {
            null
        }
    }

    private fun handleAdditionalStatusTargetDescription(
        fv: FitnessValue,
        status: Int,
        name: String,
        indexOfAction: Int,
        location5xx: String?
    ) {

        val okId = idMapper.handleLocalTarget("HTTP_SUCCESS:$name")
        val faultId = idMapper.handleLocalTarget("HTTP_FAULT:$name")

        /*
            GraphQL is not linked to HTTP.
            So, no point to create specific testing targets for all HTTP status codes.
            Most GraphQL implementations will actually return 200 even in the case of errors, including
            crashed from thrown exceptions...
            However, if in case there is a 500, we still to report it.

            Still, the server could return other codes like 503 when out of resources...
            or 401/403 when wrong auth...
         */

        //OK -> 5xx being better than 4xx, as code executed
        //FAULT -> 4xx worse than 2xx (can't find bugs if input is invalid)
        if (status in 200..299) {
            fv.updateTarget(okId, 1.0, indexOfAction)
            fv.updateTarget(faultId, 0.5, indexOfAction)
        } else {
            fv.updateTarget(okId, 0.5, indexOfAction)
            fv.updateTarget(faultId, 1.0, indexOfAction)
        }

        if (status == 500) {
            Lazy.assert {
                location5xx != null || config.blackBox
            }
            /*
                500 codes "might" be bugs. To distinguish between different bugs
                that crash the same endpoint, we need to know what was the last
                executed statement in the SUT.
                So, we create new targets for it.
            */
            val postfix = if(location5xx==null) name else "${location5xx!!} $name"
            val descriptiveId = idMapper.getFaultDescriptiveIdFor500(postfix)
            val bugId = idMapper.handleLocalTarget(descriptiveId)
            fv.updateTarget(bugId, 1.0, indexOfAction)

        }
    }

    protected fun handleGraphQLCall(
        action: GraphQLAction,
        actionResults: MutableList<ActionResult>,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>
    ): Boolean {

        /*
            In GraphQL, there are 2 types of methods: Query and Mutation
            - Query can be on either a GET or POST HTTP call
            - Mutation must be on a POST (as changes are not idempotent)

            For simplicity, for now we just do POST for Query as well, as anyway URL have limitations
            on their length
         */

        searchTimeController.waitForRateLimiter()

        val gqlcr = GraphQlCallResult(action.getLocalId())
        actionResults.add(gqlcr)

        /*
            TODO quite a lot of code here is the same as in Rest... need to refactor out into WsHttp
         */

        val response = try {
            createInvocation(action, cookies, tokens).invoke()
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

                    createInvocation(action, cookies, tokens).invoke()
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
                    LoggingUtil.uniqueWarn(
                        log,
                        "A very large response body was retrieved from the action '${action.methodName}'." +
                                " If that was expected, increase the 'maxResponseByteSize' threshold" +
                                " in the configurations."
                    )
                    gqlcr.setTooLargeBody(true)
                }
            }
        } catch (e: Exception) {

            if (e is ProcessingException && TcpUtils.isTimeout(e)) {
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


    fun createInvocation(
        a: GraphQLAction,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>
    ): Invocation {

        val uri = if (config.blackBox) {
            config.bbTargetUrl
        } else {
            infoDto.baseUrlOfSUT.plus(infoDto.graphQLProblem.endpoint)
        }
        val fullUri = GeneUtils.applyEscapes(uri, GeneUtils.EscapeMode.URI, configuration.outputFormat)

        val builder = client.target(fullUri).request("application/json")

        handleHeaders(a, builder, cookies, tokens)

        val bodyEntity = GraphQLUtils.generateGQLBodyEntity(a, config.outputFormat) ?: Entity.json(" ")
        val invocation = builder.buildPost(bodyEntity)
        return invocation
    }


}

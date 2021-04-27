package org.evomaster.core.problem.graphql.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.*
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.*
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.NewCookie


class GraphQLFitness : HttpWsFitness<GraphQLIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLFitness::class.java)
        private val mapper: ObjectMapper = ObjectMapper()
    }

    override fun doCalculateCoverage(individual: GraphQLIndividual, targets: Set<Int>): EvaluatedIndividual<GraphQLIndividual>? {
        rc.resetSUT()

        val cookies = getCookies(individual)

        doInitializingActions(individual)

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

        handleResponseTargets(fv, individual.seeActions(), actionResults, dto.additionalInfoList)


        if (config.baseTaintAnalysisProbability > 0) {
            assert(actionResults.size == dto.additionalInfoList.size)
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return EvaluatedIndividual(fv, individual.copy() as GraphQLIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    private fun handleResponseTargets(fv: FitnessValue, actions: List<GraphQLAction>, actionResults: List<ActionResult>, additionalInfoList: List<AdditionalInfoDto>) {

        (0 until actionResults.size)
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


                    //handleAdditionalOracleTargetDescription(fv, actions, result, name, it)
                }
    }

    /**
     *  handle targets with whether there exist errors in a gql action
     */
    private fun handleGraphQLErrors(fv: FitnessValue, name: String, actionIndex: Int, result: GraphQlCallResult, additionalInfoList: List<AdditionalInfoDto>) {
        val errorId = idMapper.handleLocalTarget(idMapper.getGQLErrorsDescriptiveWithMethodName(name))
        val okId = idMapper.handleLocalTarget("GQL_NO_ERRORS:$name")

        val anyError = hasErrors(result)

        if (anyError) {
            fv.updateTarget(errorId, 1.0, actionIndex)
            fv.updateTarget(okId, 0.5, actionIndex)


            // handle with last statement
            val last = additionalInfoList[actionIndex].lastExecutedStatement ?: DEFAULT_FAULT_CODE
            result.setLastStatementWhenGQLErrors(last)

            // shall we add additional target with last?
            val errorlineId = idMapper.handleLocalTarget(idMapper.getGQLErrorsDescriptiveWithMethodNameAndLine(line = last, method = name))
            fv.updateTarget(errorlineId, 1.0, actionIndex)

        } else {
            fv.updateTarget(okId, 1.0, actionIndex)
            fv.updateTarget(errorId, 0.5, actionIndex)
        }


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

    private fun handleAdditionalStatusTargetDescription(fv: FitnessValue, status: Int, name: String, indexOfAction: Int, location5xx: String?) {

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
                location5xx != null
            }
            /*
                500 codes "might" be bugs. To distinguish between different bugs
                that crash the same endpoint, we need to know what was the last
                executed statement in the SUT.
                So, we create new targets for it.
            */
            val descriptiveId = idMapper.getFaultDescriptiveIdFor500("${location5xx!!} $name")
            val bugId = idMapper.handleLocalTarget(descriptiveId)
            fv.updateTarget(bugId, 1.0, indexOfAction)
        }
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

        //TOdo check empty return type
        val returnGene = a.parameters.find { p -> p is GQReturnParam }?.gene

        val inputGenes = a.parameters.filterIsInstance<GQInputParam>().map { it.gene }

        var bodyEntity: Entity<String> = Entity.json(" ")

        if (a.methodType == GQMethodType.QUERY) {

            if (inputGenes.isNotEmpty()) {

                val printableInputGene: MutableList<String> = GraphQLUtils.getPrintableInputGene(inputGenes)

                var printableInputGenes = GraphQLUtils.getPrintableInputGenes(printableInputGene)

                //primitive type in Return
                bodyEntity = if (returnGene == null) {
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)         } ","variables":null}
                """.trimIndent())

                } else {
                    val query = GraphQLUtils.getQuery(returnGene, a)
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)  $query       } ","variables":null}
                """.trimIndent())

                }
            } else {//request without arguments and primitive type
                bodyEntity = if (returnGene == null) {
                    Entity.json("""
                    {"query" : "  { ${a.methodName}       } ","variables":null}
                """.trimIndent())

                } else {
                    var query = GraphQLUtils.getQuery(returnGene, a)
                    Entity.json("""
                   {"query" : " {  ${a.methodName}  $query   }   ","variables":null}
                """.trimIndent())
                }
            }
        } else if (a.methodType == GQMethodType.MUTATION) {
            val printableInputGene: MutableList<String> = GraphQLUtils.getPrintableInputGene(inputGenes)

            val printableInputGenes = GraphQLUtils.getPrintableInputGenes(printableInputGene)

            /*
                Need a check with Asma
                for mutation which does not have any param, there is no need for ()
                e.g., createX:X!
                      mutation{
                        createX{
                            ...
                        }
                      }
             */
            val inputParams = if (printableInputGene.isEmpty()) "" else "($printableInputGenes)"
            bodyEntity = if (returnGene == null) {//primitive type
                Entity.json("""
                {"query" : " mutation{ ${a.methodName}  $inputParams         } ","variables":null}
            """.trimIndent())

            } else {
                val mutation = GraphQLUtils.getMutation(returnGene, a)
                Entity.json("""
                { "query" : "mutation{    ${a.methodName}  $inputParams    $mutation    }","variables":null}
            """.trimIndent())

            }
        }
        val invocation = builder.buildPost(bodyEntity)
        return invocation
    }




}

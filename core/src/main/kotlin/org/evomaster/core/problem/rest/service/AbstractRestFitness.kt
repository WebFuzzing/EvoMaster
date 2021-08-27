package org.evomaster.core.problem.rest.service

import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


abstract class AbstractRestFitness<T> : HttpWsFitness<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestFitness::class.java)
    }



    /**
     * Based on what executed by the test, we might need to add new genes to the individual.
     * This for example can happen if we detected that the test is using headers or query
     * params that were not specified in the Swagger schema
     */
    open fun expandIndividual(
            individual: RestIndividual,
            additionalInfoList: List<AdditionalInfoDto>,
            actionResults: List<ActionResult>
    ) {

        if (individual.seeActions().size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
            log.warn("Length mismatch between ${individual.seeActions().size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in 0 until additionalInfoList.size) {

            val action = individual.seeActions()[i]
            val info = additionalInfoList[i]

            if (action !is RestCallAction) {
                continue
            }

            val result = actionResults[i] as RestCallResult

            /*
                Those are OptionalGenes, which MUST be off by default.
                We are changing the genotype, but MUST not change the phenotype.
                Otherwise, the fitness value we just computed would be wrong.

                TODO: should update default action templates in Sampler
             */

            info.headers
                    .filter { name ->
                        !action.parameters.any { it is HeaderParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        val gene = StringGene(it).apply { randomize(randomness, false, listOf()) }
                        action.parameters.add(HeaderParam(it, OptionalGene(it, gene, false, requestSelection = true)))
                    }

            info.queryParameters
                    .filter { name ->
                        !action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        val gene = StringGene(it).apply { randomize(randomness, false, listOf()) }
                        action.parameters.add(QueryParam(it, OptionalGene(it, gene, false, requestSelection = true)))
                    }

            if(result.getStatusCode() == 415){
                /*
                    In theory, this should not happen.
                    415 means the media type of the sent payload is wrong.
                    There is no point for EvoMaster to do that, ie sending an XML to
                    an endpoint that expects a JSON.
                    Having such kind of test would be pretty pointless.

                    However, a POST/PUT could expect a payload and, if that is not specified
                    in OpenAPI, we could get a 415 when sending no data.
                 */
                if(action.parameters.none{ it is BodyParam}){

                    val obj = ObjectGene("body", listOf())

                    val body = BodyParam(obj,
                             // TODO could look at "Accept" header instead of defaulting to JSON
                            EnumGene("contentType", listOf("application/json")))

                    val update = UpdateForBodyParam(body)

                    action.parameters.add(update)
                }
            }


            val dtoNames = info.parsedDtoNames;

            val noBody = action.parameters.none{ it is BodyParam}
            val emptyObject = !noBody &&
                    // this is the case of 415 handling
                    action.parameters.find { it is BodyParam }!!.let {
                        it.gene is ObjectGene && it.gene.fields.isEmpty()
                    }

            if(info.rawAccessOfHttpBodyPayload == true
                    && dtoNames.isNotEmpty()
                    && (noBody || emptyObject)
            ){
                /*
                    The SUT tried to read the HTTP body payload, but there is no info
                    about it in the schema. This can happen when payloads are dynamically
                    loaded directly in the business logic of the SUT, and automated tools
                    like SpringDoc/SpringFox failed to infer what is read

                    TODO could handle other types besides JSON
                    TODO what to do if more than 1 DTO are registered?
                         Likely need a new MultiOptionGene similar to DisjunctionListRxGene
                 */
                if(dtoNames.size > 1){
                    LoggingUtil.uniqueWarn(log, "More than 1 DTO option: [${dtoNames.sorted().joinToString(", ")}]")
                }
                val name = dtoNames.first()
                val obj = getObjectGeneForDto(name)
                obj.randomize(randomness, false, listOf())

                val body = BodyParam(obj, EnumGene("contentType", listOf("application/json")))
                val update = UpdateForBodyParam(body)
                action.parameters.add(update)
            }
        }
    }

    private fun getObjectGeneForDto(name: String) : Gene{

        if(!infoDto.unitsInfoDto.parsedDtos.containsKey(name)){
            /*
                parsedDto info is update throughout the search.
                so, if info is missing, we re-fetch the whole data.
                Would be more efficient to just fetch new data, but,
                as this will happens seldom (at most N times for N dtos),
                no much point in optimizing it
             */
            infoDto = rc.getSutInfo()!!

            if(!infoDto.unitsInfoDto.parsedDtos.containsKey(name)){
                throw RuntimeException("BUG: info for DTO $name is not available in the SUT driver")
            }
        }

        val schema : String = infoDto.unitsInfoDto.parsedDtos.get(name)!!
        //TODO neeed to check: referType is same with the name?
        return RestActionBuilderV3.createObjectGeneForDTO(name, schema, name)
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    fun handleResponseTargets(
            fv: FitnessValue,
            actions: List<RestCallAction>,
            actionResults: List<ActionResult>,
            additionalInfoList: List<AdditionalInfoDto>) {

        (0 until actionResults.size)
                .filter { actions[it] is RestCallAction }
                .filter { actionResults[it] is RestCallResult }
                .forEach {
                    val result = actionResults[it] as RestCallResult
                    val status = result.getStatusCode() ?: -1
                    val name = actions[it].getName()

                    //objective for HTTP specific status code
                    val statusId = idMapper.handleLocalTarget("$status:$name")
                    fv.updateTarget(statusId, 1.0, it)

                    val location5xx : String? = getlocation5xx(status, additionalInfoList, it, result, name)

                    handleAdditionalStatusTargetDescription(fv, status, name, it, location5xx)

                    handleAdditionalOracleTargetDescription(fv, actions, result, name, it)

                }
    }


    fun handleAdditionalOracleTargetDescription(fv: FitnessValue, actions: List<RestCallAction>, result : RestCallResult, name: String, indexOfAction : Int){
        /*
           Objectives for the two partial oracles implemented thus far.
        */
        val call = actions[indexOfAction] as RestCallAction
        val oracles = writer.getPartialOracles().activeOracles(call, result)
        oracles.filter { it.value }.forEach { entry ->
            val oracleId = idMapper.getFaultDescriptiveIdForPartialOracle("${entry.key} $name")
            val bugId = idMapper.handleLocalTarget(oracleId)
            fv.updateTarget(bugId, 1.0, indexOfAction)
        }
    }


    fun handleAdditionalStatusTargetDescription(fv: FitnessValue, status : Int, name: String, indexOfAction : Int, location5xx: String?){
        /*
           Objectives for results on endpoints.
           Problem: we might get a 4xx/5xx, but then no gradient to keep sampling for
           that endpoint. If we get 2xx, and full coverage, then no gradient to try
           to keep sampling that endpoint to get a 5xx
        */
        val okId = idMapper.handleLocalTarget("HTTP_SUCCESS:$name")
        val faultId = idMapper.handleLocalTarget("HTTP_FAULT:$name")

        //OK -> 5xx being better than 4xx, as code executed
        //FAULT -> 4xx worse than 2xx (can't find bugs if input is invalid)
        if (status in 200..299) {
            fv.updateTarget(okId, 1.0, indexOfAction)
            fv.updateTarget(faultId, 0.5, indexOfAction)
        } else if (status in 400..499) {
            fv.updateTarget(okId, 0.1, indexOfAction)
            fv.updateTarget(faultId, 0.1, indexOfAction)
        } else if (status in 500..599) {
            fv.updateTarget(okId, 0.5, indexOfAction)
            fv.updateTarget(faultId, 1.0, indexOfAction)
        }

        if (status == 500){
            /*
                500 codes "might" be bugs. To distinguish between different bugs
                that crash the same endpoint, we need to know what was the last
                executed statement in the SUT.
                So, we create new targets for it.

                However, such info is missing in black-box testing
            */
            Lazy.assert {
                location5xx != null || config.blackBox
            }
            val postfix = if(location5xx==null) name else "${location5xx!!} $name"
            val descriptiveId = idMapper.getFaultDescriptiveIdFor500(postfix)
            val bugId = idMapper.handleLocalTarget(descriptiveId)
            fv.updateTarget(bugId, 1.0, indexOfAction)
        }
    }



    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    protected fun handleRestCall(a: RestCallAction,
                                 actionResults: MutableList<ActionResult>,
                                 chainState: MutableMap<String, String>,
                                 cookies: Map<String, List<NewCookie>>,
                                tokens: Map<String,String>)
            : Boolean {

        searchTimeController.waitForRateLimiter()

        val rcr = RestCallResult()
        actionResults.add(rcr)

        val response = try {
            createInvocation(a, chainState, cookies, tokens).invoke()
        } catch (e: ProcessingException) {

            log.debug("There has been an issue in the evaluation of a test: {}", e)

            /*
                this could have happened for example if call ends up in an infinite redirection loop.
                However, as we no longer follow 3xx automatically, it should not happen anymore
             */
            when {
                TcpUtils.isTooManyRedirections(e) -> {
                    rcr.setInfiniteLoop(true)
                    rcr.setErrorMessage(e.cause!!.message!!)
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
                    rcr.setTimedout(true)
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

                    createInvocation(a, chainState, cookies, tokens).invoke()
                }
                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    /*
                        This should not really happen... but it does :( at least on Windows...
                     */
                    log.warn("TCP connection to SUT problem: ${e.cause!!.message}")
                    rcr.setTcpProblem(true)
                    return false
                }
                else -> throw e
            }
        }

        rcr.setStatusCode(response.status)

        handlePossibleConnectionClose(response)

        try {
            if (response.hasEntity()) {
                if (response.mediaType != null) {
                    rcr.setBodyType(response.mediaType)
                }
                /*
                    FIXME should read as byte[]
                 */
                val body = response.readEntity(String::class.java)

                if (body.length < configuration.maxResponseByteSize) {
                    rcr.setBody(body)
                } else {
                    LoggingUtil.uniqueWarn(log,
                            "A very large response body was retrieved from the endpoint '${a.path}'." +
                            " If that was expected, increase the 'maxResponseByteSize' threshold" +
                            " in the configurations.")
                    rcr.setTooLargeBody(true)
                }
            }
        } catch (e: Exception) {

            if(e is ProcessingException && TcpUtils.isTimeout(e)){
                rcr.setTimedout(true)
                statistics.reportTimeout()
                return false
            } else {
                log.warn("Failed to parse HTTP response: ${e.message}")
            }
        }

        if (response.status == 401 && a.auth !is NoAuth) {
            /*
                if the endpoint itself is to get auth info, we might exclude auth check for it
                eg,
                    the auth is Login with foo,
                    then the action is to Login with a generated account (eg bar)
                    thus, the response would likely be 401
             */
            if (!a.auth.excludeAuthCheck(a)){
                //this would likely be a misconfiguration in the SUT controller
                log.warn("Got 401 although having auth for '${a.auth.name}'")
            }
        }


        if (!handleSaveLocation(a, response, rcr, chainState)) return false

        return true
    }


    private fun createInvocation(a: RestCallAction,
                                 chainState: MutableMap<String, String>,
                                 cookies: Map<String, List<NewCookie>>,
                                 tokens: Map<String,String>
    ): Invocation {

        val baseUrl = getBaseUrl()

        val path = a.resolvedPath()

        val locationHeader = if (a.locationId != null) {
            chainState[locationName(a.locationId!!)]
                    ?: throw IllegalStateException("Call expected a missing chained 'location'")
        } else {
            null
        }

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


        val builder = if(a.produces.isEmpty()){
            log.debug("No 'produces' type defined for {}", path)
            client.target(fullUri).request("*/*")

        } else {
            /*
                TODO: This only considers the first in the list of produced responses
                This is fine for endpoints that only produce one type of response.
                Could be a problem in future
            */
            client.target(fullUri).request(a.produces.first())
        }

        handleAuth(a, builder, cookies, tokens)

        /*
            TODO: need to handle "accept" of returned resource
         */


        val body = a.parameters.find { p -> p is BodyParam }
        val forms = a.getBodyFormData()

        if (body != null && forms != null) {
            throw IllegalStateException("Issue in OpenAPI configuration: both Body and FormData definitions in the same endpoint")
        }

        val bodyEntity = if (body != null && body is BodyParam) {
            val mode = when {
                body.isJson() -> GeneUtils.EscapeMode.JSON
                body.isXml() -> GeneUtils.EscapeMode.XML
                body.isForm() -> GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED
                body.isTextPlain() -> GeneUtils.EscapeMode.TEXT
                else -> throw IllegalStateException("Cannot handle body type: " + body.contentType())
            }
            Entity.entity(body.gene.getValueAsPrintableString(mode = mode, targetFormat = configuration.outputFormat), body.contentType())
        } else if (forms != null) {
            Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if (a.verb == HttpVerb.PUT || a.verb == HttpVerb.PATCH) {
            /*
                PUT and PATCH must have a payload. But it might happen that it is missing in the Swagger schema
                when objects like WebRequest are used. So we default to urlencoded
             */
            Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if(a.verb == HttpVerb.POST && body == null){
            /*
                POST does not enforce payload (isn't it?). However seen issues with Dotnet that gives
                411 if  Content-Length is missing...
             */
            //builder.header("Content-Length", 0)
            // null
            /*
                yet another critical bug in Jersey that it ignores that header (verified with WireShark)
             */
            Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else {
            null
        }

        val invocation = when (a.verb) {
            HttpVerb.GET -> builder.buildGet()
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.DELETE -> builder.buildDelete()
            HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.HEAD -> builder.build("HEAD")
            HttpVerb.TRACE -> builder.build("TRACE")
        }
        return invocation
    }



    private fun handleSaveLocation(a: RestCallAction, response: Response, rcr: RestCallResult, chainState: MutableMap<String, String>): Boolean {
        if (a.saveLocation) {

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
                /*
                    If this failed, and following actions require the "location" header
                    of this call, there is no point whatsoever to continue evaluating
                    the remaining calls
                 */
                rcr.stopping = true
                return false
            }

            val name = locationName(a.path.lastElement())
            var location = response.getHeaderString("location")

            if (location == null) {
                /*
                    Excluding bugs, this might happen if API was
                    designed to return the created resource, from
                    which an "id" can be extracted.
                    This is usually not a good practice, but it can
                    happen. So, here we "heuristically" (cannot be 100% sure)
                    check if this is indeed the case
                 */
                val id = rcr.getResourceId()

                if (id != null && hasParameterChild(a)) {
                    location = a.resolvedPath() + "/" + id
                    rcr.setHeuristicsForChainedLocation(true)
                }
            }

            //save location for the following REST calls
            chainState[name] = location ?: ""
        }
        return true
    }


    abstract fun hasParameterChild(a: RestCallAction): Boolean

    private fun locationName(id: String): String {
        return "location_$id"
    }

    protected fun restActionResultHandling(
        individual: RestIndividual, targets: Set<Int>, actionResults: List<ActionResult>, fv: FitnessValue) : TestResultsDto?{

        if(actionResults.any { it is RestCallResult && it.getTcpProblem() }){
            /*
                If there are socket issues, we avoid trying to compute any coverage.
                The caller might restart the SUT and try again.
                Hopefully, this should be just a glitch...
                TODO if we see this happening often, we need to find a proper solution.
                For example, we could re-run the test, and see if this one always fails,
                while others in the archive do pass.
                It could be handled specially in the archive.
             */
            return null
        }

        val dto = updateFitnessAfterEvaluation(targets, individual as T, fv)
            ?: return null

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.seeActions(), actionResults, dto.additionalInfoList)

        if (config.expandRestIndividuals) {
            expandIndividual(individual, dto.additionalInfoList, actionResults)
        }

        if (config.baseTaintAnalysisProbability > 0) {
            assert(actionResults.size == dto.additionalInfoList.size)
            //TODO add taint analysis for resource-based solution
            TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness)
        }

        return dto
    }
}
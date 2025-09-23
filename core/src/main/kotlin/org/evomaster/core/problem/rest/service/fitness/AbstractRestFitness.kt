package org.evomaster.core.problem.rest.service.fitness

import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.test.utils.EMTestUtils
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.HostnameResolutionInfo
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceInfo
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.problem.httpws.service.HttpWsFitness
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.link.RestLinkValueUpdater
import org.evomaster.core.problem.rest.oracle.HttpSemanticsOracle
import org.evomaster.core.problem.rest.oracle.RestSchemaOracle
import org.evomaster.core.problem.rest.oracle.RestSecurityOracle
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler.Companion.CALL_TO_SWAGGER_ID
import org.evomaster.core.problem.rest.service.RestIndividualBuilder
import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.evomaster.core.problem.util.ParserDtoUtil
import org.evomaster.core.remote.HttpClientFactory
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.DataPool
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie


abstract class AbstractRestFitness : HttpWsFitness<RestIndividual>() {

    // TODO: This will moved under ApiWsFitness once RPC and GraphQL support is completed
    @Inject
    protected lateinit var externalServiceHandler: HttpWsExternalServiceHandler

    @Inject
    protected lateinit var harvestResponseHandler: HarvestActualHttpWsResponseHandler

    @Inject
    protected lateinit var ssrfAnalyser: SSRFAnalyser

    @Inject
    protected lateinit var responsePool: DataPool

    @Inject
    protected lateinit var builder: RestIndividualBuilder

    @Inject
    protected lateinit var responseClassifier: AIResponseClassifier

    @Inject
    protected lateinit var callGraphService: CallGraphService


    private lateinit var schemaOracle: RestSchemaOracle

    @PostConstruct
    fun initBean(){
        schemaOracle = RestSchemaOracle((sampler as AbstractRestSampler).schemaHolder)
    }


    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestFitness::class.java)

        //see org.springframework.http.HttpHeaders

        val knownHttpHeaders = setOf(
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Patch",
            "Accept-Ranges",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Origin",
            "Access-Control-Expose-Headers",
            "Access-Control-Max-Age",
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method",
            "Age",
            "Allow",
            "Authorization",
            "Cache-Control",
            "Connection",
            "Content-Encoding",
            "Content-Disposition",
            "Content-Language",
            "Content-Length",
            "Content-Location",
            "Content-Range",
            "Content-Type",
            "Cookie",
            "Date",
            "ETag",
            "Expect",
            "Expires",
            "From",
            "Host",
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            "Last-Modified",
            "Link",
            "Location",
            "Max-Forwards",
            "Origin",
            "Pragma",
            "Proxy-Authenticate",
            "Proxy-Authorization",
            "Range",
            "Referer",
            "Retry-After",
            "Server",
            "Set-Cookie",
            "Set-Cookie2",
            "TE",
            "Trailer",
            "Transfer-Encoding",
            "Upgrade",
            "User-Agent",
            "Vary",
            "Via",
            "Warning",
            "WWW-Authenticate"
        ).map { it.lowercase() }.toSet()
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

        if (individual.seeAllActions().size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
            log.warn("Length mismatch between ${individual.seeAllActions().size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in additionalInfoList.indices) {

            val action = individual.seeAllActions()[i]
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
                .filter {
                    //ignore common HTTP headers which could mess up the requests
                    !knownHttpHeaders.contains(it.lowercase())
                }
                .forEach {
                    val gene = StringGene(it)
                    action.addParam(
                        HeaderParam(it,
                            OptionalGene(it, gene, false, requestSelection = true).apply { doInitialize() })
                    )
                }

            info.queryParameters
                .filter { name ->
                    //if parameter already exists, do not add it again
                    !action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                }
                .filter { name ->
                    /*
                        This one is very tricky. Some JEE-based frameworks could conflate URL query parameters and
                         parameters in body payload of form submissions in x-www-form-urlencoded format.
                         This happens for example in LanguageTool.
                     */
                    !action.parameters.any { b ->
                        b is BodyParam && b.isForm()
                                && b.seeGenes().flatMap { it.flatView() }
                            .any { it.name.equals(name, ignoreCase = true) }
                    }
                }
                .filter { name ->
                    /*
                        Another tricky case. Some frameworks like Spring can have hidden params to override the method
                        type of the requests. This is needed for handling web browsers without JS.
                        See https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/HiddenHttpMethodFilter.html
                        This lead to meaningless tests with 405 responses.
                        So, we skip them.
                     */
                    name != "_method"
                }
                .forEach {
                    val gene = StringGene(it).apply { doInitialize(randomness) }
                    action.addParam(
                        QueryParam(it,
                            OptionalGene(it, gene, false, requestSelection = true).apply { doInitialize() })
                    )
                }

            if (result.getStatusCode() == 415) {
                /*
                    In theory, this should not happen.
                    415 means the media type of the sent payload is wrong.
                    There is no point for EvoMaster to do that, ie sending an XML to
                    an endpoint that expects a JSON.
                    Having such kind of test would be pretty pointless.

                    However, a POST/PUT could expect a payload and, if that is not specified
                    in OpenAPI, we could get a 415 when sending no data.
                 */
                if (action.parameters.none { it is BodyParam }) {

                    val obj = ObjectGene("body", listOf())
                    // TODO could look at "Accept" header instead of defaulting to JSON
                    val enumGene = EnumGene("contentType", listOf("application/json"))
                    val body = BodyParam(obj,enumGene)
                    body.seeGenes().forEach { it.doInitialize(randomness) }

                    val update = UpdateForBodyParam(body)

                    action.addParam(update)
                }
            }


            val dtoNames = info.parsedDtoNames

            val noBody = action.parameters.none { it is BodyParam }
            val emptyObject = !noBody &&
                    // this is the case of 415 handling
                    action.parameters.find { it is BodyParam }!!.let {
                        it.gene is ObjectGene && it.gene.fields.isEmpty()
                    }

            if (info.rawAccessOfHttpBodyPayload == true
                && dtoNames.isNotEmpty()
                && (noBody || emptyObject)
            ) {
                /*
                    The SUT tried to read the HTTP body payload, but there is no info
                    about it in the schema. This can happen when payloads are dynamically
                    loaded directly in the business logic of the SUT, and automated tools
                    like SpringDoc/SpringFox failed to infer what is read

                    TODO could handle other types besides JSON
                    TODO what to do if more than 1 DTO are registered?
                         Likely need a new MultiOptionGene similar to DisjunctionListRxGene
                 */
                if (dtoNames.size > 1) {
                    LoggingUtil.uniqueWarn(log, "More than 1 DTO option: [${dtoNames.sorted().joinToString(", ")}]")
                }
                val name = dtoNames.first()
                val obj = getGeneForDto(name)
                val enumGene = EnumGene("contentType", listOf("application/json"))
                val body = BodyParam(obj,enumGene)
                body.seeGenes().forEach { it.doInitialize(randomness) }
                val update = UpdateForBodyParam(body)
                action.addParam(update)
            }
        }
    }

    private fun getGeneForDto(name: String): Gene {

        if (!infoDto.unitsInfoDto.parsedDtos.containsKey(name)) {
            /*
                parsedDto info is update throughout the search.
                so, if info is missing, we re-fetch the whole data.
                Would be more efficient to just fetch new data, but,
                as this will happen seldom (at most N times for N dtos),
                no much point in optimizing it
             */
            infoDto = rc.getSutInfo()!!

            if (!infoDto.unitsInfoDto.parsedDtos.containsKey(name)) {
                throw RuntimeException("BUG: info for DTO $name is not available in the SUT driver")
            }
        }

        return ParserDtoUtil.getOrParseDtoWithSutInfo(infoDto, config.enableSchemaConstraintHandling)[name]!!
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    fun handleResponseTargets(
        fv: FitnessValue,
        actions: List<RestCallAction>,
        actionResults: List<ActionResult>,
        additionalInfoList: List<AdditionalInfoDto>
    ) {

        actionResults.indices
            .filter { actionResults[it] is RestCallResult }
            .forEach {
                val result = actionResults[it] as RestCallResult
                val status = result.getStatusCode() ?: -1
                val name = actions[it].getName()

                //objective for HTTP specific status code
                val statusId = idMapper.handleLocalTarget("$status:$name")
                fv.updateTarget(statusId, 1.0, it)

                handleAdvancedBlackBoxCriteria(fv, actions[it], result)

                val location5xx: String? = getlocation5xx(status, additionalInfoList, it, result, name)
                handleAdditionalStatusTargetDescription(result, fv, status, name, it, location5xx)
                handleAuthTargets(status, actions, it, name, fv)
            }
    }

    private fun handleAuthTargets(
        status: Int,
        actions: List<RestCallAction>,
        actionIndex: Int,
        name: String,
        fv: FitnessValue
    ) {
        val action = actions[actionIndex]

        val unauthorized = !AuthUtils.checkUnauthorizedWithAuth(status, action)
        if (unauthorized) {
            /*
                        Note: at this point we cannot consider it as a bug, because it could be just a
                        misconfigured auth info.
                        however, if for other endpoints or parameters we get a 2xx, then it is clearly
                        a bug (although we need to make 100% sure of handling token caching accordingly).
                        but this would be check in specific security tests after the end of the search.
                     */
            val unauthorizedId = idMapper.handleLocalTarget("wrong_authorization:$name")
            fv.updateTarget(unauthorizedId, 1.0, actionIndex)
        }

        if(config.security && (sampler as AbstractRestSampler).authentications.isNotEmpty()){

            val label = when{
                StatusGroup.G_2xx.isInGroup(status) -> "2xx"
                status == 401  && !action.auth.requireMockHandling-> "401"
                status == 403 -> "403"
                else -> return
            }

            val targetId = idMapper.handleLocalTarget("Auth:${action.auth.name}:$name:$label")
            fv.updateTarget(targetId, 1.0, actionIndex)
        }
    }

    private fun handleAdvancedBlackBoxCriteria(fv: FitnessValue, call: RestCallAction, result: RestCallResult) {

        if(!config.advancedBlackBoxCoverage){
            return
        }
        val status = result.getStatusCode()
        val success = StatusGroup.G_2xx.isInGroup(status)

        //Links
        if(result.getAppliedLink()){
            //create objectives to keep track of followed links
            val root = "LINK_FOLLOWED"
            fv.coverTarget(idMapper.handleLocalTarget("${root}_${call.id}"))
            if(success) {
                fv.coverTarget(idMapper.handleLocalTarget("${root}_SUCCESS_${call.id}"))
            }
        }

        //Presence of query params
        call.parameters.filterIsInstance<QueryParam>().forEach {
            val gene = it.getGeneForQuery()
            val present = gene !is OptionalGene || gene.isActive
            val root = "QUERY_PARAM"
            val id = "${present}_${it.name}_${call.id}"

            //up to 4 targets
            fv.coverTarget(idMapper.handleLocalTarget("${root}_${id}"))
            if(success){
                fv.coverTarget(idMapper.handleLocalTarget("${root}_SUCCESS_${id}"))
            }
        }

        //Values in queries/paths
        //These will include examples/defaults
        call.parameters
            .forEach { p ->
                /*
                    This choice is arguable... otherwise might lead to big test suites with no benefit,
                    eg if just data saved on database with no impact on control flow...
                    TODO could be something to empirical investigate
                 */

                val root = "INPUT_${call.id}_${p.javaClass.simpleName}_${p.name}"

                val genes = if(p is BodyParam) {
                    listOf(p.contentTypeGene) // ie, ignore the payload
                } else {
                    p.seeGenes()
                }
                genes.flatMap { g -> g.flatView() }
                    .filter { g -> g.staticCheckIfImpactPhenotype() }
                    .forEach { g ->
                        if(g is EnumGene<*> || g is BooleanGene || g is NumberGene<*>) {
                            val prefix = "${root}_${g.name}"
                            val suffix = when (g) {
                                is EnumGene<*>, is BooleanGene -> g.getValueAsRawString()
                                is NumberGene<*> -> {
                                    val negative = g.getValueAsRawString().startsWith("-")
                                    if(negative) "negative" else "positive"
                                }
                                else -> throw IllegalStateException("Not handled type: ${g.javaClass}")
                            }
                            fv.coverTarget(idMapper.handleLocalTarget("${prefix}_${suffix}"))
                            if(success){
                                fv.coverTarget(idMapper.handleLocalTarget("${prefix}_SUCCESS_${suffix}"))
                            }
                        }
                    }
            }

        //body payload type in response
        fv.coverTarget(idMapper.handleLocalTarget("RESPONSE_BODY_PAYLOAD_${call.id}_${result.getBodyType()}"))

        /*
            explicit targets for examples
         */
        val examples = call.seeTopGenes()
            .flatMap { it.flatView() }
            .filter { it.staticCheckIfImpactPhenotype() }
            .filter { it.name == RestActionBuilderV3.EXAMPLES_NAME }

        examples.forEach {
            val name = (it.parent as Gene).name
            val label = when(it){
                is EnumGene<*> -> it.getValueAsRawString()
                is ChoiceGene<*> -> ""+it.activeGeneIndex
                else ->{
                    log.warn("Unhandled example gene type: ${it.javaClass}")
                    assert(false)
                    "undefined"
                }
            }

            val target = "EXAMPLE_${call.id}_${name}_$label"
            fv.coverTarget(idMapper.handleLocalTarget(target))
        }
    }

    private fun handleAdditionalStatusTargetDescription(
        result: RestCallResult,
        fv: FitnessValue,
        status: Int,
        name: String,
        indexOfAction: Int,
        location5xx: String?
    ) {
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

        if (status == 500) {
            /*
                500 codes "might" be bugs. To distinguish between different bugs
                that crash the same endpoint, we need to know what was the last
                executed statement in the SUT.
                So, we create new targets for it.

                However, such info is missing in black-box testing
            */
            Lazy.assert { location5xx != null || config.blackBox }

            val postfix = if (location5xx == null) name else "${location5xx!!} $name"
            val descriptiveId = idMapper.getFaultDescriptiveId(DefinedFaultCategory.HTTP_STATUS_500,postfix)
            val bugId = idMapper.handleLocalTarget(descriptiveId)
            fv.updateTarget(bugId, 1.0, indexOfAction)

            result.addFault(DetectedFault(DefinedFaultCategory.HTTP_STATUS_500, name,location5xx))
        }
    }


    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    protected fun handleRestCall(
        a: RestCallAction,
        all: List<RestCallAction>,
        actionResults: MutableList<ActionResult>,
        chainState: MutableMap<String, String>,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>,
        fv: FitnessValue
    ): Boolean {

        searchTimeController.waitForRateLimiter()

        val rcr = RestCallResult(a.getLocalId())
        actionResults.add(rcr)

        val appliedLink = handleLinks(a, all,actionResults)

        val response = try {
            createInvocation(a, chainState, cookies, tokens).invoke()
        } catch (e: ProcessingException) {

            log.debug("There has been an issue in the evaluation of a test: ${e.message}", e)

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
                    client = HttpClientFactory.createTrustingJerseyClient(false, config.tcpTimeoutMs)

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

                config.blackBox && TcpUtils.isRefusedConnection(e) -> {
                    /*
                        This might happen if we have wrong info of API location, eg host/servers in
                        the schema are wrong or static with hardcoded TCP ports
                     */
                    throw SutProblemException(
                        "Failed to connect API with TCP." +
                                " Is the API up and running at '${getBaseUrl()}' ?" +
                                " If not, the location can be overridden with --bbTargetUrl"
                    )
                }

                TcpUtils.isUnknownHost(e) -> {
                    throw SutProblemException(
                        "Unknown host: ${URL(getBaseUrl()).host}\n" +
                                " Are you sure you did not misspell it?"
                    )
                }

                TcpUtils.isInternalError(e) -> {
                    throw RuntimeException(
                        "Internal bug with EvoMaster when making a HTTP call toward ${a.resolvedPath()}",
                        e
                    )
                }

                else -> throw e
            }
        }

        rcr.setStatusCode(response.status)
        rcr.setLocation(response.location?.toString())
        rcr.setAppliedLink(appliedLink)

        handlePossibleConnectionClose(response)

        try {
            if (response.hasEntity()) {
                if (response.mediaType != null) {
                    rcr.setBodyType(response.mediaType)
                }
                /*
                    FIXME should read as byte[]
                 */
                try {
                    val body = response.readEntity(String::class.java)

                    if (body.length < configuration.maxResponseByteSize) {
                        rcr.setBody(body)
                    } else {
                        LoggingUtil.uniqueWarn(
                            log,
                            "A very large response body was retrieved from the endpoint '${a.path}'." +
                                    " If that was expected, increase the 'maxResponseByteSize' threshold" +
                                    " in the configurations."
                        )
                        rcr.setTooLargeBody(true)
                    }
                } catch (e: OutOfMemoryError) {
                    /*
                        internal classes in JVM can throw this error directly, like
                        jdk.internal.util.ArraysSupport.hugeLength(...)
                        see:
                        https://github.com/WebFuzzing/EvoMaster/issues/449
                     */
                    LoggingUtil.uniqueWarn(
                        log,
                        "An extremely large response body was retrieved from the endpoint '${a.path}'." +
                                " So large that it cannot be handled inside the JVM in which EvoMaster is running."
                    )
                    rcr.setTooLargeBody(true)
                }
            }
        } catch (e: Exception) {

            if (e is ProcessingException && TcpUtils.isTimeout(e)) {
                rcr.setTimedout(true)
                statistics.reportTimeout()
                return false
            } else {
                log.warn("Failed to parse HTTP response: ${e.message}")
            }
        }

        handleSchemaOracles(a, rcr, fv)

        val handledSavedLocation = handleSaveLocation(a, rcr, chainState)

        if(config.isEnabledAIModelForResponseClassification()) {
            responseClassifier.updateModel(a, rcr)
        }

        if (config.security && config.ssrf) {
            if (ssrfAnalyser.anyCallsMadeToHTTPVerifier(a)) {
                rcr.setVulnerableForSSRF(true)
            }
        }

        return handledSavedLocation
    }

    private fun handleSchemaOracles(
        a: RestCallAction,
        rcr: RestCallResult,
        fv: FitnessValue
    ) {
        if (!config.schemaOracles || !schemaOracle.canValidate() || a.id == CALL_TO_SWAGGER_ID) {
            return
        }

        val report = try{
            schemaOracle.handleSchemaOracles(a.resolvedOnlyPath(), a.verb, rcr)
        }catch (e:Exception){
            log.warn("Failed to handle response validation for ${a.getName()}." +
                    " This might be a bug in EvoMaster, or in one of the third-party libraries it uses." +
                    " Error: ${e.message}")
            return
        }

        val onePerType = report.messages
            .groupBy { it.key }
            .filter {
                /*
                  FIXME this is due to bug in library.
                  see disabled test in [RestSchemaOraclesTest]
                 */
                it.key != "validation.response.body.schema.additionalProperties"
            }
            .map { (key, value) -> value.first() }

        onePerType.forEach {

            val discriminant = a.getName() + " -> " + it.key
            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE, discriminant)
            )
            fv.updateTarget(scenarioId, 1.0, a.positionAmongMainActions())
            val fault = DetectedFault(
                DefinedFaultCategory.SCHEMA_INVALID_RESPONSE,
                a.getName(),
                "Type: ${it.key}",
                it.message?.replace("\n", " ")
            )
            rcr.addFault(fault)
        }

    }


    private fun handleLinks(
        a: RestCallAction,
        all: List<RestCallAction>,
        actionResults: List<ActionResult>
    ) : Boolean {
        val index = all.indexOfFirst { it.getLocalId() == a.getLocalId() }
        if(index < 0){
            throw IllegalStateException("Bug: input REST call action is not present in 'all' list")
        }

        val reference = a.backwardLinkReference
            //nothing to do
            ?: return false

        /*
            Recall we cannot use localId to specify the backward link (as those are not defined yet when links are
            created).
            The "id" just represents the type, and there could be several of them in a test.
            So, here we look at previous actions (ie all before "index"), and take the closest to index
         */
        val previous = all.take(index)
            .find { action ->
                reference.sourceActionId == action.id
                // not only must be of right kind, but also return right status code
                &&
                (actionResults.find { it.sourceLocalId == action.getLocalId() } as RestCallResult?)
                    ?.getStatusCode() == reference.statusCode
            }
            //could happen if mutation (unless we force updating broken links), but never on sampling
            //TODO should handle this
            ?: return false
        val link = previous.links.find { it.id == reference.sourceLinkId }
            ?: throw IllegalStateException("Bug: endpoint ${previous.id} has no link of type ${reference.sourceLinkId}")

        val result = actionResults.find { it.sourceLocalId == previous.getLocalId() } as RestCallResult?
            // in theory, this branch is unreachable, as otherwise previous would had been null
            ?: throw IllegalArgumentException("No action result for ${previous.getLocalId()}")

        val modified = RestLinkValueUpdater.update(a,link,previous,result)
        if(modified){
            reference.actualSourceActionLocalId = previous.getLocalId()
        } else{
            reference.actualSourceActionLocalId = null
        }
        return modified
    }


    private fun createInvocation(
        a: RestCallAction,
        chainState: MutableMap<String, String>,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>
    ): Invocation {

        val baseUrl = getBaseUrl()

        val path = a.resolvedPath()

        val locationHeader = if (a.usePreviousLocationId != null) {
            chainState[locationName(a.usePreviousLocationId!!)]
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


        val builder = if (a.produces.isEmpty()) {
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

        handleHeaders(a, builder, cookies, tokens)

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

            val stringToBeSent = body.getRawStringToBeSent(mode = mode, targetFormat = configuration.outputFormat)
            Entity.entity(
                stringToBeSent,
                body.contentType()
            )
        } else if (forms != null) {
            Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if (a.verb == HttpVerb.PUT || a.verb == HttpVerb.PATCH) {
            /*
                PUT and PATCH must have a payload. But it might happen that it is missing in the Swagger schema
                when objects like WebRequest are used. So we default to urlencoded
             */
            Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if (a.verb == HttpVerb.POST && body == null) {
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
//            HttpVerb.DELETE -> builder.buildDelete()
            /*
                As of RFC 9110 it is allowed to have bodies for GET and DELETE, albeit in special cases.
                https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.1-6

                Note: due to bug in Jersey, can handle DELETE but not GET :(
                TODO: update RestActionBuilderV3 once upgraded Jersey, after JDK 11 move
             */
//            HttpVerb.GET -> builder.build("GET", bodyEntity)
            HttpVerb.DELETE -> builder.build("DELETE", bodyEntity)
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.HEAD -> builder.build("HEAD")
            HttpVerb.TRACE -> builder.build("TRACE")
        }
        return invocation
    }


    protected fun handleSaveLocation(
        a: RestCallAction,
        rcr: RestCallResult,
        chainState: MutableMap<String, String>
    ): Boolean {
        if (a.saveCreatedResourceLocation) {

            val status = rcr.getStatusCode()

            if (!StatusGroup.G_2xx.isInGroup(status)) {
                /*
                    If this failed, and following actions require the "location" header
                    of this call, there is no point whatsoever to continue evaluating
                    the remaining calls
                 */
                rcr.stopping = true
                return false
            }

            val name = locationName(a.creationLocationId())
            var location = rcr.getLocation()

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

                if (id != null) {
                    location = callGraphService.resolveLocationForChildOperationUsingCreatedResource(a,id.value)
                    if(location != null) {
                        rcr.setHeuristicsForChainedLocation(true)
                    }
                }
            }

            //save location for the following REST calls
            chainState[name] = location ?: ""
        }
        return true
    }




    private fun locationName(id: String): String {
        return "location_$id"
    }

    /**
     * WARNING: possible side-effects on input parameters, eg actionResults
     */
    protected fun restActionResultHandling(
        individual: RestIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ): TestResultsDto? {

        if (actionResults.any { it is RestCallResult && it.getTcpProblem() }) {
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

        val dto = updateFitnessAfterEvaluation(targets, allTargets, fullyCovered, descriptiveIds, individual, fv)
            ?: return null

        handleExtra(dto, fv)

        handleFurtherFitnessFunctions(fv)

        val wmStarted = handleExternalServiceInfo(individual, fv, dto.additionalInfoList)
        if(wmStarted){
            /*
                Quite tricky... if a WM instance was started as part of this test case evaluation,
                then the results are invalid, as they are based on WM not being there.

                For the first time when an external web service call detected core will
                  start initiating WM for it. Next time during the search subsequent tests will pass.
                  Although in [HostnameResolutionActionEMTest] we end up having test cases which captures
                  the first-time cases. To avoid this we're skipping those test where external web service
                  call detected but there is no active WM running.
                  We can issue [deathSentence] to the individual at this point.

                FIXME: actually not necessary... better to default on IP address in which nothing is running,
                ie, 127.0.0.2, so we can cover the case of HostnameResolutionActionEMTest.
                no need to give a death sentence here


             */
            //cannot return null here, as otherwise SUT gets restarted
             //return null
            actionResults.forEach { it.deathSentence = true }
        }

        if (epc.isInSearch()) {
            if (config.expandRestIndividuals) {
                expandIndividual(individual, dto.additionalInfoList, actionResults)
            }

            if (config.isEnabledTaintAnalysis()) {
                Lazy.assert { actionResults.size == dto.additionalInfoList.size }
                //TODO add taint analysis for resource-based solution
                TaintAnalysis.doTaintAnalysis(
                    individual,
                    dto.additionalInfoList,
                    randomness,
                    config
                )
            }
        }

        analyzeResponseData(fv,individual,actionResults,dto.additionalInfoList)

        return dto
    }


    protected fun analyzeResponseData(
        fv: FitnessValue,
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        additionalInfoList: List<AdditionalInfoDto>
    ) {
        handleResponseTargets(
            fv,
            individual.seeAllActions().filterIsInstance<RestCallAction>(),
            actionResults,
            additionalInfoList
        )

        if (config.useResponseDataPool) {
            recordResponseData(individual, actionResults.filterIsInstance<RestCallResult>())
        }

        //TODO likely would need to consider SEEDED as well in future
        if(config.security && individual.sampleType == SampleType.SECURITY){
            analyzeSecurityProperties(individual,actionResults,fv)
        }

        if (config.ssrf) {
            handleSsrfFaults(individual, actionResults, fv)
        }

        //TODO likely would need to consider SEEDED as well in future
        if(config.httpOracles && individual.sampleType == SampleType.HTTP_SEMANTICS){
            analyzeHttpSemantics(individual, actionResults, fv)
        }

    }

    private fun analyzeHttpSemantics(individual: RestIndividual, actionResults: List<ActionResult>, fv: FitnessValue) {

        handleDeleteShouldDelete(individual, actionResults, fv)
        handleRepeatedCreatePut(individual, actionResults, fv)
    }

    private fun handleRepeatedCreatePut(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {

        val issues = HttpSemanticsOracle.hasRepeatedCreatePut(individual,actionResults)
        if(!issues){
            return
        }

        val put = individual.seeMainExecutableActions().last()

        val category = ExperimentalFaultCategory.HTTP_REPEATED_CREATE_PUT
        val scenarioId = idMapper.handleLocalTarget(idMapper.getFaultDescriptiveId(category, put.getName())
        )
        fv.updateTarget(scenarioId, 1.0, individual.seeMainExecutableActions().lastIndex)

        val ar = actionResults.find { it.sourceLocalId == put.getLocalId() } as RestCallResult?
            ?: return
        ar.addFault(DetectedFault(category, put.getName(), null))
    }

    private fun handleDeleteShouldDelete(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        val res = HttpSemanticsOracle.hasNonWorkingDelete(individual, actionResults)

        if(res.checkingDelete){
            //even if no fault found, it is useful to have such test for readability and for validation
            val scenarioId = idMapper.handleLocalTarget("checkdelete:${res.name}")
            fv.updateTarget(scenarioId, 1.0, res.index)
        }

        if(res.nonWorking) {
            val category = ExperimentalFaultCategory.HTTP_NONWORKING_DELETE
            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(category, res.name)
            )
            fv.updateTarget(scenarioId, 1.0, res.index)

            val delete = individual.seeMainExecutableActions()[res.index]
            val ar = actionResults.find { it.sourceLocalId == delete.getLocalId() } as RestCallResult?
                ?: return
            ar.addFault(DetectedFault(category, res.name, null))
        }
    }

    private fun analyzeSecurityProperties(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ){
        //TODO the other cases

        handleForbiddenOperation(HttpVerb.DELETE, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleForbiddenOperation(HttpVerb.PUT, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleForbiddenOperation(HttpVerb.PATCH, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleExistenceLeakage(individual,actionResults,fv)
        handleNotRecognizedAuthenticated(individual, actionResults, fv)
        handleForgottenAuthentication(individual, actionResults, fv)
    }

    private fun handleSsrfFaults(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        individual.seeMainExecutableActions().forEach {
            val ar = (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult?)
            if (ar != null) {
                if (ar.getResultValue(HttpWsCallResult.VULNERABLE_SSRF).toBoolean()) {
                    val scenarioId = idMapper.handleLocalTarget(
                        idMapper.getFaultDescriptiveId(DefinedFaultCategory.SSRF, it.getName())
                    )
                    fv.updateTarget(scenarioId, 1.0, it.positionAmongMainActions())

                    val paramName = ssrfAnalyser.getVulnerableParameterName(it)
                    ar.addFault(DetectedFault(DefinedFaultCategory.SSRF, it.getName(), paramName))
                }
            }
        }
    }

    private fun handleNotRecognizedAuthenticated(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {

        val notRecognized = individual.seeMainExecutableActions()
            .filter {
                val ar = (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult?)
                if(ar == null){
                    // this can be happened in the POST/DELETE template
                    val prematureStoppedAction = individual.seeMainExecutableActions().filter { it.auth !is NoAuth
                            && (actionResults.find { r -> r.sourceLocalId != it.getLocalId() } as RestCallResult?)?.stopping == true
                    }
                    if (prematureStoppedAction.isNotEmpty()){
                        log.debug("Premature stopping of HTTP call sequence")
                        return
                    }
                    throw IllegalArgumentException("Missing action result with id: ${actionResults.map { it.sourceLocalId }}")
                }
                it.auth !is NoAuth && ar.getStatusCode() == 401
            }.filter { RestSecurityOracle.hasNotRecognizedAuthenticated(it, individual, actionResults) }

        if(notRecognized.isEmpty()){
            return
        }

        notRecognized.forEach {
            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, it.getName())
            )
            fv.updateTarget(scenarioId, 1.0, it.positionAmongMainActions())
            val r = actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult
            r.addFault(DetectedFault(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, it.getName(), null))
        }
    }

    private fun handleExistenceLeakage(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        val getPaths = individual.seeMainExecutableActions()
            .filter { it.verb == HttpVerb.GET }
            .map { it.path }
            .toSet()

        val faultyPaths = getPaths.filter { RestSecurityOracle.hasExistenceLeakage(it, individual, actionResults)  }
        if(faultyPaths.isEmpty()){
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult

            if(a.verb == HttpVerb.GET && faultyPaths.contains(a.path) && r.getStatusCode() == 404){
                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE, a.getName(), null))
            }
        }
    }

    private fun handleForgottenAuthentication(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        val endpoints = individual.seeMainExecutableActions()
            .map { it.getName() }
            .toSet()

        val faultyEndpoints = endpoints.filter { RestSecurityOracle.hasForgottenAuthentication(it, individual, actionResults)  }

        if(faultyEndpoints.isEmpty()){
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult

            if(a.auth is NoAuth && faultyEndpoints.contains(a.getName()) &&  StatusGroup.G_2xx.isInGroup(r.getStatusCode())){
                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.SECURITY_FORGOTTEN_AUTHENTICATION, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(ExperimentalFaultCategory.SECURITY_FORGOTTEN_AUTHENTICATION, a.getName(), null))
            }
        }
    }

    private fun handleForbiddenOperation(
        verb: HttpVerb,
        faultCategory: FaultCategory,
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (RestSecurityOracle.hasForbiddenOperation(verb, individual, actionResults)) {
           val actionIndex = individual.size() - 1
            val action = individual.seeMainExecutableActions()[actionIndex]
            val result = actionResults
                .filterIsInstance<RestCallResult>()
                .find { it.sourceLocalId == action.getLocalId() }
                ?: return

            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(faultCategory, action.getName())
            )
            fv.updateTarget(scenarioId, 1.0, actionIndex)
            result.addFault(DetectedFault(faultCategory, action.getName(), null))
        }
    }


    protected fun recordResponseData(individual: RestIndividual, actionResults: List<RestCallResult>) {

        for (res in actionResults) {
            val source = individual.seeAllActions().find { it.getLocalId() == res.sourceLocalId } as RestCallAction?
            if (source == null) {
                log.warn("Failed to analyze response. Cannot match response to source action")
                assert(false)//only break in tests
                continue
            }
            RestResponseFeeder.handleResponse(source, res, responsePool)
        }
    }



    /**
     * Based on info coming from SUT execution, register and start new WireMock instances.
     *
     * TODO push this thing up to hierarchy to EntepriseFitness
     *
     * @return whether there was side effect of starting new instance of WireMock
     */
    private fun handleExternalServiceInfo(
        individual: RestIndividual,
        fv: FitnessValue,
        infoDto: List<AdditionalInfoDto>
    ) : Boolean {

        /*
            Note: this info here is based from what connections / hostname resolving done in the SUT,
            via instrumentation.

            However, what is actually called on an already up and running WM instance from a previous call is
            done on WM directly, and it must be done at SUT call (as WM get reset there)
         */

        var wmStarted = false

        infoDto.forEachIndexed { index, info ->
            info.hostnameResolutionInfoDtos.forEach { hn ->

                val dns = HostnameResolutionInfo(
                    hn.remoteHostname,
                    hn.resolvedAddress
                )
                externalServiceHandler.addHostname(dns)

                if (dns.isResolved()) {
                    /*
                        We need to ask, are we in that special case in which a hostname was resolved but there is
                        no action for it?
                        that would represent a real website resolution, which we cannot allow in the generated tests.
                        for this reason, in instrumentation, we redirect toward a RESERVED IP address.
                        to guarantee such behavior in generated tests, where there is instrumentation, we need to modify
                        the genotype of this evaluated individual (without modifying its phenotype)
                     */
                    val actions = individual.seeActions(ActionFilter.ONLY_DNS) as List<HostnameResolutionAction>
                    if ((actions.isEmpty() || actions.none { it.hostname == hn.remoteHostname }) &&
                        // To avoid adding action for the local WM address in case of InetAddress used
                        // in the case study.
                        !externalServiceHandler.isWireMockAddress(hn.remoteHostname)
                    ) {
                        // OK, we are in that special case
                        val hra = HostnameResolutionAction(
                            hn.remoteHostname,
                            ExternalServiceSharedUtils.RESERVED_RESOLVED_LOCAL_IP
                        )
                        individual.addChildToGroup(hra, GroupsOfChildren.INITIALIZATION_DNS)
                        // TODO: Above line adds unnecessary tests at the end, which is
                        //  causing the created tests to fail.
                        //  Now handling in Mutator, removing existing actions with default IP address for the same hostname.

                    }
                }
            }

            info.externalServices.forEach { es ->

                /*
                    The info here is coming from SUT instrumentation
                 */

                val started = externalServiceHandler.addExternalService(
                    HttpExternalServiceInfo(
                        es.protocol,
                        es.remoteHostname,
                        es.remotePort
                    )
                )
                wmStarted = wmStarted || started
            }


            // register the external service info which re-direct to the default WM
//            fv.registerExternalRequestToDefaultWM(
//                index,
//                info.employedDefaultWM.associate { it ->
//                    val signature = getWMDefaultSignature(it.protocol, it.remotePort)
//                    it.remoteHostname to externalServiceHandler.getExternalService(signature)
//                }
//            )
        }

        return wmStarted
    }

    override fun getActionDto(action: Action, index: Int): ActionDto {
        val actionDto = super.getActionDto(action, index)
        // TODO: Need to move under ApiWsFitness after the GraphQL and RPC support is completed
        if (index == 0) {
            val individual = action.getRoot() as RestIndividual
            actionDto.localAddressMapping = individual
                .seeActions(ActionFilter.ONLY_DNS)
                .filterIsInstance<HostnameResolutionAction>()
                .associate { it.hostname to it.localIPAddress }
            actionDto.externalServiceMapping = externalServiceHandler.getExternalServiceMappings()
            actionDto.skippedExternalServices = externalServiceHandler.getSkippedExternalServices()
        }
        return actionDto
    }
}

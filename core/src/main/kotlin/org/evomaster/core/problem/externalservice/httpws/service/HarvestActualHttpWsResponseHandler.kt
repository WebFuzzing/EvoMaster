package org.evomaster.core.problem.externalservice.httpws.service

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Inject
import org.apache.commons.text.similarity.LevenshteinDistance
import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.ActualResponseInfo
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest.Companion.getTextualMethodURLFromRequestDescription
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest.Companion.getTextualURLFromRequestDescription
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.problem.externalservice.param.ResponseParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.ParserDtoUtil
import org.evomaster.core.problem.util.ParserDtoUtil.getJsonNodeFromText
import org.evomaster.core.problem.util.ParserDtoUtil.parseJsonNodeAsGene
import org.evomaster.core.problem.util.ParserDtoUtil.setGeneBasedOnString
import org.evomaster.core.problem.util.ParserDtoUtil.wrapWithOptionalGene
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.math.max
import kotlin.math.min


/**
 * based on collected requests to external services from WireMock
 * harvest actual responses by making the requests to real external services
 *
 * harvested actual responses could be applied as seeded in optimizing
 * test generation with search
 */
class HarvestActualHttpWsResponseHandler {

    // note rc should never be used in the thread of sending requests to external services
    @Inject(optional = true)
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var randomness: Randomness

    /**
     * clients used to harvest requests for multiple threads
     * key is the id of thread
     * value is instantiated client
     */
    private val clients = ConcurrentHashMap<Long, Client>()

    /**
     * TODO if one day we need priorities on the queue, it can be set here. See:
     * https://stackoverflow.com/questions/3198660/java-executors-how-can-i-set-task-priority
     */
    private lateinit var workerPool: ExecutorService

    /**
     * If an hostname is unknown and cannot be resolved, no point in trying yet again to connect to it
     */
    private val unknownHosts : MutableSet<String> = CopyOnWriteArraySet()


    companion object {
        private val log: Logger = LoggerFactory.getLogger(HarvestActualHttpWsResponseHandler::class.java)
        private const val ACTUAL_RESPONSE_GENE_NAME = "ActualResponse"

        init {
            /**
             * this default setting in jersey-client is false
             * to allow collected requests which might have restricted headers, then
             * set the property
             */
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
        }
    }

    /**
     * save the harvested actual responses
     *
     * key is actual request based on [HttpExternalServiceRequest.getDescription]
     *      ie, "method:absoluteURL[headers]{body payload}",
     * value is an actual response info
     */
    private val actualResponses = ConcurrentHashMap<String, ActualResponseInfo>()

    /**
     * track a list of actual responses which have been seeded in the search based on
     * its corresponding request using its description, ie, ie, "method:absoluteURL[headers]{body payload}",
     */
    private val seededResponses = mutableSetOf<String>()

    /**
     * track a list of actual responses in success family (eg, 2xx for HTTP external services) which
     * have been seeded in the search
     */
    private val seededSuccessfulResponses = mutableSetOf<String>()

    /**
     * key is dto class name
     * value is parsed gene based on schema
     */
    private val extractedObjectDto = mutableMapOf<String, Gene>()

    /*
        skip headers if they depend on the client
     */
    private val skipHeaders = listOf("user-agent", "host", "accept-encoding", "connection")


    /**
     * Contains the set of references to initiated external service requests
     */
    private val startedRequests: MutableSet<String> = mutableSetOf()


    private var actualFixedThreadPool = 0

    @PostConstruct
    fun initialize() {
        if (config.isEnabledHarvestingActualResponse()) {

            actualFixedThreadPool = min(
                    config.externalRequestHarvesterNumberOfThreads,
                    Runtime.getRuntime().availableProcessors()
            )
            workerPool = Executors.newFixedThreadPool(
                actualFixedThreadPool
            )
        }
    }

    @PreDestroy
    private fun preDestroy() {
        if (config.isEnabledHarvestingActualResponse()) {
            shutdown()
        }
    }

    fun shutdown() {
        Lazy.assert { config.isEnabledHarvestingActualResponse() }
        workerPool.shutdown()
        clients.values.forEach{it.close()}
    }

    /**
     * @return the number of created clients for harvesting
     *
     * this is mainly for debugging
     */
    fun getNumOfClients() = clients.size

    /**
     * @return the number of harvested responses
     *
     * this is mainly for debugging
     */
    fun getNumOfHarvestedResponse() = actualResponses.size

    /**
     * @return the number of thread configured in ExecutionService
     *
     * this is mainly for debugging
     */
    fun getConfiguredFixedThreadPool() = actualFixedThreadPool

    /**
     * @return the number of started requests for harvesting responses
     *
     * this is mainly for debugging
     */
    fun getNumOfStartedRequests() = startedRequests.size

    private fun sendRequestToRealExternalService(clientId: Long, httpWsClient: Client, request: HttpExternalServiceRequest) {
        val info = handleActualResponse(
            createInvocationToRealExternalService(clientId, httpWsClient, request)
        )
        if (info != null) {
            info.param.responseBody.markAllAsInitialized()
            actualResponses[request.getDescription()] = info
        } else
            LoggingUtil.uniqueWarn(log, "Fail to harvest actual responses for ${request.method} ${request.absoluteURL} from actual URL (${request.actualAbsoluteURL})")
    }

    /**
     * @return a copy of gene of actual responses based on the given [gene] and probability
     *
     * note that this method is used in mutation phase to mutate the given [gene] based on actual response if it exists
     * and based on the given [probability]
     *
     * the given [gene] should be the response body gene of ResponseParam
     */
    private fun getACopyOfItsActualResponseIfExist(gene: Gene, probability: Double): ResponseParam? {
        if (probability == 0.0) return null
        val exAction = gene.getFirstParent { it is ApiExternalServiceAction } ?: return null

        // only support HttpExternalServiceAction, TODO for others
        if (exAction is HttpExternalServiceAction) {
            Lazy.assert { gene.parent == exAction.response }
            if (exAction.response.responseBody == gene) {
                val p = if (doPrioritizeHarvesting(exAction.request.getDescription())) {
                    max(config.probOfHarvestingResponsesFromActualExternalServices, probability)
                } else {
                    probability
                }
                if (randomness.nextBoolean(p))
                    return getACopyOfActualResponse(exAction.request)
            }

        }
        return null
    }

    private fun doPrioritizeHarvesting(key: String) : Boolean{
        // if the actual response is never seeded, might prefer harvesting
        val never = !seededResponses.contains(key)
        if (never) return true
        return !seededSuccessfulResponses.contains(key) && randomness.nextBoolean(config.probOfPrioritizingSuccessfulHarvestedActualResponses)
    }

    /**
     * @return a copy of actual responses based on the given [httpRequest] and probability
     */
    fun getACopyOfActualResponse(httpRequest: HttpExternalServiceRequest, probability: Double? = null): ResponseParam? {
        val harvest = probability == null || (randomness.nextBoolean(probability))
        if (!harvest || actualResponses.isEmpty()){
            return null
        }
        var found: ResponseParam? = null
        synchronized(actualResponses) {

            //first check for an exact match
            found = (actualResponses[httpRequest.getDescription()]?.param?.copy() as? ResponseParam)
            val preferSuccess = randomness.nextBoolean(config.probOfPrioritizingSuccessfulHarvestedActualResponses)

            if(found == null || (config.externalRequestResponseSelectionStrategy!= EMConfig.ExternalRequestResponseSelectionStrategy.EXACT && preferSuccess && !(found as HttpWsResponseParam).isStatusCodeInSuccessFamily())){
                when(config.externalRequestResponseSelectionStrategy) {
                    EMConfig.ExternalRequestResponseSelectionStrategy.CLOSEST_SAME_PATH ->{
                        val requestsFromSameURL = findRequestsFromSameURLPath(
                            httpRequest.getDescription())
                        if (requestsFromSameURL.isNotEmpty()){
                            val success = if (preferSuccess){
                                 requestsFromSameURL.filter { (it.value.param as? HttpWsResponseParam)?.isStatusCodeInSuccessFamily() == true }
                            }else null
                            val candidates = if (success?.isNotEmpty() == true) success else requestsFromSameURL
                            val rand = randomness.choose(candidates.keys)
                            found = actualResponses[rand]?.param?.copy() as? ResponseParam
                        }
                    }
                    EMConfig.ExternalRequestResponseSelectionStrategy.CLOSEST_SAME_DOMAIN -> {
                        val closestRequest = findClosestRequestFromSameDomain(httpRequest.getDescription(), preferSuccess)
                        if (closestRequest != null) {
                            found = (actualResponses[closestRequest]?.param?.copy() as? ResponseParam)
                        }else if (preferSuccess){
                            val anyClosestRequest = findClosestRequestFromSameDomain(httpRequest.getDescription(), false)
                            if (anyClosestRequest != null)
                                found = actualResponses[anyClosestRequest]?.param?.copy() as? ResponseParam
                        }
                    }
                    EMConfig.ExternalRequestResponseSelectionStrategy.RANDOM -> {
                        val success = if (preferSuccess) actualResponses.filter { (it.value.param as? HttpWsResponseParam)?.isStatusCodeInSuccessFamily() == true } else null
                        val candidates = if (success?.isNotEmpty() == true) success else actualResponses
                        val randomIndex = randomness.nextInt(candidates.size)
                        found = candidates[candidates.keys.toList()[randomIndex]]?.param?.copy() as? ResponseParam
                    }
                    else -> {}
                }
            }
        }
        if (found != null) {
            seededResponses.add(httpRequest.getDescription())
            if ((found as HttpWsResponseParam).isStatusCodeInSuccessFamily())
                seededSuccessfulResponses.add(httpRequest.getDescription())
        }
        return found
    }

    /**
     * add http request to queue for sending them to real external services
     */
    fun addHttpRequests(requests: List<HttpExternalServiceRequest>) {
        if (requests.isEmpty()) return

        if (!config.isEnabledHarvestingActualResponse())
            return

        /*
        Apart from GET and POST, other HTTP verbs are excluded for now to avoid side effects.
         */
        requests
            .filter { it.method.equals("GET", ignoreCase = true) || it.method.equals("POST", ignoreCase = true) }
            .forEach {
                addRequest(it)
            }
    }

    private fun addRequest(request: HttpExternalServiceRequest) {
        if (startedRequests.contains(request.getDescription())) {
            return
        }

        if(unknownHosts.contains(request.getHostName())){
            return
        }

        startedRequests.add(request.getDescription())

        updateExtractedObjectDto()

        val task = Runnable {
            val clientId = Thread.currentThread().id
            val httpWsClient = clients.getOrPut(clientId){initClient()}
            sendRequestToRealExternalService(clientId, httpWsClient, request)
        }

        workerPool.execute(task)
    }

    private fun initClient() : Client{

//        val clientConfiguration = ClientConfig()
//                .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
//                .property(ClientProperties.READ_TIMEOUT, config.tcpTimeoutMs)
//                //workaround bug in Jersey client
//                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
//                .property(ClientProperties.FOLLOW_REDIRECTS, false)

        val clientConfiguration = ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, 2_000)
                .property(ClientProperties.READ_TIMEOUT, 2_000)
                //workaround bug in Jersey client
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .property(ClientProperties.FOLLOW_REDIRECTS, true)

        return ClientBuilder.newBuilder()
                .sslContext(PreDefinedSSLInfo.getSSLContext())  // configure ssl certificate
                .hostnameVerifier(PreDefinedSSLInfo.allowAllHostNames()) // configure all hostnames
                .withConfig(clientConfiguration).build()
    }

    private fun buildInvocation(httpWsClient: Client, httpRequest: HttpExternalServiceRequest): Invocation {
        val build = httpWsClient.target(httpRequest.actualAbsoluteURL).request("*/*").apply {
            val handledHeaders = httpRequest.headers.filterNot { skipHeaders.contains(it.key.lowercase()) }
            if (handledHeaders.isNotEmpty())
                handledHeaders.forEach { (t, u) -> this.header(t, u) }
            //if we do not do this, we can run out of ephemeral ports
            this.header("Connection", "keep-alive")
        }

        val bodyEntity = if (httpRequest.body != null) {
            val contentType = httpRequest.getContentType()
            if (contentType != null)
                Entity.entity(httpRequest.body, contentType)
            else
                Entity.entity(httpRequest.body, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else {
            null
        }

        return when {
            httpRequest.method.equals("GET", ignoreCase = true) -> build.buildGet()
            httpRequest.method.equals("POST", ignoreCase = true) -> build.buildPost(bodyEntity)
            httpRequest.method.equals("PUT", ignoreCase = true) -> build.buildPut(bodyEntity)
            httpRequest.method.equals("DELETE", ignoreCase = true) -> build.buildDelete()
            httpRequest.method.equals("PATCH", ignoreCase = true) -> build.build("PATCH", bodyEntity)
            httpRequest.method.equals("OPTIONS", ignoreCase = true) -> build.build("OPTIONS")
            httpRequest.method.equals("HEAD", ignoreCase = true) -> build.build("HEAD")
            httpRequest.method.equals("TRACE", ignoreCase = true) -> build.build("TRACE")
            else -> {
                throw IllegalStateException("NOT SUPPORT to create invocation for method ${httpRequest.method}")
            }
        }
    }

    private fun createInvocationToRealExternalService(clientId: Long, httpWsClient: Client, httpRequest: HttpExternalServiceRequest): Response? {
        return try {
            buildInvocation(httpWsClient, httpRequest).invoke()
        } catch (e: ProcessingException) {

            log.debug(
                "There has been an issue in accessing external service with url (${httpRequest.getDescription()}): {}",
                e
            )

            when {
                TcpUtils.isTooManyRedirections(e) -> {
                    return null
                }

                TcpUtils.isTimeout(e) -> {
                    return null
                }

                TcpUtils.isOutOfEphemeralPorts(e) -> {
                    httpWsClient.close() //make sure to release any resource
                    clients.replace(clientId, ClientBuilder.newClient())

                    TcpUtils.handleEphemeralPortIssue()

                    createInvocationToRealExternalService(clientId, clients[clientId]!!, httpRequest)
                }

                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    log.warn("TCP connection to Real External Service: ${e.cause!!.message}")
                    return null
                }

                TcpUtils.isRefusedConnection(e) -> {
                    log.warn("Failed to connect Real External Service with TCP with url ($httpRequest).")
                    return null
                }

                TcpUtils.isUnknownHost(e) -> {
                    unknownHosts.add(httpRequest.getHostName())
                    return null
                }

                else -> throw e
            }
        }
    }

    private fun handleActualResponse(response: Response?): ActualResponseInfo? {
        response ?: return null
        val status = response.status
        var statusGene = HttpWsResponseParam.getDefaultStatusEnumGene()
        if (!statusGene.values.contains(status))
            statusGene = EnumGene(name = statusGene.name, statusGene.values.plus(status))

        val body = response.readEntity(String::class.java)
        val node = getJsonNodeFromText(body)

        // TODO handle content type
        val responseParam = if (node != null) {
            getHttpResponse(node, statusGene).apply {
                setGeneBasedOnString(responseBody, body)
            }
        } else {
            HttpWsResponseParam(
                status = statusGene,
                responseBody = OptionalGene(
                    ACTUAL_RESPONSE_GENE_NAME,
                    StringGene(ACTUAL_RESPONSE_GENE_NAME, value = body)
                )
            )
        }

        (responseParam as HttpWsResponseParam).setStatus(status)
        return ActualResponseInfo(body, responseParam)
    }

    private fun getHttpResponse(node: JsonNode, statusGene: EnumGene<Int>): ResponseParam {
        synchronized(extractedObjectDto) {
            val found = if (extractedObjectDto.isEmpty()) null else parseJsonNodeAsGene(
                ACTUAL_RESPONSE_GENE_NAME,
                node,
                extractedObjectDto
            )
            return if (found != null)
                HttpWsResponseParam(status = statusGene, responseBody = OptionalGene(ACTUAL_RESPONSE_GENE_NAME, found))
            else {
                val parsed = parseJsonNodeAsGene(ACTUAL_RESPONSE_GENE_NAME, node)
                HttpWsResponseParam(
                    status = statusGene,
                    responseBody = wrapWithOptionalGene(parsed, true) as OptionalGene
                )
            }
        }
    }

    private fun updateExtractedObjectDto() {
        synchronized(extractedObjectDto) {
            val infoDto = rc.getSutInfo()!!
            val map = ParserDtoUtil.getOrParseDtoWithSutInfo(infoDto, config.enableSchemaConstraintHandling)
            if (map.isNotEmpty()) {
                map.forEach { (t, u) ->
                    extractedObjectDto.putIfAbsent(t, u)
                }
            }
        }
    }

    /**
     * harvest the existing action [externalServiceAction] with collected actual responses
     * only if it is never seeded or it is never seeded with successful responses
     */
    fun harvestExistingExternalActionIfNeverSeeded(
        externalServiceAction: HttpExternalServiceAction,
        probability: Double
    ): Boolean {
        val des = externalServiceAction.request.getDescription()
        if ((!seededResponses.contains(des) || (config.probOfPrioritizingSuccessfulHarvestedActualResponses > 0 && !seededSuccessfulResponses.contains(des)))
            //&& actualResponses.containsKey(des)
        ) {
            return harvestExistingGeneBasedOn(externalServiceAction.response.responseBody, probability)
        }
        return false
    }

    /**
     * harvest the existing mocked response with collected actual responses
     */
    fun harvestExistingGeneBasedOn(geneToMutate: Gene, probability: Double): Boolean {
        try {
            val template = getACopyOfItsActualResponseIfExist(geneToMutate, probability)?.responseBody ?: return false

            val v = ParamUtil.getValueGene(geneToMutate)
            val t = ParamUtil.getValueGene(template)
            if (v::class.java == t::class.java) {
                v.copyValueFrom(t)
                return true
            } else if (v is StringGene) {
                // add template as part of specialization
                v.addChild(t)
                v.selectedSpecialization = v.specializationGenes.indexOf(t)
                return true
            }
            LoggingUtil.uniqueWarn(
                log,
                "Fail to mutate gene (${geneToMutate::class.java.name}) based on a given gene (${template::class.java.name})"
            )
            return false
        } catch (e: Exception) {
            LoggingUtil.uniqueWarn(
                log,
                "Fail to mutate gene based on a given gene and an exception (${e.message}) is thrown"
            )
            return false
        }
    }


    /**
     * @param key is description of request
     * @return all harvested responses whose request has same url path as [key]
     */
    private fun findRequestsFromSameURLPath(key: String) : Map<String, ActualResponseInfo>{
        return actualResponses.filter { matchRequestFromSameMethodAndURL(it.key, key)}
    }

    /**
     * Finds the closest harvested requested based on the given key.
     * Uses Levenshtein Distance to calculate the distance, then selects the
     * shortest. If none exists, returns null.
     */
    private fun findClosestRequestFromSameDomain(key: String, onlySuccess: Boolean): String? {
        var out: String? = null
        var diff = 100.0

        actualResponses.forEach { (k, v) ->
            val httpWsResponseParam = v.param as HttpWsResponseParam

            if (matchRequestFromSameDomain(key, k)) {
                if (!onlySuccess || httpWsResponseParam.isStatusCodeInSuccessFamily()) {
                    val ld = LevenshteinDistance.getDefaultInstance().apply(k, key).toDouble()

                    if (ld == 0.0) {
                        out = k
                    } else {
                        val nDiff = (ld / key.length.toDouble()) * 100.0

                        if (nDiff < diff) {
                            diff = nDiff
                            out = k
                        }
                    }
                }
            }
        }
        return out
    }

    /**
     * A request description contains information about the method, url, headers, and body.
     * This extract the URL information as [URL] for ease of use.
     *
     * e.g.: GET::http://exists.local:12354/api/fzz::[]::{none}
     */
    private fun getURLFromRequestDescription(requestDescription: String): URL {
        return URL(getTextualURLFromRequestDescription(requestDescription))
    }


    /**
     * A request description contains information about the method, url, headers, and body.
     * This extract the request method as [String].
     *
     * e.g.: GET::http://exists.local:12354/api/fzz::[]::{none}
     */
    private fun getMethodFromRequestDescription(requestDescription: String): String {
        return requestDescription.split("::")[0].lowercase()
    }

    private fun matchRequestFromSameDomain(left: String, right: String): Boolean {
        val leftMethod = getMethodFromRequestDescription(left)
        val leftProtocol = getURLFromRequestDescription(left).protocol
        val leftHostname = getURLFromRequestDescription(left).host

        val rightMethod = getMethodFromRequestDescription(right)
        val rightProtocol = getURLFromRequestDescription(right).protocol
        val rightHostname = getURLFromRequestDescription(right).host

        return leftMethod.equals(rightMethod) && leftProtocol.equals(rightProtocol) && leftHostname.equals(rightHostname)
    }

    private fun matchRequestFromSameMethodAndURL(aRequestDescription: String, bRequestDescription: String): Boolean{
        val aMethodAndPath = getTextualMethodURLFromRequestDescription(aRequestDescription)
        val bMethodAndPath = getTextualMethodURLFromRequestDescription(bRequestDescription)
        val aLast = getLastIndexOfURLPath(aMethodAndPath)
        val bLast = getLastIndexOfURLPath(bMethodAndPath)
        if (aLast == -1 || bLast == -1) return false
        if (aLast != bLast) return false

        return aMethodAndPath.substring(aLast)  == bMethodAndPath.substring(bLast)
    }

    private fun getLastIndexOfURLPath(url: String) : Int{
        if (url.contains("?")) return url.indexOf('?')
        if (url.contains("#")) return url.indexOf('#')
        return url.length
    }
}
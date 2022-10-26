package org.evomaster.core.problem.external.service.httpws

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.external.service.httpws.param.HttpWsResponseParam
import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.problem.util.ParserDtoUtil
import org.evomaster.core.problem.util.ParserDtoUtil.parseJsonNodeAsGene
import org.evomaster.core.problem.util.ParserDtoUtil.wrapWithOptionalGene
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

class HarvestActualHttpWsResponseHandler {

    @Inject(optional = true)
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var config: EMConfig

    private lateinit var httpWsClient : Client

    private lateinit var threadToHandleRequest: Thread


    companion object {
        private val log: Logger = LoggerFactory.getLogger(HarvestActualHttpWsResponseHandler::class.java)
        const val ACTUAL_RESPONSE_GENE_NAME = "ActualResponse"

        init{
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        }
    }

    /**
     * key is actual request based on [HttpExternalServiceRequest.getDescription]
     * value is an actual response info
     */
    private val actualResponses = mutableMapOf<String, ActualResponseInfo>()

    private val cachedRequests = mutableMapOf<String, HttpExternalServiceRequest>()

    /**
     * need it for wait and notify in kotlin
     */
    private val lock = Object()

    /**
     * an queue for handling urls for
     */
    private val queue = ConcurrentLinkedQueue<String>()

    private val extractedObjectDto = mutableMapOf<String, Gene>()

    private val jacksonMapper = ObjectMapper()

    /*
        skip headers if they depend on the client
        shall we skip Connection
     */
    private val skipHeaders = listOf("user-agent","host","accept-encoding")

    @PostConstruct
    fun initialize() {
        if (config.probOfHarvestingResponsesFromActualExternalServices > 0){
            val clientConfiguration = ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
                .property(ClientProperties.READ_TIMEOUT, config.tcpTimeoutMs)
                //workaround bug in Jersey client
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .property(ClientProperties.FOLLOW_REDIRECTS, false)


            httpWsClient = ClientBuilder.newBuilder()
                .sslContext(PreDefinedSSLInfo.getSSLContext())  // configure ssl certificate
                .hostnameVerifier(PreDefinedSSLInfo.allowAllHostNames()) // configure all hostnames
                .withConfig(clientConfiguration).build()

            threadToHandleRequest = object :Thread() {
                override fun run() {
                    while (true) {
                        sendRequestToRealExternalService()
                    }
                }
            }
            threadToHandleRequest.start()
        }
    }

    @PreDestroy
    private fun preDestroy() {
        if (config.probOfHarvestingResponsesFromActualExternalServices > 0){
            threadToHandleRequest.interrupt()
            httpWsClient.close()
        }

    }

    @Synchronized
    private fun sendRequestToRealExternalService() {
        synchronized(lock){
            while (queue.size == 0) {
                lock.wait()
            }
            val first = queue.remove()
            val info = handleActualResponse(createInvocationToRealExternalService(cachedRequests[first]?:throw IllegalStateException("Fail to get Http request with description $first")))
            if (info != null)
                actualResponses[first] = info
            else
                LoggingUtil.uniqueWarn(log, "Fail to harvest actual responses from GET $first")
        }
    }

    fun addHttpRequests(requests: List<HttpExternalServiceRequest>){
        // only harvest responses with GET method
        val filter = requests.filter { it.method.equals("GET", ignoreCase = true) }
        synchronized(cachedRequests){
            filter.forEach { cachedRequests.putIfAbsent(it.getDescription(), it) }
        }

        addRequests(filter.map { it.getDescription() })
    }

    private fun addRequests(requests : List<String>) {
        if (requests.isEmpty()) return

        if (config.probOfHarvestingResponsesFromActualExternalServices == 0.0)
            return

        val notInCollected = requests.filterNot { actualResponses.containsKey(it) }.distinct()
        if (notInCollected.isEmpty()) return

        synchronized(lock){
            val newRequests = notInCollected.filterNot { queue.contains(it) }
            if (newRequests.isEmpty())
                return
            lock.notify()
            queue.addAll(newRequests)
        }
    }

    private fun createInvocationToRealExternalService(httpRequest : HttpExternalServiceRequest) : Response?{
        return try {
            httpWsClient.target(httpRequest.actualAbsoluteURL).request("*/*").apply {
                val handledHeaders = httpRequest.headers.filterNot { skipHeaders.contains(it.key.lowercase()) }
                if (handledHeaders.isNotEmpty())
                    handledHeaders.forEach { (t, u) -> this.header(t, u) }
            }.buildGet().invoke()
        } catch (e: ProcessingException) {

            log.debug("There has been an issue in accessing external service with url (${httpRequest.getDescription()}): {}", e)

            when {
                TcpUtils.isTooManyRedirections(e) -> {
                    return null
                }

                TcpUtils.isTimeout(e) -> {
                    return null
                }

                TcpUtils.isOutOfEphemeralPorts(e) -> {
                    httpWsClient.close() //make sure to release any resource
                    httpWsClient = ClientBuilder.newClient()

                    TcpUtils.handleEphemeralPortIssue()

                    createInvocationToRealExternalService(httpRequest)
                }

                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    log.warn("TCP connection to Real External Service: ${e.cause!!.message}")
                    return null
                }

                TcpUtils.isRefusedConnection(e) -> {

                    log.warn("Failed to connect Real External Service with TCP with url ($httpRequest).")
                    return null
                }

                else -> throw e
            }
        }
    }

    private fun handleActualResponse(response: Response?) : ActualResponseInfo? {
        response?:return null
        val body = response.readEntity(String::class.java)
        val node = handleJsonResponse(body)
        val responseParam = if(node != null){
            getHttpResponse(node, 0)
        }else{
            HttpWsResponseParam(responseBody = OptionalGene(ACTUAL_RESPONSE_GENE_NAME, StringGene(ACTUAL_RESPONSE_GENE_NAME, value = body)))
        }
        return ActualResponseInfo(body, responseParam)
    }

    private fun handleJsonResponse(textualResponse: String) : JsonNode?{
        return try {
            jacksonMapper.readTree(textualResponse)
        }catch (e: Exception){
            null
        }
    }

    private fun getHttpResponse(node: JsonNode, times : Int) : ResponseParam{
        var anotherAttempt = (times == 0)
        if (extractedObjectDto.isEmpty()){
            updateExtractedObjectDto()
            anotherAttempt = false
        }

        val found = if (extractedObjectDto.isEmpty()) null else parseJsonNodeAsGene(ACTUAL_RESPONSE_GENE_NAME, node, extractedObjectDto)
        return if (found != null)
            HttpWsResponseParam(responseBody = OptionalGene(ACTUAL_RESPONSE_GENE_NAME, found))
        else if (anotherAttempt){
            updateExtractedObjectDto()
            getHttpResponse(node, 1)
        } else {
            val parsed = parseJsonNodeAsGene(ACTUAL_RESPONSE_GENE_NAME, node)
            HttpWsResponseParam(responseBody = wrapWithOptionalGene(parsed, true) as OptionalGene)
        }
    }


    private fun updateExtractedObjectDto(){
        synchronized(rc){
            val infoDto = rc.getSutInfo()!!
            val map = ParserDtoUtil.getOrParseDtoWithSutInfo(infoDto)
            if (map.isNotEmpty()){
                map.forEach { (t, u) ->
                    extractedObjectDto.putIfAbsent(t, u)
                }
            }
        }
    }
}
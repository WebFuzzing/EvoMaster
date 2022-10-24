package org.evomaster.core.problem.external.service.httpws

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo
import org.evomaster.core.EMConfig
import org.evomaster.core.remote.TcpUtils
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

class HarvestActualHttpWsResponseHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HarvestActualHttpWsResponseHandler::class.java)

        init{
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        }
    }

    @Inject
    private lateinit var config: EMConfig

    private lateinit var httpWsClient : Client

    /**
     * key is actual request based on [HttpExternalServiceRequest.getDescription]
     * value is an actual response info
     */
    private val actualResponses = mutableMapOf<String, ActualResponseInfo>()

    @PostConstruct
    fun initialize() {
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
    }

    @PreDestroy
    private fun preDestroy() {
        httpWsClient.close()
    }



    private fun createInvocationToRealExternalService(url : String) : Response?{
        return try {
            httpWsClient.target(url).request("*/*").buildGet().invoke()
        } catch (e: ProcessingException) {

            log.debug("There has been an issue in accessing external service with url ($url): {}", e)

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

                    createInvocationToRealExternalService(url)
                }

                TcpUtils.isStreamClosed(e) || TcpUtils.isEndOfFile(e) -> {
                    log.warn("TCP connection to Real External Service: ${e.cause!!.message}")
                    return null
                }

                TcpUtils.isRefusedConnection(e) -> {

                    log.warn("Failed to connect Real External Service with TCP with url ($url).")
                    return null
                }

                else -> throw e
            }
        }
    }
}
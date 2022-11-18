package org.evomaster.core.problem.external.service.httpws

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.evomaster.core.logging.LoggingUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Represent the external service related information including
 * WireMock server and ExternalServiceInfo collected from SUT.
 */
class HttpWsExternalService(
    /**
     * External service information collected from SUT
     */
    private val externalServiceInfo: HttpExternalServiceInfo,
) {

    companion object{

        private const val WIREMOCK_DEFAULT_RESPONSE_CODE = 404
        private const val WIREMOCK_DEFAULT_RESPONSE_MESSAGE = "Not Found"

        private val log: Logger = LoggerFactory.getLogger(HttpWsExternalServiceHandler::class.java)
    }

    /**
     * Initiated WireMock server for the external service
     */
    private val wireMockServer: WireMockServer? = null

    /**
     * Will initialise WireMock instance on a given IP address for a given port.
     */
    fun start(address: String) {
        if (!externalServiceInfo.isPartial()) {
            val port = externalServiceInfo.remotePort

            // TODO: Port need to be changed to the remote service port
            //  In CI also using remote ports as 80 and 443 fails
            val config =  WireMockConfiguration()
                .bindAddress(address)
                .extensions(ResponseTemplateTransformer(false))

            if (!externalServiceInfo.isHttp() && !externalServiceInfo.isHttps())
                LoggingUtil.uniqueWarn(log, "do not get explicit protocol for address ($address)")
            val applyHttps = externalServiceInfo.isHttps() || (!externalServiceInfo.isHttp() && externalServiceInfo.isDerivedHttps())

            if (applyHttps){
                config.httpsPort(port)
            }else {
                config.port(port)
            }

            val wm = WireMockServer(config)

            /*
                for SUT, its first connection is re-directed to a given ip address,
                to keep same behavior in generated tests, we do not start the WM accordingly
             */
            if (externalServiceInfo !is DefaultHttpExternalServiceInfo){
                wm.start()
                wm.stubFor(getDefaultWMMappingBuilder())
            }
        }
    }

    fun getWMDefaultPriority() = 100
    fun getWMDefaultUrlSetting() = "anyUrl()"
    fun getWMDefaultMethod() = "any"
    fun getWMDefaultCode() = WIREMOCK_DEFAULT_RESPONSE_CODE
    fun getWMDefaultMessage() = WIREMOCK_DEFAULT_RESPONSE_MESSAGE
    fun getWMDefaultConnectionHeader() = "close"

    /**
     * @return the default response setup for WM instance
     */
    fun getDefaultWMMappingBuilder() : MappingBuilder{
        return WireMock.any(WireMock.anyUrl()).atPriority(getWMDefaultPriority()).willReturn(
                WireMock.aResponse()
                    .withStatus(getWMDefaultCode())
                    .withBody(getWMDefaultMessage())
                    /*
                        do not set close connection in search
                        however, it will be needed in the generated tests
                     */
//                    .withHeader("Connection",getWMDefaultConnectionHeader())
            )
    }

    /**
     * Return the IP address of WireMock instance
     */
    fun getWireMockAddress(): String {
        return wireMockServer!!.options.bindAddress()
    }

    /**
     * Return the running port of WireMock instance
     */
    fun getWireMockPort(): Int {
        return wireMockServer!!.options.portNumber()
    }

    fun getWireMockServer(): WireMockServer {
        return wireMockServer!!
    }

    fun getRemoteHostName(): String {
        return externalServiceInfo.remoteHostname
    }

    fun getWireMockAbsoluteAddress(): String {
        return getWireMockAddress().plus(":").plus(getWireMockPort())
    }

    /**
     * Returns the signature of the external service.
     * Which is usually contains protocol, remote hostname, and port.
     */
    fun getSignature(): String {
        return externalServiceInfo.signature()
    }

    /**
     * Returns the active stub mappings from the WireMockServer
     */
    fun getStubs(): List<StubMapping> {
        return wireMockServer!!.stubMappings
    }


    /**
     * To get all the HTTP/S requests made to the WireMock instance
     *
     * TODO: For now watMatched serves no purpose. Should be handled when
     *  handling diff for the received requests
     *
     * TODO: Query parameters are not available under ServeEvent for some
     *  reasons. Need to check why.
     */
    fun getAllServedRequests(): List<HttpExternalServiceRequest> {
        return wireMockServer!!.allServeEvents.map { it ->
            HttpExternalServiceRequest(
                it.id,
                it.request.method.value(),
                it.request.url,
                it.request.absoluteUrl,
                it.wasMatched,
                getSignature(),
                externalServiceInfo.getDescriptiveURLPath()+it.request.url,
                // separated by a comma, https://www.rfc-editor.org/rfc/rfc9110.html#name-field-order
                it.request.headers?.all()?.filter { h-> h.key() != null }
                    ?.associate {h-> h.key() to h.values().filterNotNull().joinToString(",") } ?: emptyMap(),
                it.request.bodyAsString
            )
        }.toList()
    }

    /**
     * Reset WireMock to clean everything including stubs and
     * requests.
     */
    fun reset() {
        if (!externalServiceInfo.isPartial())
            wireMockServer!!.resetAll()
    }

    /**
     * Reset the served request on the respective WireMock
     * instance.
     */
    fun resetServedRequests() {
        if (!externalServiceInfo.isPartial())
            wireMockServer!!.resetRequests()
    }

    /**
     * To stop the WireMock server
     */
    fun stopWireMockServer() {
        if (!externalServiceInfo.isPartial())
            wireMockServer!!.stop()
    }

    /**
     * Will remove the stub mapping related to the given id
     */
    fun removeStub(stubId: UUID): Boolean {
        val stubMapping = wireMockServer!!.getStubMapping(stubId)
        if (stubMapping.isPresent) {
            wireMockServer!!.removeStubMapping(stubMapping.item)
            return true
        }
        return false
    }

    fun isActive(): Boolean {
        return wireMockServer != null
    }
}
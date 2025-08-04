package org.evomaster.core.problem.security.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.Metadata
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.SecuritySharedUtils
import org.evomaster.core.EMConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class HttpCallbackVerifier {

    @Inject
    private lateinit var config: EMConfig

    private var wireMockServer: WireMockServer? = null

    private var traceTokens: MutableMap<String, String> = mutableMapOf()

    val isActive: Boolean get() = wireMockServer != null && wireMockServer!!.isRunning

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpCallbackVerifier::class.java)
    }

    @PostConstruct
    fun init() {
        if (config.vulnerabilityAnalyser) {
            // TODO: Nothing to do now
        }
    }

    @PreDestroy
    fun destroy() {
        wireMockServer!!.stop()
        wireMockServer = null
    }

    fun initWireMockServer() {
        try {
            val config = WireMockConfiguration()
                .bindAddress(SecuritySharedUtils.HTTP_CALLBACK_VERIFIER)
                .extensions(ResponseTemplateTransformer(false))
                .port(19000) // Changed for testing purposes

            wireMockServer = WireMockServer(config)
            wireMockServer!!.start()
            wireMockServer!!.stubFor(getDefaultStub())
        } catch (e: Exception) {
            throw RuntimeException(
                e.message +
                        ". If it is macOS, please make sure loopback alias is set."
            )
        }
    }

    fun hasTokenForActionName(name: String, value: String): Boolean {
        return traceTokens.containsKey(name) && traceTokens[name] == value
    }

    fun isCallbackURL(value: String): Boolean {
        val pattern =
            """^http:\/\/${SecuritySharedUtils.HTTP_CALLBACK_VERIFIER}:[0-9]{5}\/sink\/.{36}""".toRegex()

        return pattern.matches(value)
    }

    fun getTraceTokenFromURL(url: String): String {
        if (isCallbackURL(url)) {
            val token = url.substringAfterLast("/")
            return token
        }

        return ""
    }

    fun generateCallbackLink(name: String): String {
        val token = UUID.randomUUID().toString()
        val ssrfPath = "/sink/$token"

        wireMockServer!!.stubFor(
            WireMock.any(WireMock.urlEqualTo(ssrfPath))
                .withMetadata(Metadata.metadata().attr("originalPath", name))
                .atPriority(1)
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody("OK")
                )

        )

        val link = "http://${SecuritySharedUtils.HTTP_CALLBACK_VERIFIER}:${wireMockServer!!.port()}$ssrfPath"

        traceTokens[name] = link

        return link
    }

    /**
     * @param name represents the [Action] name
     *
     * During stub creation, stubs are tagged with [Action] name in the metadata.
     */
    fun verify(name: String): Boolean {
        if (isActive) {
            wireMockServer!!.allServeEvents
                .filter { event -> event.wasMatched }
                .forEach { e ->
                    val matched = e.stubMapping.metadata
                    if (matched != null && matched.getString("originalPath") == name) {
                        return true
                    }
                }
        }

        return false
    }

    fun resetHTTPVerifier() {
        wireMockServer?.resetAll()
        wireMockServer?.stubFor(getDefaultStub())
        traceTokens.clear()
    }

    private fun getDefaultStub(): MappingBuilder {
        return WireMock.any(WireMock.anyUrl())
            .atPriority(100)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(418)
                    .withBody("I'm a teapot")
            )
    }
}

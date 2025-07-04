package org.evomaster.core.problem.security.verifiers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.common.Metadata.metadata
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.evomaster.client.java.instrumentation.shared.SecuritySharedUtils
import org.evomaster.core.problem.security.VulnerabilityVerifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class HttpCallbackVerifier : VulnerabilityVerifier() {

    private var wireMockServer: WireMockServer? = null

    private var traceTokens: MutableMap<String, String> = mutableMapOf()

    val isActive: Boolean get() = wireMockServer != null && wireMockServer!!.isRunning

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpCallbackVerifier::class.java)
    }

    override fun init() {
        try {
            val config = WireMockConfiguration()
                .bindAddress(SecuritySharedUtils.HTTP_CALLBACK_VERIFIER)
                .extensions(ResponseTemplateTransformer(false))
                .port(19000) // Changed for testing purposes

            wireMockServer = WireMockServer(config)
            wireMockServer!!.start()
            wireMockServer!!.stubFor(
                WireMock.any(WireMock.anyUrl())
                    .atPriority(100)
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(418)
                            .withBody("I'm a teapot")
                    )
            )
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to initialize SSRFVulnerabilityVerifier due to " +
                        e.message +
                        " If it is macOS, please make sure loopback alias is set."
            )
        }
    }

    fun generateCallbackLink(name: String): String {
        val token = UUID.randomUUID().toString()
        val ssrfPath = "/sink/$token"

        wireMockServer!!.stubFor(
            any(urlEqualTo(ssrfPath))
                .withMetadata(metadata().attr("originalPath", name))
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

    override fun verify(name: String): Boolean {
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

    override fun destroy() {
        wireMockServer!!.stop()
        wireMockServer = null
    }

    fun resetHTTPVerifier() {
        wireMockServer?.resetAll()
        // TODO: set default stub
        traceTokens.clear()
    }

}

package org.evomaster.core.problem.security.verifiers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.Metadata.metadata
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.evomaster.client.java.instrumentation.shared.SecuritySharedUtils
import org.evomaster.core.problem.security.VulnerabilityVerifier
import java.util.UUID

class HttpCallbackVerifier : VulnerabilityVerifier() {

    private var wireMockServer: WireMockServer? = null

    private var traceTokens: MutableMap<String, String> = mutableMapOf()

    override fun init() {
        try {
            val config = WireMockConfiguration()
                .bindAddress(SecuritySharedUtils.HTTP_CALLBACK_VERIFIER)
                .extensions(ResponseTemplateTransformer(false))
                .port(9000)

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
        val ssrfPath = "sink/$token"

        wireMockServer!!.stubFor(
            WireMock.post(WireMock.urlPathMatching(ssrfPath))
                .withMetadata(metadata().attr("originalPath", name))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody("OK")
                )

        )

        val link = "http://${SecuritySharedUtils.HTTP_CALLBACK_VERIFIER}:${wireMockServer!!.port()}/$ssrfPath"

        traceTokens[name] = link

        return link
    }

    override fun verify(name: String): Boolean {
        if (wireMockServer!!.allServeEvents.any { it -> it.wasMatched }) {
            wireMockServer!!.allServeEvents.filter { event -> event.wasMatched }
                .forEach { e ->
                    val matched = e.stubMapping.metadata.getString("originalPath")
                    if (matched == name) {
                        return true
                    }
                }
        }
        return false
    }

    override fun destroy() {
        wireMockServer!!.stop()
    }


    private fun resetHTTPVerifier() {
        wireMockServer?.resetAll()
        traceTokens.clear()
    }

}

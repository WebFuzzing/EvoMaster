package org.evomaster.core.problem.security.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.Metadata
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class HttpCallbackVerifier {

    @Inject
    private lateinit var config: EMConfig

    private var wireMockServer: WireMockServer? = null

    /**
     * Key holds the name of the [Action] and value holds the callback link generated for it.
     */
    private var actionCallbackLinkMapping: MutableMap<String, String> = mutableMapOf()

    val isActive: Boolean get() = wireMockServer != null && wireMockServer!!.isRunning

    private var counter: Long = 0

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpCallbackVerifier::class.java)
    }

    @PostConstruct
    fun init() {
        if (config.ssrf) {
            log.debug("Initializing {}", HttpCallbackVerifier::class.simpleName)
        }
    }

    @PreDestroy
    private fun destroy() {
        if (config.ssrf) {
            reset()
        }
    }

    fun initWireMockServer() {
        try {
            val config = WireMockConfiguration()
                .extensions(ResponseTemplateTransformer(false))
                .port(config.httpCallbackVerifierPort)

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

    fun isCallbackURL(value: String): Boolean {
        // Regex pattern looks for URL contains [HTTP_CALLBACK_VERIFIER] address and [HTTPCallbackVerifier]
        // port, along with the path /sink/ and UUID as token generated to make the callback URL unique.
        val pattern =
            """^http:\/\/localhost:${config.httpCallbackVerifierPort}\/sink\/.{36}""".toRegex()

        return pattern.matches(value)
    }

    /**
     * Method generates a unique callback link to be used as payload for SSRF.
     */
    fun generateCallbackLink(name: String): String {
        val ssrfPath = "/sink/${counter++}"

        wireMockServer!!.stubFor(
            WireMock.any(WireMock.urlEqualTo(ssrfPath))
                .withMetadata(Metadata.metadata().attr("ssrf", name))
                .atPriority(1)
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody("OK")
                )

        )

        val link = "http://localhost:${wireMockServer!!.port()}$ssrfPath"

        actionCallbackLinkMapping[name] = link

        return link
    }

    /**
     * @param name represents the Action name
     *
     * During stub creation, stubs are tagged with Action name in the metadata.
     */
    fun verify(name: String): Boolean {
        if (isActive) {
            wireMockServer!!.allServeEvents
                .filter { event -> event.wasMatched }
                .forEach { e ->
                    val matched = e.stubMapping.metadata
                    if (matched != null && matched.getString("ssrf") == name) {
                        return true
                    }
                }
        }

        return false
    }

    fun resetHTTPVerifier() {
        wireMockServer?.resetAll()
        wireMockServer?.stubFor(getDefaultStub())
        actionCallbackLinkMapping.clear()
        counter = 0
    }

    fun reset() {
        counter = 0
        wireMockServer?.stop()
        wireMockServer = null
        actionCallbackLinkMapping.clear()
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

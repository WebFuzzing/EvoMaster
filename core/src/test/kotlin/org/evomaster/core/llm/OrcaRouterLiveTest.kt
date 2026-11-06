package org.evomaster.core.llm

import org.evomaster.core.llm.orcarouter.OrcaRouterCatalogClient
import org.evomaster.core.llm.orcarouter.OrcaRouterCredentials
import org.evomaster.core.llm.orcarouter.OrcaRouterDefaults
import org.evomaster.core.llm.orcarouter.OrcaRouterOrigins
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * Live check through the provider code path that the integration actually ships.
 *
 * Requires ORCAROUTER_API_KEY (or ORCA_API_KEY). It is skipped when no key is configured so
 * that the regular test run stays offline; the release verification runs it with a real key.
 */
class OrcaRouterLiveTest {

    private fun apiKey(): String? =
        OrcaRouterCredentials.resolve(null)

    @Test
    fun testLiveCatalogAndInferenceThroughProvider() {

        val key = apiKey()
        assumeTrue(!key.isNullOrBlank(), "No OrcaRouter API key configured")

        // Model discovery must use the public inference origin, never the auth origin.
        val catalog = OrcaRouterCatalogClient(
            baseUrl = OrcaRouterOrigins.DEFAULT_API_BASE_URL,
            timeoutSeconds = 20
        ).fetchChat(key)

        assertFalse(catalog.degraded, "Live model discovery failed: ${catalog.note}")
        val chatModels = OrcaRouterCatalogClient.filterChat(catalog.models)
        assertTrue(chatModels.isNotEmpty(), "Live catalog advertised no chat-capable models")

        // Prefer the cyber-tuned model; fall back to the general router model (and then to any
        // chat model) if this account/workspace cannot use it.
        val ordered = buildList {
            add(OrcaRouterDefaults.DEFAULT_MODEL)
            add(OrcaRouterDefaults.FALLBACK_MODEL)
            addAll(chatModels.map { it.id })
        }.distinct().filter { id -> chatModels.any { it.id == id } }

        var lastFailure: Exception? = null
        var usedModel: String? = null
        for (id in ordered) {
            try {
                val model = LlmSupport.createModel(
                    LlmProvider.ORCAROUTER,
                    apiKey = key,
                    url = OrcaRouterOrigins.DEFAULT_API_BASE_URL,
                    modelName = id,
                    timeoutSeconds = 60,
                    temperature = 0.0
                )
                val answer = LlmSupport.chat(model, "Reply with the single word OK.")
                assertTrue(answer.isNotBlank())
                assertFalse(answer.contains(key!!))
                usedModel = id
                break
            } catch (e: Exception) {
                if (OrcaRouterCredentials.isAuthenticationFailure(e)) {
                    throw AssertionError("OrcaRouter rejected the configured API key", e)
                }
                lastFailure = e
            }
        }

        assertTrue(usedModel != null, "No OrcaRouter chat model could be used: ${lastFailure?.message}")
        println("LIVE OrcaRouter: ${chatModels.size} chat-capable models, used '$usedModel'")
    }
}

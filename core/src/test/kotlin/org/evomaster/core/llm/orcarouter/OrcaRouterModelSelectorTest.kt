package org.evomaster.core.llm.orcarouter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrcaRouterModelSelectorTest {

    private val live = OrcaRouterCatalogResult(
        models = listOf(
            OrcaRouterModel("vendor/a", setOf("openai"), setOf("text")),
            OrcaRouterModel("orcacyber-zero", setOf("openai"), setOf("text")),
            OrcaRouterModel("vendor/embed", setOf("embeddings"), setOf("text"))
        ),
        source = OrcaRouterCatalogSource.LIVE,
        degraded = false
    )

    @Test
    fun testDefaultsToCyberTunedModel() {
        val selected = OrcaRouterModelSelector.selectChatModel(live, null)
        assertEquals("orcacyber-zero", selected.id)
    }

    @Test
    fun testRequestedChatModelIsAccepted() {
        assertEquals("vendor/a", OrcaRouterModelSelector.selectChatModel(live, " vendor/a ").id)
    }

    @Test
    fun testNonChatOrUnknownModelFailsClosed() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterModelSelector.selectChatModel(live, "vendor/embed")
        }
        val message = error.message!!
        assertTrue(message.contains("not a chat-capable model"))
        assertTrue(message.contains("--llmListModels"))
        assertTrue(message.contains("vendor/a"))
    }

    @Test
    fun testUndeclaredFreeTextModelIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterModelSelector.selectChatModel(live, "some-model-i-typed")
        }
    }

    @Test
    fun testFallbackWhenCyberModelIsUnavailable() {
        val withoutCyber = live.copy(
            models = listOf(
                OrcaRouterModel("orcarouter/fusion", setOf("openai"), setOf("text")),
                OrcaRouterModel("vendor/a", setOf("openai"), setOf("text"))
            )
        )
        assertEquals("orcarouter/fusion", OrcaRouterModelSelector.selectChatModel(withoutCyber, null).id)
    }

    @Test
    fun testDegradedCatalogOnlyAcceptsVerifiedModels() {
        val seed = OrcaRouterCatalogResult(
            models = OrcaRouterCatalogClient.verifiedSeed(),
            source = OrcaRouterCatalogSource.SEED,
            degraded = true,
            note = "model catalog request failed (IOException)"
        )
        assertEquals("orcacyber-zero", OrcaRouterModelSelector.selectChatModel(seed, null).id)

        val error = assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterModelSelector.selectChatModel(seed, "vendor/not-verified")
        }
        assertTrue(error.message!!.contains("verified fallback list"))

        val rendered = OrcaRouterModelSelector.render(seed)
        assertTrue(rendered.contains("live discovery unavailable"))
        assertTrue(rendered.contains("orcacyber-zero"))
        assertTrue(rendered.contains("[recommended]"))
        assertTrue(rendered.contains("reasoning=low/medium/high/xhigh"))
    }

    @Test
    fun testRenderLiveCatalogListsOnlyChatModels() {
        val rendered = OrcaRouterModelSelector.render(live)
        assertTrue(rendered.contains("live catalog"))
        assertTrue(rendered.contains("vendor/a"))
        assertFalse(rendered.contains("vendor/embed"))
        assertFalse(rendered.contains("[fallback]"))
    }

    @Test
    fun testEmptyChatCatalogIsReported() {
        val empty = OrcaRouterCatalogResult(
            models = listOf(OrcaRouterModel("vendor/embed", setOf("embeddings"), setOf("text"))),
            source = OrcaRouterCatalogSource.LIVE,
            degraded = false
        )
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterModelSelector.selectChatModel(empty, null)
        }
        assertTrue(OrcaRouterModelSelector.render(empty).contains("No chat-capable models"))
    }
}

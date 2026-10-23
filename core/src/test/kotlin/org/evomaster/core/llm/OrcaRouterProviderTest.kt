package org.evomaster.core.llm

import org.evomaster.core.EMConfig
import org.evomaster.core.llm.orcarouter.OrcaRouterCredentials
import org.evomaster.core.llm.orcarouter.OrcaRouterDefaults
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrcaRouterProviderTest {

    private val fakeKey = "sk-orca-unit-test-not-a-real-key"

    @Test
    fun testProviderIsRegisteredAsNamedEnumValue() {
        val provider = LlmProvider.valueOf("ORCAROUTER")
        assertTrue(LlmProvider.entries.contains(provider))
        assertEquals("orcacyber-zero", OrcaRouterDefaults.DEFAULT_MODEL)
        assertEquals("orcarouter/fusion", OrcaRouterDefaults.FALLBACK_MODEL)
    }

    @Test
    fun testChatModelBuildsThroughExistingFactory() {
        val model = LlmSupport.createModel(
            LlmProvider.ORCAROUTER,
            apiKey = fakeKey,
            modelName = "orcacyber-zero"
        )
        assertNotNull(model)
        assertEquals("OpenAiChatModel", model.javaClass.simpleName)
    }

    @Test
    fun testStreamingModelBuildsThroughExistingFactory() {
        val model = LlmSupport.createStreamingModel(
            LlmProvider.ORCAROUTER,
            apiKey = fakeKey,
            modelName = "orcarouter/fusion"
        )
        assertNotNull(model)
        assertEquals("OpenAiStreamingChatModel", model.javaClass.simpleName)
    }

    @Test
    fun testMissingKeyFailsWithActionableMessage() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            LlmSupport.createModel(LlmProvider.ORCAROUTER, apiKey = null)
        }
        assertTrue(error.message!!.contains("sk-orca"))
        assertTrue(error.message!!.contains("console"))
    }

    @Test
    fun testWrongKeyShapeDoesNotLeakTheValue() {
        val wrong = "wrong-key-value-4321"
        val error = assertThrows(IllegalArgumentException::class.java) {
            LlmSupport.createModel(LlmProvider.ORCAROUTER, apiKey = wrong)
        }
        assertFalse(error.message!!.contains(wrong))
        assertTrue(error.message!!.contains(OrcaRouterCredentials.KEY_PREFIX))
    }

    @Test
    fun testOtherProvidersKeepWorking() {
        // Adding a named provider must not change the behaviour of the existing ones.
        assertNotNull(LlmSupport.createModel(LlmProvider.MOCK))
        assertNotNull(LlmSupport.createModel(LlmProvider.OPENAI, apiKey = "sk-fake-openai-key"))
        assertNotNull(LlmSupport.createModel(LlmProvider.DEEPSEEK, apiKey = "sk-fake-deepseek-key"))
        assertNotNull(LlmSupport.createModel(LlmProvider.OLLAMA))

        val error = assertThrows(RuntimeException::class.java) {
            LlmSupport.createModel(LlmProvider.OPENAI, apiKey = null)
        }
        assertTrue(error.message!!.contains("API key required for OpenAI"))
    }

    @Test
    fun testModelListingConfigOptionExistsAndIsOffByDefault() {
        val config = EMConfig()
        assertFalse(config.llmListModels)
        assertFalse(config.llm)
        // the option is discoverable on the command line, next to the other llm options
        val parser = EMConfig.validateOptions(arrayOf("--llm", "true", "--llmListModels", "true"))
        val options = parser.parse("--llm", "true", "--llmListModels", "true")
        assertTrue(options.has("llmListModels"))
    }
}

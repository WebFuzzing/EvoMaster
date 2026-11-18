package org.evomaster.core.llm.orcarouter

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class OrcaRouterCatalogTest {

    private val catalogJson = """
        {
          "data": [
            {
              "id": "vendor/text-only",
              "supported_endpoint_types": ["openai"],
              "architecture": {"input_modalities": ["text"]},
              "context_length": 128000
            },
            {
              "id": "vendor/vision-chat",
              "supported_endpoint_types": ["openai", "anthropic"],
              "architecture": {"input_modalities": ["text", "image"]}
            },
            {
              "id": "vendor/embed",
              "supported_endpoint_types": ["embeddings"],
              "architecture": {"input_modalities": ["text"]}
            },
            {
              "id": "vendor/image-gen",
              "supported_endpoint_types": ["image-generation"],
              "architecture": {"input_modalities": ["text"], "output_modalities": ["image"]}
            },
            {
              "id": "vendor/video-gen",
              "supported_endpoint_types": ["openai-video"],
              "architecture": {"input_modalities": ["text"]}
            },
            {
              "id": "vendor/rerank",
              "supported_endpoint_types": ["jina-rerank"],
              "architecture": {"input_modalities": ["text"]}
            },
            {
              "id": "vendor/undeclared",
              "architecture": {"input_modalities": ["text"]}
            },
            {
              "id": "vendor/gpt-5.5",
              "supportedEndpointTypes": ["openai"],
              "architecture": {"inputModalities": ["text"]},
              "reasoning": {"efforts": ["low", "medium", "high", "xhigh"]}
            }
          ]
        }
    """.trimIndent()

    private fun withServer(handler: (HttpExchange) -> Unit, block: (baseUrl: String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}/v1")
        } finally {
            server.stop(0)
        }
    }

    private fun client(baseUrl: String) = OrcaRouterCatalogClient(baseUrl = baseUrl, timeoutSeconds = 5)

    @Test
    fun testParsesCatalogAndFiltersByCapability() {
        withServer({ exchange ->
            exchange.sendResponseHeaders(200, catalogJson.toByteArray().size.toLong())
            exchange.responseBody.write(catalogJson.toByteArray())
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat("sk-orca-fake-key")
            assertFalse(result.degraded)
            assertEquals(OrcaRouterCatalogSource.LIVE, result.source)

            val chat = OrcaRouterCatalogClient.filterChat(result.models).map { it.id }
            assertEquals(listOf("vendor/text-only", "vendor/vision-chat", "vendor/gpt-5.5"), chat)

            val embeddings = OrcaRouterCatalogClient
                .filterByCapability(result.models, OrcaRouterCapability.EMBEDDING).map { it.id }
            assertEquals(listOf("vendor/embed"), embeddings)

            val images = OrcaRouterCatalogClient
                .filterByCapability(result.models, OrcaRouterCapability.IMAGE_GENERATION).map { it.id }
            assertEquals(listOf("vendor/image-gen"), images)

            val videos = OrcaRouterCatalogClient
                .filterByCapability(result.models, OrcaRouterCapability.VIDEO_GENERATION).map { it.id }
            assertEquals(listOf("vendor/video-gen"), videos)

            val rerank = OrcaRouterCatalogClient
                .filterByCapability(result.models, OrcaRouterCapability.RERANK).map { it.id }
            assertEquals(listOf("vendor/rerank"), rerank)
        }
    }

    @Test
    fun testMultimodalSelectionFailsClosed() {
        withServer({ exchange ->
            exchange.sendResponseHeaders(200, catalogJson.toByteArray().size.toLong())
            exchange.responseBody.write(catalogJson.toByteArray())
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat("sk-orca-fake-key")

            val imageChat = OrcaRouterCatalogClient.filterChatByModalities(result.models, setOf("image"))
            assertEquals(listOf("vendor/vision-chat"), imageChat.map { it.id })

            // No catalogue entry declares audio support, so nothing may be offered.
            assertTrue(OrcaRouterCatalogClient.filterChatByModalities(result.models, setOf("audio")).isEmpty())

            // A model whose metadata is absent is not assumed to support extra modalities.
            assertTrue(imageChat.none { it.id == "vendor/text-only" })
        }
    }

    @Test
    fun testUsesChatCapabilityQueryParameter() {
        var query: String? = null
        withServer({ exchange ->
            query = exchange.requestURI.query
            exchange.sendResponseHeaders(200, catalogJson.toByteArray().size.toLong())
            exchange.responseBody.write(catalogJson.toByteArray())
        }) { baseUrl ->
            client(baseUrl).fetchChat("sk-orca-fake-key")
        }
        assertEquals("capability=chat", query)
    }

    @Test
    fun testHttpFailureFallsBackToVerifiedSeed() {
        withServer({ exchange ->
            exchange.sendResponseHeaders(401, -1)
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat("sk-orca-fake-key")
            assertTrue(result.degraded)
            assertEquals(OrcaRouterCatalogSource.SEED, result.source)
            assertNotNull(result.note)
            assertTrue(result.note!!.contains("401"))

            val ids = OrcaRouterCatalogClient.filterChat(result.models).map { it.id }
            assertTrue(ids.contains(OrcaRouterDefaults.DEFAULT_MODEL))
            assertTrue(ids.contains("openai/gpt-5.5"))

            // Verified metadata must survive the fallback.
            val gpt = result.models.first { it.id == "openai/gpt-5.5" }
            assertEquals(listOf("low", "medium", "high", "xhigh"), gpt.reasoningEfforts)
            assertEquals(setOf("text"), gpt.inputModalities)
        }
    }

    @Test
    fun testMissingKeyDoesNotCallServerAndKeepsSecretOutOfNote() {
        var called = false
        withServer({ exchange ->
            called = true
            exchange.sendResponseHeaders(200, 2)
            exchange.responseBody.write("{}".toByteArray())
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat(null)
            assertTrue(result.degraded)
            assertFalse(called)
            assertFalse(result.note!!.contains("sk-orca"))
        }
    }

    @Test
    fun testMalformedCatalogFallsBackToSeed() {
        withServer({ exchange ->
            val body = """{"data": [{"no_id": true}, {"id": ""}, 42]}"""
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.write(body.toByteArray())
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat("sk-orca-fake-key")
            assertTrue(result.degraded)
            assertEquals(OrcaRouterCatalogSource.SEED, result.source)
        }
    }

    @Test
    fun testOversizedCatalogResponseIsRejected() {
        withServer({ exchange ->
            val filler = "x".repeat(OrcaRouterCatalogClient.MAX_RESPONSE_BYTES + 1024)
            val body = """{"data":[],"filler":"$filler"}"""
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            try {
                exchange.responseBody.write(body.toByteArray())
            } catch (e: Exception) {
                // the client stops reading on purpose
            }
        }) { baseUrl ->
            val result = client(baseUrl).fetchChat("sk-orca-fake-key")
            assertTrue(result.degraded)
            assertTrue(result.note!!.contains("too large") || result.note!!.contains("failed"))
        }
    }

    @Test
    fun testBoundedItemCount() {
        val entries = (0 until OrcaRouterCatalogClient.MAX_MODELS + 50).joinToString(",") { i ->
            """{"id":"vendor/model-$i","supported_endpoint_types":["openai"]}"""
        }
        val body = """{"data":[$entries]}"""
        val parsed = client("http://127.0.0.1:1/v1").parseModels(body)
        assertEquals(OrcaRouterCatalogClient.MAX_MODELS, parsed.size)
    }
}

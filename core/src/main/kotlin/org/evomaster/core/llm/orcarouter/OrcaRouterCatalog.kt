package org.evomaster.core.llm.orcarouter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Capabilities used to filter the OrcaRouter model catalog. EvoMaster only sends text chat
 * requests today, but the catalog is modelled explicitly so that a capability can never be
 * inferred from a model name.
 */
enum class OrcaRouterCapability(val queryValue: String) {
    CHAT("chat"),
    EMBEDDING("embedding"),
    IMAGE_GENERATION("image"),
    VIDEO_GENERATION("video"),
    RERANK("rerank")
}

data class OrcaRouterModel(
    val id: String,
    val endpointTypes: Set<String>,
    val inputModalities: Set<String>,
    val contextLength: Int? = null,
    val reasoningEfforts: List<String> = emptyList(),
    val fromSeed: Boolean = false
)

enum class OrcaRouterCatalogSource { LIVE, SEED }

data class OrcaRouterCatalogResult(
    val models: List<OrcaRouterModel>,
    val source: OrcaRouterCatalogSource,
    val degraded: Boolean,
    val note: String? = null
)

/**
 * Reads the OrcaRouter model catalog (`GET /v1/models`) with bounded time, size and item count.
 *
 * Live discovery is authoritative. When it fails, a small, verified seed keeps a fresh
 * installation usable and is marked as degraded everywhere it is shown.
 */
class OrcaRouterCatalogClient(
    private val baseUrl: String = OrcaRouterOrigins.resolveApiBase(),
    private val timeoutSeconds: Long = 10,
    private val httpClient: HttpClient = defaultClient(timeoutSeconds)
) {

    companion object {
        const val MAX_RESPONSE_BYTES = 2_000_000
        const val MAX_MODELS = 500

        /** Endpoint types that mean "this model speaks a text-chat wire format we can use". */
        val CHAT_ENDPOINT_TYPES = setOf("openai", "anthropic", "gemini", "openai-response")

        /** Endpoint types that only serve non-chat workloads and must never enter the chat list. */
        val NON_CHAT_ENDPOINT_TYPES = setOf("image-generation", "openai-video", "jina-rerank")

        /**
         * Small verified catalog used when live discovery is unavailable. Only models whose
         * metadata has been checked are listed; capabilities are never guessed from the name.
         */
        fun verifiedSeed(): List<OrcaRouterModel> = listOf(
            OrcaRouterModel(
                id = OrcaRouterDefaults.DEFAULT_MODEL,
                endpointTypes = setOf("openai"),
                inputModalities = setOf("text"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = OrcaRouterDefaults.FALLBACK_MODEL,
                endpointTypes = setOf("openai"),
                inputModalities = setOf("text"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = "openai/gpt-5.5",
                endpointTypes = setOf("openai"),
                inputModalities = setOf("text"),
                reasoningEfforts = listOf("low", "medium", "high", "xhigh"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = "anthropic/claude-opus-4.8",
                endpointTypes = setOf("anthropic"),
                inputModalities = setOf("text"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = "google/gemini-3.5-flash",
                endpointTypes = setOf("gemini"),
                inputModalities = setOf("text"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = "deepseek/deepseek-v4-pro",
                endpointTypes = setOf("openai"),
                inputModalities = setOf("text"),
                fromSeed = true
            ),
            OrcaRouterModel(
                id = "orcarouter/auto",
                endpointTypes = setOf("openai"),
                inputModalities = setOf("text"),
                fromSeed = true
            )
        )

        fun defaultClient(timeoutSeconds: Long): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds.coerceIn(1, 30)))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

        /**
         * Chat models: the model must declare at least one text-chat endpoint type. Models that
         * only declare media/rerank endpoints are excluded.
         */
        fun filterChat(models: List<OrcaRouterModel>): List<OrcaRouterModel> =
            models.filter { it.endpointTypes.any { type -> type in CHAT_ENDPOINT_TYPES } }

        /**
         * Multimodal chat models: chat-capable and explicitly declaring every required input
         * modality. Undeclared modalities fail closed.
         */
        fun filterChatByModalities(
            models: List<OrcaRouterModel>,
            requiredModalities: Set<String>
        ): List<OrcaRouterModel> {
            val required = requiredModalities.map { it.lowercase() }.toSet()
            return filterChat(models).filter { model -> model.inputModalities.containsAll(required) }
        }

        fun filterByCapability(models: List<OrcaRouterModel>, capability: OrcaRouterCapability): List<OrcaRouterModel> =
            when (capability) {
                OrcaRouterCapability.CHAT -> filterChat(models)
                OrcaRouterCapability.EMBEDDING ->
                    models.filter { it.endpointTypes.any { t -> t == "embeddings" || t == "embedding" } }
                OrcaRouterCapability.IMAGE_GENERATION ->
                    models.filter { it.endpointTypes.contains("image-generation") }
                OrcaRouterCapability.VIDEO_GENERATION ->
                    models.filter { it.endpointTypes.contains("openai-video") }
                OrcaRouterCapability.RERANK ->
                    models.filter { it.endpointTypes.contains("jina-rerank") }
            }
    }

    /**
     * Fetches the catalog for [capability]. Any transport/parse/HTTP failure produces the
     * verified seed with [OrcaRouterCatalogResult.degraded] set, never an exception.
     */
    fun fetch(capability: OrcaRouterCapability?, apiKey: String?): OrcaRouterCatalogResult {
        val key = apiKey?.trim()
        if (key.isNullOrEmpty()) {
            return degraded("no API key configured for model discovery")
        }

        val url = buildUrl(capability)
        val request = try {
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds.coerceIn(1, 60)))
                .header("Authorization", "Bearer $key")
                .header("Accept", "application/json")
                .GET()
                .build()
        } catch (e: Exception) {
            return degraded("invalid catalog URL")
        }

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() != 200) {
                response.body().close()
                return degraded("model catalog request was rejected (HTTP ${response.statusCode()})")
            }
            val body = response.body().use { readBounded(it) }
                ?: return degraded("model catalog response was too large")

            val models = parseModels(body)
            if (models.isEmpty()) {
                return degraded("model catalog returned no usable entries")
            }
            OrcaRouterCatalogResult(
                models = models,
                source = OrcaRouterCatalogSource.LIVE,
                degraded = false
            )
        } catch (e: Exception) {
            // Never surface the request object or the key; only a fixed diagnostic.
            degraded("model catalog request failed (${e.javaClass.simpleName})")
        }
    }

    fun fetchChat(apiKey: String?): OrcaRouterCatalogResult = fetch(OrcaRouterCapability.CHAT, apiKey)

    private fun buildUrl(capability: OrcaRouterCapability?): String {
        val base = baseUrl.trimEnd('/')
        return if (capability == null) "$base/models" else "$base/models?capability=${capability.queryValue}"
    }

    private fun readBounded(stream: java.io.InputStream): String? {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_RESPONSE_BYTES) {
                return null
            }
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8)
    }

    private fun degraded(note: String) =
        OrcaRouterCatalogResult(
            models = verifiedSeed(),
            source = OrcaRouterCatalogSource.SEED,
            degraded = true,
            note = note
        )

    /**
     * Parses either `{"data":[...]}` or a bare array, accepting snake_case and camelCase
     * metadata variants. Entries without an id or without declared endpoints are dropped:
     * an undeclared capability is never assumed.
     */
    fun parseModels(json: String): List<OrcaRouterModel> {
        val mapper = ObjectMapper()
        val root = try {
            mapper.readTree(json)
        } catch (e: Exception) {
            return emptyList()
        }
        val array = when {
            root.isArray -> root
            root.isObject && root.has("data") && root.get("data").isArray -> root.get("data")
            else -> return emptyList()
        }

        val models = mutableListOf<OrcaRouterModel>()
        for (node in array) {
            if (models.size >= MAX_MODELS) break
            if (!node.isObject) continue
            val id = node.get("id")?.asText()?.trim()
            if (id.isNullOrEmpty()) continue

            val endpoints = stringSet(node, "supported_endpoint_types", "supportedEndpointTypes", "endpoint_types")
            val modalities: Set<String> = run {
                val architecture = node.get("architecture")
                if (architecture != null && architecture.isObject) {
                    stringSet(architecture, "input_modalities", "inputModalities")
                } else {
                    stringSet(node, "input_modalities", "inputModalities")
                }
            }.map { it.lowercase() }.toSet()

            val context = intOrNull(node, "context_length", "contextLength")
                ?: node.get("top_provider")?.takeIf { it.isObject }?.let { intOrNull(it, "context_length", "contextLength") }

            models.add(
                OrcaRouterModel(
                    id = id,
                    endpointTypes = endpoints,
                    inputModalities = modalities,
                    contextLength = context,
                    reasoningEfforts = reasoningEfforts(node)
                )
            )
        }
        return models
    }

    private fun stringSet(node: JsonNode, vararg names: String): Set<String> {
        for (name in names) {
            val value = node.get(name) ?: continue
            if (value.isArray) {
                return value.mapNotNull { it.asText()?.trim()?.takeIf { t -> t.isNotEmpty() } }
                    .map { it.lowercase() }
                    .toSet()
            }
        }
        return emptySet()
    }

    private fun intOrNull(node: JsonNode, vararg names: String): Int? {
        for (name in names) {
            val value = node.get(name) ?: continue
            if (value.isInt && value.asInt() > 0) {
                return value.asInt()
            }
        }
        return null
    }

    private fun reasoningEfforts(node: JsonNode): List<String> {
        val direct = node.get("reasoning_efforts") ?: node.get("reasoningEfforts")
        if (direct != null && direct.isArray) {
            return direct.mapNotNull { it.asText()?.trim()?.takeIf { t -> t.isNotEmpty() } }
        }
        val reasoning = node.get("reasoning")
        if (reasoning != null && reasoning.isObject) {
            val efforts = reasoning.get("efforts")
            if (efforts != null && efforts.isArray) {
                return efforts.mapNotNull { it.asText()?.trim()?.takeIf { t -> t.isNotEmpty() } }
            }
        }
        return emptyList()
    }

}

object OrcaRouterDefaults {
    /**
     * The OrcaRouter cyber-tuned model. It is the documented recommendation for this tool;
     * accounts without access to it fall back to the general router model.
     */
    const val DEFAULT_MODEL = "orcacyber-zero"
    const val FALLBACK_MODEL = "orcarouter/fusion"
}

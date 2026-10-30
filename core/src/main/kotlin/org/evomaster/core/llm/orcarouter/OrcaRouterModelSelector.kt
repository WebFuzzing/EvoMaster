package org.evomaster.core.llm.orcarouter

/**
 * Turns a catalog result into the model EvoMaster will actually use, and renders the list a
 * user picks from. Model ids are never accepted from free text without a catalog check:
 * a value that is not in the compatible (chat) list fails closed with the available options.
 */
object OrcaRouterModelSelector {

    private const val MAX_AVAILABLE_IN_ERROR = 25

    fun chatModels(catalog: OrcaRouterCatalogResult): List<OrcaRouterModel> =
        OrcaRouterCatalogClient.filterChat(catalog.models)

    /**
     * Resolves the requested model id against the chat-capable catalog.
     *
     * @throws IllegalArgumentException when the requested id is not compatible/known.
     */
    fun selectChatModel(catalog: OrcaRouterCatalogResult, requested: String?): OrcaRouterModel {
        val chat = chatModels(catalog)
        if (chat.isEmpty()) {
            throw IllegalArgumentException(
                "The OrcaRouter catalog contains no chat-capable models" +
                        (catalog.note?.let { " ($it)" } ?: "")
            )
        }

        val wanted = requested?.trim()?.takeIf { it.isNotEmpty() }
        if (wanted == null) {
            return chat.firstOrNull { it.id == OrcaRouterDefaults.DEFAULT_MODEL }
                ?: chat.firstOrNull { it.id == OrcaRouterDefaults.FALLBACK_MODEL }
                ?: chat.first()
        }

        chat.firstOrNull { it.id == wanted }?.let { return it }

        val available = chat.take(MAX_AVAILABLE_IN_ERROR).joinToString(", ") { it.id }
        val suffix = if (chat.size > MAX_AVAILABLE_IN_ERROR) ", …" else ""
        val source = if (catalog.degraded) {
            "Live catalog discovery is unavailable, so only the verified fallback list can be used."
        } else {
            "The current OrcaRouter catalog is authoritative."
        }
        throw IllegalArgumentException(
            "Model '$wanted' is not a chat-capable model in the current OrcaRouter catalog. " +
                    "$source Available models: $available$suffix. " +
                    "Run with --llmListModels true to inspect the list."
        )
    }

    /**
     * Human-readable rendering of the catalog, used by the `--llmListModels` command.
     */
    fun render(catalog: OrcaRouterCatalogResult): String {
        val chat = chatModels(catalog)
        val builder = StringBuilder()
        val header = if (catalog.degraded) {
            "OrcaRouter models — verified fallback catalog (live discovery unavailable: ${catalog.note})"
        } else {
            "OrcaRouter models — live catalog"
        }
        builder.appendLine(header)
        builder.appendLine("Model ids are shown exactly as advertised; select one with --llmName.")
        builder.appendLine()
        if (chat.isEmpty()) {
            builder.appendLine("No chat-capable models were advertised.")
            return builder.toString()
        }
        val idWidth = chat.maxOf { it.id.length }.coerceAtLeast(24)
        for (model in chat) {
            val modalities = model.inputModalities.ifEmpty { setOf("undeclared") }.joinToString(",")
            val context = model.contextLength?.let { "  context=$it" } ?: ""
            val reasoning = model.reasoningEfforts.takeIf { it.isNotEmpty() }
                ?.let { "  reasoning=${it.joinToString("/")}" } ?: ""
            val recommended = when (model.id) {
                OrcaRouterDefaults.DEFAULT_MODEL -> "  [recommended]"
                OrcaRouterDefaults.FALLBACK_MODEL -> "  [fallback]"
                else -> ""
            }
            builder.appendLine(
                "  ${model.id.padEnd(idWidth)}  modalities=$modalities$context$reasoning$recommended"
            )
        }
        return builder.toString()
    }
}

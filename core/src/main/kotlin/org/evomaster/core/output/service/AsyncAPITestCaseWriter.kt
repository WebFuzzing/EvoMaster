package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import java.nio.file.Path
import java.util.UUID

/**
 * Test-case writer for AsyncAPI runs.
 *
 * For Java JUnit 5 output (the default), emits real Kafka producer /
 * consumer calls via [org.evomaster.test.utils.EMTestUtils.kafkaPublish]
 * and [org.evomaster.test.utils.EMTestUtils.kafkaAwaitReply].  The
 * helpers wrap kafka-clients so each generated test action stays one
 * line; users opt into AsyncAPI tests by pulling in
 * `org.apache.kafka:kafka-clients` (the helper raises a clear error
 * if it's missing at test runtime).
 *
 * Other languages still get the comment-only skeleton — Kotlin/JS/Python
 * emission is parked as follow-up.
 */
class AsyncAPITestCaseWriter : ApiTestCaseWriter() {

    @Inject
    private lateinit var emConfig: EMConfig

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        lines.addSingleCommentLine("AsyncAPI test case: ${test.name}")
    }

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testCaseName: String,
        testSuitePath: Path?
    ) {
        val actions = ind.individual.seeMainExecutableActions()
        if (actions.isEmpty()) {
            lines.addSingleCommentLine("(no AsyncAPI actions in this individual)")
            return
        }
        if (config.outputFormat.isJava()) {
            // Pin the broker URL into the test so it can be re-run without
            // re-running the EvoMaster search.  When --bbBrokerUrl is empty
            // (white-box runs read it from the EM Driver at search time),
            // fall back to the standard env var that most Kafka tests use.
            val broker = emConfig.bbBrokerUrl
            lines.add(
                "final String __evm_broker = !\"$broker\".isEmpty() ? \"$broker\" : " +
                        "java.util.Optional.ofNullable(System.getenv(\"KAFKA_BOOTSTRAP_SERVERS\")).orElse(\"localhost:9092\");"
            )
            lines.addEmpty(1)
        }
        // Map from pairId to the correlation id we synthesised for the matching
        // PUBLISH; SUBSCRIBE_REPLY actions read it so the consumer-side filter
        // matches the producer-side header value.
        val correlationByPair = mutableMapOf<String, String>()
        for ((i, action) in actions.withIndex()) {
            val result = ind.seeResult(action.getLocalId())
            val async = action as? AsyncAPIAction
            if (async != null && async.kind == AsyncAPIAction.Kind.PUBLISH) {
                correlationByPair[async.pairId] = "evm-${UUID.randomUUID()}"
            }
            if (result == null) {
                lines.addSingleCommentLine("Action #$i (${async?.kind ?: "?"}) — no result captured (action chain stopped early)")
                continue
            }
            addAsyncAPIAction(async, i, lines, result, correlationByPair)
        }
    }

    private fun addAsyncAPIAction(
        action: AsyncAPIAction?,
        index: Int,
        lines: Lines,
        result: ActionResult,
        correlationByPair: Map<String, String>
    ) {
        if (action == null) return

        lines.addSingleCommentLine(
            "Action #$index: ${action.kind} on '${action.channelAddress}' (operation '${action.operationName}')"
        )
        result.getResultValue("delivery")?.let { lines.addSingleCommentLine("delivery: $it") }
        result.getResultValue("reply")?.let { lines.addSingleCommentLine("reply:    $it") }
        result.getResultValue("schemaValid")?.let { lines.addSingleCommentLine("schema-valid: $it") }
        result.getResultValue("variant")?.let { lines.addSingleCommentLine("matched reply variant: $it") }

        if (!config.outputFormat.isJava()) {
            lines.addSingleCommentLine("TODO Kotlin/Python AsyncAPI emission not implemented yet")
            lines.addEmpty(1)
            return
        }

        when (action.kind) {
            AsyncAPIAction.Kind.PUBLISH -> emitPublish(action, index, lines, correlationByPair[action.pairId])
            AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> emitSubscribeReply(action, index, lines, correlationByPair[action.pairId])
        }
        lines.addEmpty(1)
    }

    private fun emitPublish(action: AsyncAPIAction, index: Int, lines: Lines, correlationId: String?) {
        val payloadGene = action.payloadParam()?.primaryGene()
        val payloadJson = payloadGene?.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null) ?: "{}"
        val key = action.keyParam()?.primaryGene()?.getValueAsRawString()
        val keyExpr = if (key == null) "null" else "\"${escapeJava(key)}\""

        val rendered = renderChannelAddress(action)
        val correlationHeader = action.correlationHeaderName

        lines.add("{")
        lines.indented {
            lines.add("java.util.Map<String, byte[]> __evm_headers = new java.util.LinkedHashMap<>();")
            if (correlationHeader != null && correlationId != null) {
                lines.add(
                    "__evm_headers.put(\"${escapeJava(correlationHeader)}\", " +
                            "\"$correlationId\".getBytes(java.nio.charset.StandardCharsets.UTF_8));"
                )
            }
            // User-defined headers (auth tokens, tenant ids, …).
            action.headersParam()?.primaryGene()?.let { headersGene ->
                if (headersGene is org.evomaster.core.search.gene.ObjectGene) {
                    headersGene.fields.forEach { field ->
                        val active = (field as? org.evomaster.core.search.gene.wrapper.OptionalGene)?.isActive ?: true
                        if (!active) return@forEach
                        val raw = if (field is org.evomaster.core.search.gene.wrapper.OptionalGene)
                            field.gene.getValueAsRawString() else field.getValueAsRawString()
                        lines.add(
                            "__evm_headers.put(\"${escapeJava(field.name)}\", " +
                                    "\"${escapeJava(raw)}\".getBytes(java.nio.charset.StandardCharsets.UTF_8));"
                        )
                    }
                }
            }
            lines.add(
                "org.evomaster.test.utils.EMTestUtils.kafkaPublish(__evm_broker, " +
                        "\"${escapeJava(rendered)}\", $keyExpr, " +
                        "\"${escapeJava(payloadJson)}\".getBytes(java.nio.charset.StandardCharsets.UTF_8), " +
                        "__evm_headers);"
            )
        }
        lines.add("}")
    }

    private fun emitSubscribeReply(action: AsyncAPIAction, index: Int, lines: Lines, correlationId: String?) {
        val rendered = renderChannelAddress(action)
        val correlationHeader = action.correlationHeaderName ?: ""
        val correlationValue = correlationId ?: ""
        val varName = "__evm_reply_$index"
        lines.add(
            "byte[] $varName = " +
                    "org.evomaster.test.utils.EMTestUtils.kafkaAwaitReply(__evm_broker, " +
                    "\"${escapeJava(rendered)}\", " +
                    "\"${escapeJava(correlationHeader)}\", \"${escapeJava(correlationValue)}\", 5000L);"
        )
        lines.add(
            "org.junit.jupiter.api.Assertions.assertNotNull($varName, " +
                    "\"AsyncAPI reply on ${escapeJava(rendered)} did not arrive within 5s\");"
        )
    }

    /**
     * Render the action's templated channel address (`tenants/{tenantId}/orders`)
     * using the captured channel-parameter gene values.  Mirrors
     * `AbstractAsyncAPIFitness.renderChannelAddress` — duplicated because the
     * writer doesn't carry a reference to the fitness instance.
     */
    private fun renderChannelAddress(action: AsyncAPIAction): String {
        val params = action.channelParams()
        if (params.isEmpty()) return action.channelAddress
        var rendered = action.channelAddress
        params.forEach { (name, param) ->
            rendered = rendered.replace("{$name}", param.primaryGene().getValueAsRawString())
        }
        return rendered
    }

    private fun escapeJava(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    override fun addActionLinesPerType(
        action: Action,
        index: Int,
        testCaseName: String,
        lines: Lines,
        result: ActionResult,
        testSuitePath: Path?,
        baseUrlOfSut: String
    ) {
        // No-op: handleActionCalls drives per-action emission directly so we
        // can sequence the synthesised correlation id across each pair.
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean = false
}

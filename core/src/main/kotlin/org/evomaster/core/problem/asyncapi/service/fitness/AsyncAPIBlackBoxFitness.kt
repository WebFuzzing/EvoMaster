package org.evomaster.core.problem.asyncapi.service.fitness

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.problem.asyncapi.broker.MessageBrokerClient
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIMessage
import org.evomaster.core.problem.asyncapi.schema.AsyncAPISchema
import org.evomaster.core.problem.asyncapi.service.sampler.AsyncAPISampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.evomaster.core.search.service.FitnessFunction

/**
 * Black-box fitness for AsyncAPI 3.0 SUTs.  All targets are derived purely
 * from the AsyncAPI schema — no SUT-specific oracle.  The fitness gradient
 * an EA climbs:
 *
 *  - delivery_ok / delivery_fail
 *  - reply_received / reply_timeout
 *  - reply_correlation_match (when the schema declares a correlationId)
 *  - reply_schema_valid / reply_schema_invalid
 *  - reply_variant:<variantName> when the reply payload uses oneOf/anyOf
 *
 * Fully strict JSON-Schema validation is out of scope for the starter slice;
 * the schema-validity check here is the cheap "does the reply parse and
 * satisfy each variant's required+enum constraints" approximation.
 */
class AsyncAPIBlackBoxFitness : FitnessFunction<AsyncAPIIndividual>() {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncAPIBlackBoxFitness::class.java)
        private const val DEFAULT_REPLY_TIMEOUT_MS = 5_000L
    }

    @Inject
    private lateinit var sampler: AsyncAPISampler

    @Inject
    private lateinit var broker: MessageBrokerClient

    private val mapper = ObjectMapper()

    override fun doCalculateCoverage(
        individual: AsyncAPIIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ): EvaluatedIndividual<AsyncAPIIndividual>? {

        val schema = sampler.parsedSchema
            ?: throw IllegalStateException("AsyncAPI schema not initialised; check sampler bootstrapping")

        try {
            broker.connect()
        } catch (e: Exception) {
            log.warn("Could not connect to broker {}: {}", configuration.bbBrokerUrl, e.message)
            return null
        }

        val fv = FitnessValue(individual.size().toDouble())
        val actionResults = mutableListOf<ActionResult>()
        val actions = individual.seeMainExecutableActions()

        // Map from pairId to the correlation id we used for the matching publish.
        val correlationByPair = mutableMapOf<String, String>()

        for ((index, action) in actions.withIndex()) {
            val result = ActionResult(action.getLocalId())
            actionResults.add(result)
            try {
                when (action.kind) {
                    AsyncAPIAction.Kind.PUBLISH -> handlePublish(action, schema, fv, correlationByPair, result)
                    AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> handleSubscribeReply(action, schema, fv, correlationByPair, result)
                }
            } catch (e: Exception) {
                log.warn("AsyncAPI action #{} ({}) failed: {}", index, action.getName(), e.message)
                result.setErrorMessage(e.message ?: e.javaClass.simpleName)
                result.stopping = true
            }
            if (result.stopping) break
        }

        return EvaluatedIndividual(
            fv,
            individual.copy() as AsyncAPIIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    private fun handlePublish(
        action: AsyncAPIAction,
        schema: AsyncAPISchema,
        fv: FitnessValue,
        correlationByPair: MutableMap<String, String>,
        result: ActionResult
    ) {
        val payloadGene = action.payloadParam()?.primaryGene()
        val payloadJson = payloadGene?.let {
            it.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)
        } ?: "{}"

        val correlationId = "evm-${UUID.randomUUID()}"
        correlationByPair[action.pairId] = correlationId

        val headers = mutableMapOf<String, ByteArray>()
        action.correlationHeaderName?.let { name ->
            headers[name] = correlationId.toByteArray(StandardCharsets.UTF_8)
        }

        val outcome = broker.publish(
            channel = action.channelAddress,
            key = null,
            headers = headers,
            payload = payloadJson.toByteArray(StandardCharsets.UTF_8)
        )

        when (outcome) {
            is MessageBrokerClient.PublishOutcome.Sent -> {
                coverLocal(fv, "DELIVERY_OK:${action.channelAddress}:${action.operationName}")
                result.addResultValue("delivery", "ok")
                result.addResultValue("correlationId", correlationId)
            }
            is MessageBrokerClient.PublishOutcome.Failed -> {
                coverLocal(fv, "DELIVERY_FAIL:${action.channelAddress}:${action.operationName}")
                result.addResultValue("delivery", "fail")
                result.setErrorMessage(outcome.reason)
                result.stopping = true
            }
        }
    }

    private fun handleSubscribeReply(
        action: AsyncAPIAction,
        schema: AsyncAPISchema,
        fv: FitnessValue,
        correlationByPair: Map<String, String>,
        result: ActionResult
    ) {
        val expectedCorrelation = correlationByPair[action.pairId]
        val headerName = action.correlationHeaderName

        val outcome = broker.awaitFirstMatching(
            channel = action.channelAddress,
            predicate = { headers ->
                if (headerName == null || expectedCorrelation == null) return@awaitFirstMatching true
                val received = headers[headerName]?.toString(StandardCharsets.UTF_8)
                received == expectedCorrelation
            },
            timeoutMs = DEFAULT_REPLY_TIMEOUT_MS
        )

        when (outcome) {
            is MessageBrokerClient.SubscribeOutcome.Received -> {
                coverLocal(fv, "REPLY_RECEIVED:${action.channelAddress}:${action.operationName}")
                if (expectedCorrelation != null) {
                    coverLocal(fv, "REPLY_CORRELATION_MATCH:${action.channelAddress}:${action.operationName}")
                }
                evaluateReplyPayload(action, schema, outcome.payload, fv, result)
                result.addResultValue("reply", "received")
            }
            is MessageBrokerClient.SubscribeOutcome.Timeout -> {
                coverLocal(fv, "REPLY_TIMEOUT:${action.channelAddress}:${action.operationName}")
                result.addResultValue("reply", "timeout")
            }
        }
    }

    private fun evaluateReplyPayload(
        action: AsyncAPIAction,
        schema: AsyncAPISchema,
        payloadBytes: ByteArray,
        fv: FitnessValue,
        result: ActionResult
    ) {
        val text = payloadBytes.toString(StandardCharsets.UTF_8)
        val parsed: JsonNode = try {
            mapper.readTree(text)
        } catch (e: Exception) {
            coverLocal(fv, "REPLY_SCHEMA_INVALID:${action.channelAddress}:${action.operationName}")
            result.addResultValue("schemaValid", "false")
            result.addResultValue("schemaError", "not-json")
            return
        }

        val message = schema.messages[action.messageId]
        val replyPayloadSchema = message?.let { resolvePayloadNode(it, schema) }
        if (replyPayloadSchema == null) {
            coverLocal(fv, "REPLY_SCHEMA_VALID:${action.channelAddress}:${action.operationName}")
            result.addResultValue("schemaValid", "unknown")
            return
        }

        val variantMatch = matchVariants(parsed, replyPayloadSchema, schema)
        if (variantMatch.anyValid) {
            coverLocal(fv, "REPLY_SCHEMA_VALID:${action.channelAddress}:${action.operationName}")
            result.addResultValue("schemaValid", "true")
            variantMatch.matchedVariantName?.let { variant ->
                coverLocal(fv, "REPLY_VARIANT:${variant}:${action.channelAddress}:${action.operationName}")
                result.addResultValue("variant", variant)
            }
        } else {
            coverLocal(fv, "REPLY_SCHEMA_INVALID:${action.channelAddress}:${action.operationName}")
            result.addResultValue("schemaValid", "false")
        }
    }

    private fun resolvePayloadNode(message: AsyncAPIMessage, schema: AsyncAPISchema): JsonNode? {
        return message.payloadInline ?: message.payloadSchemaRef?.let { schema.componentSchemas[it] }
    }

    private data class VariantMatch(val anyValid: Boolean, val matchedVariantName: String?)

    /**
     * Walks a JSON-Schema variant tree (oneOf/anyOf, otherwise single) and
     * checks whether [payload] satisfies at least one variant's required
     * fields and discriminator-style enum constraints.  This is intentionally
     * cheap; full JSON-Schema validation is a follow-up.
     */
    private fun matchVariants(payload: JsonNode, schemaNode: JsonNode, schema: AsyncAPISchema): VariantMatch {
        // oneOf / anyOf: each variant may be a $ref or inline schema.
        val variants: List<Pair<String, JsonNode>> = when {
            schemaNode.has("oneOf") -> resolveVariants(schemaNode.get("oneOf"), schema)
            schemaNode.has("anyOf") -> resolveVariants(schemaNode.get("anyOf"), schema)
            else -> listOf("default" to schemaNode)
        }

        var matchedName: String? = null
        for ((name, variant) in variants) {
            if (matchesShallow(payload, variant)) {
                matchedName = name
                break
            }
        }
        return VariantMatch(anyValid = matchedName != null, matchedVariantName = matchedName)
    }

    private fun resolveVariants(arrayNode: JsonNode, schema: AsyncAPISchema): List<Pair<String, JsonNode>> {
        if (!arrayNode.isArray) return emptyList()
        val out = mutableListOf<Pair<String, JsonNode>>()
        for (entry in arrayNode) {
            val ref = entry.get("\$ref")?.asText()
            if (ref != null) {
                val name = ref.substringAfterLast('/')
                val resolved = schema.componentSchemas[name] ?: continue
                out.add(name to resolved)
            } else {
                out.add(("variant_" + (out.size + 1)) to entry)
            }
        }
        return out
    }

    private fun matchesShallow(payload: JsonNode, variant: JsonNode): Boolean {
        if (!payload.isObject) return false
        val required = variant.get("required")?.takeIf { it.isArray }?.map { it.asText() } ?: emptyList()
        for (field in required) {
            if (!payload.has(field)) return false
        }
        // Honour enum/const constraints on each property where we can.
        val properties = variant.get("properties") ?: return true
        if (!properties.isObject) return true
        properties.fields().forEach { (k, propSchema) ->
            val value = payload.get(k) ?: return@forEach
            val enumNode = propSchema.get("enum") ?: return@forEach
            if (enumNode.isArray) {
                val allowed = enumNode.map { it.asText() }
                if (value.isTextual && value.asText() !in allowed) {
                    return false
                }
            }
        }
        return true
    }

    private fun coverLocal(fv: FitnessValue, descriptiveId: String) {
        val id = idMapper.handleLocalTarget("Local:$descriptiveId")
        fv.coverTarget(id)
    }
}

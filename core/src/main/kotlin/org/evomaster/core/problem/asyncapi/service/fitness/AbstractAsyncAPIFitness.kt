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
import org.evomaster.core.problem.enterprise.service.EnterpriseFitness
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Shared scaffolding for AsyncAPI fitness functions.
 *
 * Owns the broker round-trip and the schema-derivable fitness targets that
 * both black-box and white-box modes need:
 *
 *  - delivery_ok / delivery_fail
 *  - reply_received / reply_timeout
 *  - reply_correlation_match (when the schema declares a correlationId)
 *  - reply_schema_valid / reply_schema_invalid
 *  - reply_variant:&lt;variantName&gt; when the reply payload uses oneOf/anyOf
 *
 * Subclasses extend the loop via two open hooks:
 *
 *  - [beforeAction] — called once per action just before publish/subscribe.
 *    White-box overrides this to register the action with the EM Driver so
 *    bytecode coverage gets attributed correctly.
 *  - [afterIndividualEvaluated] — called once after the action loop with the
 *    full action result list.  White-box overrides this to pull coverage
 *    targets from the EM Driver.
 *
 * Schema-validity is the cheap "required + enum" approximation used in M4b;
 * full JSON-Schema validation is M6 work.
 */
abstract class AbstractAsyncAPIFitness : EnterpriseFitness<AsyncAPIIndividual>() {

    companion object {
        private val log = LoggerFactory.getLogger(AbstractAsyncAPIFitness::class.java)
        private const val DEFAULT_REPLY_TIMEOUT_MS = 5_000L
    }

    @Inject
    private lateinit var asyncSampler: AsyncAPISampler

    @Inject
    private lateinit var broker: MessageBrokerClient

    private val mapper = ObjectMapper()

    final override fun doCalculateCoverage(
        individual: AsyncAPIIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ): EvaluatedIndividual<AsyncAPIIndividual>? {

        val schema = asyncSampler.parsedSchema
            ?: throw IllegalStateException("AsyncAPI schema not initialised; check sampler bootstrapping")

        try {
            broker.connect()
        } catch (e: Exception) {
            log.warn("Could not connect to broker at runtime: {}", e.message)
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
                beforeAction(action, index)
                when (action.kind) {
                    AsyncAPIAction.Kind.PUBLISH -> handlePublish(action, fv, correlationByPair, result)
                    AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> handleSubscribeReply(action, schema, fv, correlationByPair, result)
                }
            } catch (e: Exception) {
                log.warn("AsyncAPI action #{} ({}) failed: {}", index, action.getName(), e.message)
                result.setErrorMessage(e.message ?: e.javaClass.simpleName)
                result.stopping = true
            }
            if (result.stopping) break
        }

        afterIndividualEvaluated(individual, fv, actionResults, targets, allTargets, fullyCovered, descriptiveIds)

        return EvaluatedIndividual(
            fv,
            individual.copy() as AsyncAPIIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    /**
     * Hook invoked once per action right before it's published or awaited.
     * Default: no-op (black-box).  White-box overrides to register the action
     * with the EM Driver so coverage is attributed to it.
     */
    protected open fun beforeAction(action: AsyncAPIAction, index: Int) {
        // intentionally empty
    }

    /**
     * Hook invoked once after every action has been processed.  Default:
     * no-op (black-box).  White-box overrides to pull coverage targets from
     * the EM Driver and merge them into [fv].
     */
    protected open fun afterIndividualEvaluated(
        individual: AsyncAPIIndividual,
        fv: FitnessValue,
        actionResults: List<ActionResult>,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ) {
        // intentionally empty
    }

    private fun handlePublish(
        action: AsyncAPIAction,
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

    private fun matchVariants(payload: JsonNode, schemaNode: JsonNode, schema: AsyncAPISchema): VariantMatch {
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

    protected fun coverLocal(fv: FitnessValue, descriptiveId: String) {
        val id = idMapper.handleLocalTarget("Local:$descriptiveId")
        fv.coverTarget(id)
    }
}

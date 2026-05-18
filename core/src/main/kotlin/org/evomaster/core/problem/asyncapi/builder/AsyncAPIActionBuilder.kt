package org.evomaster.core.problem.asyncapi.builder

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.param.AsyncAPIParam
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIChannel
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIMessage
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIOperation
import org.evomaster.core.problem.asyncapi.schema.AsyncAPISchema
import org.evomaster.core.problem.asyncapi.schema.JsonSchemaConverter
import org.evomaster.core.problem.asyncapi.schema.ReplyBinding
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.builder.JsonSchemaToGeneConverter
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Translates a parsed [AsyncAPISchema] into [AsyncAPIAction]s ready for sampling.
 *
 * The shared [JsonSchemaToGeneConverter] does all the JSON-Schema → Gene
 * heavy lifting; we just feed it converted Swagger [Schema] objects (built
 * from AsyncAPI message payloads via [JsonSchemaConverter]) plus an
 * [AsyncAPISchemaRefResolver] that knows how to look up component schemas
 * by `$ref`.  No synthetic OpenAPI document is constructed.
 *
 * Black-box only for now: the builder ignores reply correlation header
 * generation (the fitness layer fills in a per-evaluation UUID at publish time).
 */
class AsyncAPIActionBuilder(private val config: EMConfig) {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncAPIActionBuilder::class.java)
    }

    data class Built(
        /**
         * Action cluster (operation key → list of paired actions).  A simple
         * SEND operation produces one PUBLISH action; a request/reply
         * operation produces a PUBLISH followed by a SUBSCRIBE_REPLY sharing
         * the same `pairId`.
         */
        val operations: Map<String, List<AsyncAPIAction>>
    )

    fun build(schema: AsyncAPISchema): Built {

        val resolver = AsyncAPISchemaRefResolver(schema.componentSchemas)
        val converter = JsonSchemaToGeneConverter(
            resolver,
            JsonSchemaToGeneConverter.Options(config)
        )

        val out = LinkedHashMap<String, List<AsyncAPIAction>>()

        schema.operations.values.forEach { op ->
            try {
                buildForOperation(op, schema, converter).forEach { (key, actions) ->
                    out[key] = actions
                }
            } catch (e: Exception) {
                log.warn("Skipping AsyncAPI operation '{}': {}", op.name, e.message)
            }
        }

        return Built(out)
    }

    private fun buildForOperation(
        op: AsyncAPIOperation,
        schema: AsyncAPISchema,
        converter: JsonSchemaToGeneConverter
    ): Map<String, List<AsyncAPIAction>> {

        if (op.action != AsyncAPIOperation.Action.RECEIVE) {
            // SEND operations describe channels the SUT (= the application
            // defined by this AsyncAPI doc) publishes onto. From the testing
            // tool's perspective these are observation-only — the SUT
            // produces messages here as a side effect of its own work, and
            // we can't trigger them directly. Emit one SUBSCRIBE_OUTPUT
            // action per SEND operation, appended by the sampler at the
            // end of each individual.
            //
            // (Pre-M10 the SEND/RECEIVE polarity was inverted here — the
            // builder treated SEND as the engine-publishable channel.
            // That was inconsistent with AsyncAPI 3.0 §4.5: `action: send`
            // means "the application sends", i.e. the SUT publishes; and
            // `action: receive` means "the application receives", i.e. the
            // SUT consumes. The Meridian + Microcks real-schema validation
            // pass surfaced the inversion; this branch now follows the spec.)
            val channel = schema.channels[op.channelName]
                ?: throw SutProblemException("AsyncAPI operation '${op.name}' references missing channel '${op.channelName}'")
            val channelAddress = channel.address
                ?: throw SutProblemException("AsyncAPI channel '${channel.name}' has no address; cannot subscribe")
            val resolvedIds = resolveMessageIds(op, channel)
            if (resolvedIds.isEmpty()) {
                // Bare SEND channels (no declared message shape) can still
                // be listened to ("did anything arrive?"), so fall back to
                // an empty message-id list rather than rejecting the op.
                log.debug("SEND operation '{}' has no resolvable message ids; emitting bare SUBSCRIBE_OUTPUT", op.name)
            }
            val primaryId = resolvedIds.firstOrNull() ?: ""
            val alternativeIds = if (resolvedIds.size > 1) resolvedIds.drop(1) else emptyList()
            val action = AsyncAPIAction(
                operationName = op.name,
                channelAddress = channelAddress,
                channelName = op.channelName,
                kind = AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT,
                pairId = UUID.randomUUID().toString().substring(0, 8),
                messageId = primaryId,
                additionalReplyMessageIds = alternativeIds,
                parameters = mutableListOf(),
                replyBinding = null,
                correlationHeaderName = null
            )
            return mapOf(op.name to listOf(action))
        }
        // Falls through to the SEND-as-PUBLISH path → flipped: RECEIVE-as-PUBLISH.
        // The SUT receives on this channel, so the engine publishes here to
        // trigger SUT consumer code. Variable names retain "publish*" prefix
        // since the engine's behaviour is unchanged; only the schema label
        // that drives this branch flipped.

        val channel = schema.channels[op.channelName]
            ?: throw SutProblemException("AsyncAPI operation '${op.name}' references missing channel '${op.channelName}'")
        val channelAddress = channel.address
            ?: throw SutProblemException("AsyncAPI channel '${channel.name}' has no address; cannot publish")

        val resolvedMessageIds = resolveMessageIds(op, channel)
        if (resolvedMessageIds.isEmpty()) {
            throw SutProblemException("AsyncAPI operation '${op.name}' has no resolvable message id")
        }

        val out = LinkedHashMap<String, List<AsyncAPIAction>>()
        resolvedMessageIds.forEach { publishMessageId ->
            // Operation-key disambiguates per message type so the sampler can
            // pick any of them with uniform probability and the structure
            // mutator keeps each variant's PUBLISH/SUBSCRIBE_REPLY pair intact.
            val key = if (resolvedMessageIds.size == 1) op.name else "${op.name}#$publishMessageId"
            out[key] = buildSingleVariantActions(op, channel, channelAddress, publishMessageId, schema, converter)
        }
        return out
    }

    private fun buildSingleVariantActions(
        op: AsyncAPIOperation,
        channel: AsyncAPIChannel,
        channelAddress: String,
        publishMessageId: String,
        schema: AsyncAPISchema,
        converter: JsonSchemaToGeneConverter
    ): List<AsyncAPIAction> {

        val publishMessage = schema.messages[publishMessageId]
            ?: throw SutProblemException("AsyncAPI operation '${op.name}' references unknown component message '$publishMessageId'")

        val pairId = UUID.randomUUID().toString().substring(0, 8)
        val publishAction = buildAction(
            kind = AsyncAPIAction.Kind.PUBLISH,
            op = op,
            channelAddress = channelAddress,
            channelName = op.channelName,
            channelParameters = channel.parameters,
            message = publishMessage,
            pairId = pairId,
            schema = schema,
            converter = converter,
            replyBinding = op.reply,
            correlationLocation = publishMessage.correlationLocation
        )

        val out = mutableListOf(publishAction)

        op.reply?.let { reply ->
            val replyChannelName = reply.channelNames.firstOrNull()
                ?: throw SutProblemException("AsyncAPI reply on '${op.name}' has no reply channel")
            val replyChannel = schema.channels[replyChannelName]
                ?: throw SutProblemException("AsyncAPI reply on '${op.name}' references missing channel '$replyChannelName'")
            val replyAddress = replyChannel.address
                ?: throw SutProblemException("AsyncAPI reply channel '${replyChannel.name}' has no address")
            val resolvedReplyIds = resolveReplyMessageIds(reply, replyChannel)
            if (resolvedReplyIds.isEmpty()) {
                throw SutProblemException("AsyncAPI reply on '${op.name}' has no resolvable message id")
            }
            val primaryReplyId = resolvedReplyIds.first()
            val alternativeReplyIds = resolvedReplyIds.drop(1)
            val replyMessage = schema.messages[primaryReplyId]
                ?: throw SutProblemException("AsyncAPI reply on '${op.name}' references unknown component message '$primaryReplyId'")

            out.add(
                buildAction(
                    kind = AsyncAPIAction.Kind.SUBSCRIBE_REPLY,
                    op = op,
                    channelAddress = replyAddress,
                    channelName = replyChannelName,
                    channelParameters = replyChannel.parameters,
                    message = replyMessage,
                    pairId = pairId,
                    schema = schema,
                    converter = converter,
                    replyBinding = null,
                    correlationLocation = replyMessage.correlationLocation,
                    additionalReplyMessageIds = alternativeReplyIds
                )
            )
        }

        return out
    }

    private fun buildAction(
        kind: AsyncAPIAction.Kind,
        op: AsyncAPIOperation,
        channelAddress: String,
        channelName: String,
        channelParameters: Map<String, com.fasterxml.jackson.databind.JsonNode>,
        message: AsyncAPIMessage,
        pairId: String,
        schema: AsyncAPISchema,
        converter: JsonSchemaToGeneConverter,
        replyBinding: ReplyBinding?,
        correlationLocation: String?,
        additionalReplyMessageIds: List<String> = emptyList()
    ): AsyncAPIAction {

        val swaggerSchema = resolvePayloadSchema(message, schema)
        val messages = mutableListOf<String>()
        val gene = converter.getGene(
            name = AsyncAPIAction.PAYLOAD_PARAM,
            schema = swaggerSchema,
            referenceClassDef = null,
            messages = messages
        )
        if (messages.isNotEmpty() && log.isDebugEnabled) {
            messages.forEach { log.debug("AsyncAPI gene-build message: {}", it) }
        }

        val unwrapped = if (gene is NullableGene) gene.gene else gene
        val payloadGene = unwrapped as? ObjectGene
            ?: ObjectGene(AsyncAPIAction.PAYLOAD_PARAM, listOf(gene))
        val payloadParam = AsyncAPIParam(AsyncAPIAction.PAYLOAD_PARAM, payloadGene)

        val params = mutableListOf<AsyncAPIParam>(payloadParam)

        // AsyncAPI 3.0 channel parameters: the address may carry `{...}`
        // placeholders (e.g. `tenants/{tenantId}/orders`) and the channel
        // declares each placeholder under `parameters.<name>` with its own
        // schema.  Build a gene per parameter so the EA mutates them
        // independently of the payload; the placeholder is rendered to a
        // concrete topic at publish time by reading the gene's value.
        // Restrict to placeholders that actually appear in the address —
        // the schema sometimes lists more parameters than the address uses.
        val placeholders = extractPlaceholders(channelAddress)
        channelParameters.forEach { (paramName, paramNode) ->
            if (paramName !in placeholders) return@forEach
            // AsyncAPI 3.0 parameter objects nest their JSON Schema under
            // `schema:`.  Tolerate shorthand (a bare schema with no `schema`
            // wrapper) for backwards compatibility with hand-written schemas.
            val schemaNode = paramNode.get("schema") ?: paramNode
            val swaggerSchema = JsonSchemaConverter.convert(schemaNode)
            val paramMsgs = mutableListOf<String>()
            val paramGene = converter.getGene(
                name = paramName,
                schema = swaggerSchema,
                referenceClassDef = null,
                messages = paramMsgs
            )
            if (paramMsgs.isNotEmpty() && log.isDebugEnabled) {
                paramMsgs.forEach { log.debug("AsyncAPI channel-parameter gene-build message: {}", it) }
            }
            val unwrappedParam = if (paramGene is NullableGene) paramGene.gene else paramGene
            params += AsyncAPIParam(AsyncAPIAction.CHANNEL_PARAM_PREFIX + paramName, unwrappedParam)
        }
        // AsyncAPI 3.0 protocol bindings — Kafka message key.  Mutating it
        // changes which partition the broker picks, which is the schema-
        // derivable signal that exercises partition-aware SUT logic.  The
        // existing enum/field/boundary target builders pick up the gene
        // automatically once it's threaded into the action's parameter list.
        message.kafkaKeyInline?.let { keyNode ->
            val keySwagger = JsonSchemaConverter.convert(keyNode)
            val keyMsgs = mutableListOf<String>()
            val keyGene = converter.getGene(
                name = AsyncAPIAction.KEY_PARAM,
                schema = keySwagger,
                referenceClassDef = null,
                messages = keyMsgs
            )
            if (keyMsgs.isNotEmpty() && log.isDebugEnabled) {
                keyMsgs.forEach { log.debug("AsyncAPI Kafka-key gene-build message: {}", it) }
            }
            val keyUnwrapped = if (keyGene is NullableGene) keyGene.gene else keyGene
            params += AsyncAPIParam(AsyncAPIAction.KEY_PARAM, keyUnwrapped)
        }

        // AsyncAPI 3.0 lets messages declare a `headers` schema independent
        // of the payload — typically auth tokens, tenant ids, tracing ids.
        // Build a separate gene tree for it so the EA can mutate the
        // headers without disturbing the payload, and the existing
        // enum/field/boundary target builders pick them up automatically.
        resolveHeadersSchema(message, schema)?.let { headersSwagger ->
            val headersMsgs = mutableListOf<String>()
            val headersGene = converter.getGene(
                name = AsyncAPIAction.HEADERS_PARAM,
                schema = headersSwagger,
                referenceClassDef = null,
                messages = headersMsgs
            )
            if (headersMsgs.isNotEmpty() && log.isDebugEnabled) {
                headersMsgs.forEach { log.debug("AsyncAPI headers gene-build message: {}", it) }
            }
            val headersUnwrapped = if (headersGene is NullableGene) headersGene.gene else headersGene
            val headersObj = headersUnwrapped as? ObjectGene
                ?: ObjectGene(AsyncAPIAction.HEADERS_PARAM, listOf(headersGene))
            params += AsyncAPIParam(AsyncAPIAction.HEADERS_PARAM, headersObj)
        }

        val replyFieldAssertions = if (kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY) {
            extractReplyFieldAssertions(message, schema)
        } else {
            emptyList()
        }

        return AsyncAPIAction(
            operationName = op.name,
            channelAddress = channelAddress,
            channelName = channelName,
            kind = kind,
            pairId = pairId,
            messageId = message.id,
            additionalReplyMessageIds = additionalReplyMessageIds,
            parameters = params.toMutableList(),
            replyBinding = replyBinding,
            correlationHeaderName = correlationLocation?.let(::extractHeaderName),
            replyFieldAssertions = replyFieldAssertions
        )
    }

    /**
     * Walk the reply variant's declared properties and pre-compute the
     * assertion specs the writer should emit on the captured reply payload.
     * Captures `required` + `enum` for the starter (PR5); bounds / length /
     * format are tracked as a follow-up that doesn't change [ReplyFieldAssertion].
     */
    private fun extractReplyFieldAssertions(
        message: AsyncAPIMessage,
        schema: AsyncAPISchema
    ): List<org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion> {
        val node = message.payloadInline ?: message.payloadSchemaRef?.let { schema.componentSchemas[it] }
            ?: return emptyList()
        if (!node.isObject) return emptyList()
        // Variant resolution: for oneOf / anyOf, we'd produce per-variant
        // assertions in a follow-up; for now, only flat schemas are scanned
        // (the fitness layer already covers the oneOf case via REPLY_FIELD_*
        // targets keyed by matched variant name).
        val properties = node.get("properties")?.takeIf { it.isObject } ?: return emptyList()
        val required = node.get("required")?.takeIf { it.isArray }
            ?.map { it.asText() }?.toSet() ?: emptySet()

        val out = mutableListOf<org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion>()
        properties.fields().forEach { (name, prop) ->
            if (name in required) {
                out.add(
                    org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion(
                        path = name,
                        kind = org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion.Kind.REQUIRED
                    )
                )
            }
            val enumNode = prop.get("enum")
            if (enumNode != null && enumNode.isArray) {
                val values = enumNode.mapNotNull { if (it.isTextual) it.asText() else null }
                if (values.isNotEmpty()) {
                    out.add(
                        org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion(
                            path = name,
                            kind = org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion.Kind.ENUM,
                            expectedValues = values
                        )
                    )
                }
            }
        }
        return out
    }

    private fun resolveHeadersSchema(message: AsyncAPIMessage, schema: AsyncAPISchema): Schema<*>? {
        val node = when {
            message.headersInline != null -> message.headersInline
            message.headersSchemaRef != null -> schema.componentSchemas[message.headersSchemaRef]
                ?: throw SutProblemException(
                    "AsyncAPI message '${message.id}' references missing headers schema '${message.headersSchemaRef}'"
                )
            else -> return null
        }
        return JsonSchemaConverter.convert(node)
    }

    private fun resolvePayloadSchema(message: AsyncAPIMessage, schema: AsyncAPISchema): Schema<*> {
        val node = when {
            message.payloadInline != null -> message.payloadInline
            message.payloadSchemaRef != null -> schema.componentSchemas[message.payloadSchemaRef]
                ?: throw SutProblemException(
                    "AsyncAPI message '${message.id}' references missing component schema '${message.payloadSchemaRef}'"
                )
            else -> throw SutProblemException("AsyncAPI message '${message.id}' has no payload")
        }
        return JsonSchemaConverter.convert(node)
    }

    private fun pickFirstMessageId(op: AsyncAPIOperation, channel: AsyncAPIChannel): String? {
        val opIds = op.messageIds.toSet()
        return channel.messageIds.firstOrNull { it in opIds || opIds.isEmpty() }
    }

    /**
     * Every message id the operation can dispatch on this channel.  An
     * AsyncAPI 3.0 operation's `messages:` array enumerates which of the
     * channel's declared messages it actually picks from; when omitted, the
     * operation can dispatch any of them.
     */
    private fun resolveMessageIds(op: AsyncAPIOperation, channel: AsyncAPIChannel): List<String> {
        val opIds = op.messageIds.toSet()
        return channel.messageIds.filter { it in opIds || opIds.isEmpty() }
    }

    /**
     * All reply message ids declared on the operation that resolve to messages
     * the channel actually carries.  When `reply.messages: [...]` enumerates
     * several variants, every one becomes a candidate the fitness layer can
     * match against.
     */
    private fun resolveReplyMessageIds(reply: ReplyBinding, channel: AsyncAPIChannel): List<String> {
        val replyIds = reply.messageIds.toSet()
        return channel.messageIds.filter { it in replyIds || replyIds.isEmpty() }
    }

    private fun pickFirstReplyMessageId(reply: ReplyBinding, channel: AsyncAPIChannel): String? {
        val opIds = reply.messageIds.toSet()
        return channel.messageIds.firstOrNull { it in opIds || opIds.isEmpty() }
    }

    private fun extractPlaceholders(address: String): Set<String> {
        // Match {paramName} segments — AsyncAPI 3.0 address templating uses
        // the same brace syntax as RFC 6570 URI templates.
        val regex = Regex("""\{([^{}/]+)\}""")
        return regex.findAll(address).map { it.groupValues[1] }.toSet()
    }

    private fun extractHeaderName(location: String): String? {
        // AsyncAPI 3.0 correlationId.location: "$message.header#/<headerName>"
        val marker = "header#/"
        val idx = location.indexOf(marker)
        if (idx < 0) return null
        return location.substring(idx + marker.length).trim().takeIf { it.isNotEmpty() }
    }
}

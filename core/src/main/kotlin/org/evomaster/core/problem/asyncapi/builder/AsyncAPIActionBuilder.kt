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
                out[op.name] = buildForOperation(op, schema, converter)
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
    ): List<AsyncAPIAction> {

        if (op.action != AsyncAPIOperation.Action.SEND) {
            // Black-box can only directly trigger SUTs via SEND-from-our-side
            // (the SUT consumes); RECEIVE operations describe SUT producers we
            // can only observe, which the M4 fitness layer does not yet model
            // as standalone actions — they only show up via reply bindings.
            return emptyList()
        }

        val channel = schema.channels[op.channelName]
            ?: throw SutProblemException("AsyncAPI operation '${op.name}' references missing channel '${op.channelName}'")
        val channelAddress = channel.address
            ?: throw SutProblemException("AsyncAPI channel '${channel.name}' has no address; cannot publish")

        val publishMessageId = pickFirstMessageId(op, channel)
            ?: throw SutProblemException("AsyncAPI operation '${op.name}' has no resolvable message id")
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
            val replyMessageId = pickFirstReplyMessageId(reply, replyChannel)
                ?: throw SutProblemException("AsyncAPI reply on '${op.name}' has no resolvable message id")
            val replyMessage = schema.messages[replyMessageId]
                ?: throw SutProblemException("AsyncAPI reply on '${op.name}' references unknown component message '$replyMessageId'")

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
                    correlationLocation = replyMessage.correlationLocation
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
        correlationLocation: String?
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
            val swaggerSchema = JsonSchemaConverter.convert(paramNode)
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

        return AsyncAPIAction(
            operationName = op.name,
            channelAddress = channelAddress,
            channelName = channelName,
            kind = kind,
            pairId = pairId,
            messageId = message.id,
            parameters = params.toMutableList(),
            replyBinding = replyBinding,
            correlationHeaderName = correlationLocation?.let(::extractHeaderName)
        )
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

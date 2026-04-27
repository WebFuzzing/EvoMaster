package org.evomaster.core.problem.asyncapi.builder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.param.AsyncAPIParam
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIMessage
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIOperation
import org.evomaster.core.problem.asyncapi.schema.AsyncAPISchema
import org.evomaster.core.problem.asyncapi.schema.JsonSchemaConverter
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Translates a parsed [AsyncAPISchema] into [AsyncAPIAction]s ready for sampling.
 *
 * The trick that lets us avoid duplicating REST's ~800 LOC schema-to-gene
 * pipeline: we synthesise a tiny OpenAPI document whose `components.schemas`
 * reproduces the AsyncAPI component schemas, hand it to the swagger parser,
 * and call [RestActionBuilderV3.getGene] (recently bumped to `internal` for
 * exactly this reuse).  Inline payloads are converted to swagger
 * [io.swagger.v3.oas.models.media.Schema] via [JsonSchemaConverter] and fed in
 * the same way; `$ref`s resolve through the synthetic OpenAPI components.
 *
 * Black-box only for now: the builder ignores reply correlation header
 * generation (the fitness layer fills in a per-evaluation UUID at publish time).
 */
class AsyncAPIActionBuilder(private val config: EMConfig) {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncAPIActionBuilder::class.java)
    }

    private val yamlMapper = ObjectMapper(YAMLFactory())

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

        val syntheticOpenApi = synthesiseOpenAPI(schema)
        val syntheticSchema = OpenApiAccess.parseOpenApi(
            syntheticOpenApi,
            SchemaLocation("asyncapi://synthetic", SchemaLocationType.RESOURCE)
        )
        val restSchema = RestSchema(syntheticSchema)
        val options = RestActionBuilderV3.Options(config)
        val messages = mutableListOf<String>()

        val out = LinkedHashMap<String, List<AsyncAPIAction>>()

        schema.operations.values.forEach { op ->
            try {
                out[op.name] = buildForOperation(op, schema, restSchema, syntheticSchema, options, messages)
            } catch (e: Exception) {
                log.warn("Skipping AsyncAPI operation '{}': {}", op.name, e.message)
            }
        }

        if (messages.isNotEmpty() && log.isDebugEnabled) {
            messages.forEach { log.debug("AsyncAPI gene-build message: {}", it) }
        }

        return Built(out)
    }

    private fun buildForOperation(
        op: AsyncAPIOperation,
        schema: AsyncAPISchema,
        restSchema: RestSchema,
        syntheticSchema: SchemaOpenAPI,
        options: RestActionBuilderV3.Options,
        messages: MutableList<String>
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
            message = publishMessage,
            pairId = pairId,
            schema = schema,
            restSchema = restSchema,
            syntheticSchema = syntheticSchema,
            options = options,
            messages = messages,
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
                    message = replyMessage,
                    pairId = pairId,
                    schema = schema,
                    restSchema = restSchema,
                    syntheticSchema = syntheticSchema,
                    options = options,
                    messages = messages,
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
        message: AsyncAPIMessage,
        pairId: String,
        schema: AsyncAPISchema,
        restSchema: RestSchema,
        syntheticSchema: SchemaOpenAPI,
        options: RestActionBuilderV3.Options,
        messages: MutableList<String>,
        replyBinding: org.evomaster.core.problem.asyncapi.schema.ReplyBinding?,
        correlationLocation: String?
    ): AsyncAPIAction {

        val swaggerSchema = resolvePayloadSchema(message, schema)
        val gene = RestActionBuilderV3.getGene(
            name = AsyncAPIAction.PAYLOAD_PARAM,
            schema = swaggerSchema,
            schemaHolder = restSchema,
            currentSchema = syntheticSchema,
            referenceClassDef = null,
            options = options,
            messages = messages
        )
        val unwrapped = if (gene is NullableGene) gene.gene else gene
        val payloadGene = unwrapped as? ObjectGene
            ?: ObjectGene(AsyncAPIAction.PAYLOAD_PARAM, listOf(gene))
        val payloadParam = AsyncAPIParam(AsyncAPIAction.PAYLOAD_PARAM, payloadGene)

        return AsyncAPIAction(
            operationName = op.name,
            channelAddress = channelAddress,
            channelName = channelName,
            kind = kind,
            pairId = pairId,
            messageId = message.id,
            parameters = mutableListOf(payloadParam),
            replyBinding = replyBinding,
            correlationHeaderName = correlationLocation?.let(::extractHeaderName)
        )
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

    private fun pickFirstMessageId(op: AsyncAPIOperation, channel: org.evomaster.core.problem.asyncapi.schema.AsyncAPIChannel): String? {
        // Operation-level messages reference channel-level message keys, but
        // channels carry component-level message ids directly.  Take the
        // intersection ordered by op-level appearance, falling back to channel.
        val opIds = op.messageIds.toSet()
        return channel.messageIds.firstOrNull { it in opIds || opIds.isEmpty() }
    }

    private fun pickFirstReplyMessageId(
        reply: org.evomaster.core.problem.asyncapi.schema.ReplyBinding,
        channel: org.evomaster.core.problem.asyncapi.schema.AsyncAPIChannel
    ): String? {
        val opIds = reply.messageIds.toSet()
        return channel.messageIds.firstOrNull { it in opIds || opIds.isEmpty() }
    }

    private fun extractHeaderName(location: String): String? {
        // AsyncAPI 3.0 correlationId.location: "$message.header#/<headerName>"
        val marker = "header#/"
        val idx = location.indexOf(marker)
        if (idx < 0) return null
        return location.substring(idx + marker.length).trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Builds a tiny OpenAPI 3.0 YAML document whose `components.schemas`
     * mirrors the AsyncAPI document's, so swagger-parser can resolve `$ref`s
     * for free.  No paths are declared; `RestSchema.validate()` is **not**
     * called on the result because the synthetic doc has no operations.
     */
    private fun synthesiseOpenAPI(schema: AsyncAPISchema): String {
        val componentSchemas = schema.componentSchemas
        val openapiRoot = mutableMapOf<String, Any>(
            "openapi" to "3.0.3",
            "info" to mapOf("title" to "asyncapi-bridge", "version" to "1.0.0"),
            "paths" to emptyMap<String, Any>()
        )
        if (componentSchemas.isNotEmpty()) {
            openapiRoot["components"] = mapOf(
                "schemas" to componentSchemas
                    .mapValues { (_, v) -> yamlMapper.treeToValue(v, Any::class.java) }
            )
        }
        return yamlMapper.writeValueAsString(openapiRoot)
    }
}

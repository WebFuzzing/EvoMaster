package org.evomaster.core.problem.asyncapi.builder

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion
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
        /** Bound recursion so circular $refs / deep schemas don't loop. */
        private const val MAX_REPLY_ASSERTION_DEPTH = 5
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

        // SUBSCRIBE_OUTPUT actions reuse the same per-field assertion pipeline
        // as SUBSCRIBE_REPLY: the schema-derived facets (required / enum /
        // min / max / minLength / maxLength / format) are pre-computed and
        // applied to every message captured during the listen window. PUBLISH
        // actions get an empty list (the engine emits inputs, not outputs).
        val replyFieldAssertions = if (
            kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY ||
            kind == AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT
        ) {
            extractReplyFieldAssertions(message, schema)
        } else {
            emptyList()
        }
        // M11-PR6: also walk the `headers:` schema if declared, surfacing
        // per-header facets that the writer will emit against the
        // ReplyEnvelope.headers map at test runtime. Headers are typically
        // flat key-value pairs (not nested objects), so the same recursive
        // walker handles them with depth 0.
        val headerFieldAssertions = if (
            kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY ||
            kind == AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT
        ) {
            extractHeaderFieldAssertions(message, schema)
        } else {
            emptyList()
        }
        // M11-PR3 fix #9: when the payload schema is a oneOf with a
        // discriminator, also pre-compute the per-variant facet sets so
        // the writer can emit a runtime if/else-if chain on the
        // discriminator value (stronger than the intersection-only
        // fallback used by M11-PR2 fix G).
        val perVariantReplyAssertions = if (
            kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY ||
            kind == AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT
        ) {
            extractPerVariantReplyAssertions(message, schema)
        } else {
            null
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
            replyFieldAssertions = replyFieldAssertions,
            headerFieldAssertions = headerFieldAssertions,
            perVariantReplyAssertions = perVariantReplyAssertions
        )
    }

    /**
     * Pre-compute the (discriminator, per-variant facets) table used by the
     * writer's runtime-dispatch oneOf handling (M11-PR3 fix #9). Returns
     * null when the reply schema isn't a discriminated `oneOf` (or when the
     * discriminator declaration is incomplete).
     */
    private fun extractPerVariantReplyAssertions(
        message: AsyncAPIMessage,
        schema: AsyncAPISchema
    ): AsyncAPIAction.VariantReplyAssertions? {
        val node = message.payloadInline ?: message.payloadSchemaRef?.let { schema.componentSchemas[it] }
            ?: return null
        if (!node.isObject) return null
        val discriminator = node.get("discriminator")?.takeIf { it.isObject } ?: return null
        val discriminatorProp = discriminator.get("propertyName")?.takeIf { it.isTextual }?.asText() ?: return null
        // Composition keyword — try the three in priority order. oneOf is
        // the typical case; anyOf is treated the same way for dispatch.
        val variantsArr = listOf("oneOf", "anyOf", "allOf").firstNotNullOfOrNull { node.get(it) }
            ?.takeIf { it.isArray && it.size() > 0 } ?: return null

        val mapping = discriminator.get("mapping")?.takeIf { it.isObject }
        val byVariant = linkedMapOf<String, List<ReplyFieldAssertion>>()
        variantsArr.forEachIndexed { idx, v ->
            val resolved = resolveSchemaRef(v, schema) ?: return@forEachIndexed
            val name = if (mapping != null) {
                // Mapping is value->ref, reverse-lookup the ref to its value.
                val ref = v.get("\$ref")?.takeIf { it.isTextual }?.asText()
                mapping.fields().asSequence().firstOrNull {
                    it.value.isTextual && it.value.asText() == ref
                }?.key ?: ref?.substringAfterLast("/") ?: "variant_$idx"
            } else {
                v.get("\$ref")?.takeIf { it.isTextual }?.asText()?.substringAfterLast("/") ?: "variant_$idx"
            }
            val perVariantBatch = mutableListOf<ReplyFieldAssertion>()
            collectFieldAssertions(resolved, schema, pathPrefix = "", depth = 0, out = perVariantBatch)
            byVariant[name] = perVariantBatch
        }
        if (byVariant.isEmpty()) return null
        return AsyncAPIAction.VariantReplyAssertions(discriminatorProp, byVariant)
    }

    /**
     * Walk the reply variant's declared properties and pre-compute the
     * assertion specs the writer should emit on the captured reply payload.
     *
     * Covers the M9-PR5 starter facets (required + enum), the M11-PR1
     * extension (numeric bounds, string length, format) and the M11-PR2
     * additions:
     *  - **A** nested dotted paths (`a.b.c`) via recursion with depth cap.
     *  - **B** `pattern`, `const`, `multipleOf`, `minItems`, `maxItems`,
     *          `uniqueItems`.
     *  - **G** intersection-of-facets for `oneOf` / `anyOf` reply unions —
     *          only facets that hold across every variant are emitted, so
     *          unknown-variant runtime payloads still pass the assertions
     *          that genuinely apply to the union.
     *  - **H** discriminator surfacing — if the declared composition carries
     *          a `discriminator`, the discriminator property's value is
     *          asserted to be one of the declared variant names.
     *
     * Recursion is depth-capped (default 5) to defuse circular-ref schemas
     * without external-ref work.
     */
    private fun extractReplyFieldAssertions(
        message: AsyncAPIMessage,
        schema: AsyncAPISchema
    ): List<ReplyFieldAssertion> {
        val node = message.payloadInline ?: message.payloadSchemaRef?.let { schema.componentSchemas[it] }
            ?: return emptyList()
        val out = mutableListOf<ReplyFieldAssertion>()
        collectFieldAssertions(node, schema, pathPrefix = "", depth = 0, out = out)
        return out
    }

    /**
     * M11-PR6: walk the message's `headers:` schema (inline or referenced) and
     * emit the same per-field facet vocabulary as for the payload. Headers are
     * usually flat key/value pairs (auth tokens, tenant ids, tracing ids), so
     * [collectFieldAssertions] just visits one level, but the depth-capped
     * recursion still handles the rare nested object case for free.
     */
    private fun extractHeaderFieldAssertions(
        message: AsyncAPIMessage,
        schema: AsyncAPISchema
    ): List<ReplyFieldAssertion> {
        val node = message.headersInline
            ?: message.headersSchemaRef?.let { schema.componentSchemas[it] }
            ?: return emptyList()
        val out = mutableListOf<ReplyFieldAssertion>()
        collectFieldAssertions(node, schema, pathPrefix = "", depth = 0, out = out)
        return out
    }

    private fun collectFieldAssertions(
        node: com.fasterxml.jackson.databind.JsonNode,
        schema: AsyncAPISchema,
        pathPrefix: String,
        depth: Int,
        out: MutableList<ReplyFieldAssertion>
    ) {
        if (depth > MAX_REPLY_ASSERTION_DEPTH) return
        if (!node.isObject) return

        // Composition: oneOf / anyOf / allOf. M11-PR2 fix G.
        val variants = listOf("oneOf", "anyOf", "allOf").firstNotNullOfOrNull { keyword ->
            node.get(keyword)?.takeIf { it.isArray && it.size() > 0 }?.let { keyword to it }
        }
        if (variants != null) {
            val (compositionKeyword, variantArr) = variants
            // Discriminator surfacing — emit an ENUM-style assertion on the
            // discriminator property with the declared variant names.
            // Tolerant of both inline `mapping` and bare variant `$ref` names.
            // M11-PR2 fix H.
            node.get("discriminator")?.takeIf { it.isObject }?.get("propertyName")
                ?.takeIf { it.isTextual }
                ?.let { discProp ->
                    val variantNames = collectVariantNames(node.get("discriminator"), variantArr)
                    if (variantNames.isNotEmpty()) {
                        out.add(ReplyFieldAssertion(
                            path = joinPath(pathPrefix, discProp.asText()),
                            kind = ReplyFieldAssertion.Kind.DISCRIMINATOR,
                            expectedValues = variantNames
                        ))
                    }
                }
            // For oneOf / anyOf / allOf: collect each variant's full assertion
            // set, then keep only assertions that *every* variant shares.
            // (`allOf` requires AND, so intersection is the conservative
            // common-ground anyway.)
            val perVariantBatches: List<List<ReplyFieldAssertion>> = variantArr.map { v ->
                val resolved = resolveSchemaRef(v, schema) ?: return@map emptyList()
                val variantBatch = mutableListOf<ReplyFieldAssertion>()
                collectFieldAssertions(resolved, schema, pathPrefix, depth + 1, variantBatch)
                variantBatch
            }
            if (perVariantBatches.isNotEmpty()) {
                val intersection = perVariantBatches.reduce { acc, batch -> acc.intersect(batch.toSet()).toList() }
                out.addAll(intersection)
            }
            return
        }

        val properties = node.get("properties")?.takeIf { it.isObject }
        val required = node.get("required")?.takeIf { it.isArray }
            ?.map { it.asText() }?.toSet() ?: emptySet()

        properties?.fields()?.forEach { (name, prop) ->
            val fullPath = joinPath(pathPrefix, name)
            if (name in required) {
                out.add(ReplyFieldAssertion(path = fullPath, kind = ReplyFieldAssertion.Kind.REQUIRED))
            }
            // Resolve $ref so nested schema fragments contribute their facets.
            val resolved = resolveSchemaRef(prop, schema) ?: prop

            // Scalar facets on the property itself.
            resolved.get("enum")?.takeIf { it.isArray }?.let { enumNode ->
                val values = enumNode.mapNotNull { if (it.isTextual) it.asText() else null }
                if (values.isNotEmpty()) {
                    out.add(ReplyFieldAssertion(
                        path = fullPath, kind = ReplyFieldAssertion.Kind.ENUM, expectedValues = values
                    ))
                }
            }
            resolved.get("const")?.takeIf { it.isValueNode }?.let { c ->
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.CONST,
                    expectedValues = listOf(c.asText())
                ))
            }
            resolved.get("pattern")?.takeIf { it.isTextual }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.PATTERN, pattern = it.asText()
                ))
            }
            resolved.get("minimum")?.takeIf { it.isNumber }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.MIN, numericBound = it.asDouble()
                ))
            }
            resolved.get("maximum")?.takeIf { it.isNumber }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.MAX, numericBound = it.asDouble()
                ))
            }
            resolved.get("multipleOf")?.takeIf { it.isNumber }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.MULTIPLE_OF, numericBound = it.asDouble()
                ))
            }
            resolved.get("minLength")?.takeIf { it.isInt }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.MIN_LENGTH, lengthBound = it.asInt()
                ))
            }
            resolved.get("maxLength")?.takeIf { it.isInt }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.MAX_LENGTH, lengthBound = it.asInt()
                ))
            }
            resolved.get("format")?.takeIf { it.isTextual }?.let {
                out.add(ReplyFieldAssertion(
                    path = fullPath, kind = ReplyFieldAssertion.Kind.FORMAT, format = it.asText()
                ))
            }
            // Array facets.
            val typeText = resolved.get("type")?.takeIf { it.isTextual }?.asText()
            if (typeText == "array") {
                resolved.get("minItems")?.takeIf { it.isInt }?.let {
                    out.add(ReplyFieldAssertion(
                        path = fullPath, kind = ReplyFieldAssertion.Kind.ARRAY_MIN_ITEMS, lengthBound = it.asInt()
                    ))
                }
                resolved.get("maxItems")?.takeIf { it.isInt }?.let {
                    out.add(ReplyFieldAssertion(
                        path = fullPath, kind = ReplyFieldAssertion.Kind.ARRAY_MAX_ITEMS, lengthBound = it.asInt()
                    ))
                }
                resolved.get("uniqueItems")?.takeIf { it.isBoolean && it.asBoolean() }?.let {
                    out.add(ReplyFieldAssertion(
                        path = fullPath, kind = ReplyFieldAssertion.Kind.ARRAY_UNIQUE
                    ))
                }
            }

            // Recurse into nested objects (depth-capped). M11-PR2 fix A.
            if (typeText == "object" || resolved.get("properties") != null) {
                collectFieldAssertions(resolved, schema, fullPath, depth + 1, out)
            }
        }
    }

    private fun resolveSchemaRef(
        node: com.fasterxml.jackson.databind.JsonNode,
        schema: AsyncAPISchema
    ): com.fasterxml.jackson.databind.JsonNode? {
        if (!node.isObject) return node
        val ref = node.get("\$ref")?.takeIf { it.isTextual }?.asText() ?: return node
        // Only resolve refs that look like component lookups.
        val key = ref.substringAfterLast("/")
        return schema.componentSchemas[key]
    }

    private fun collectVariantNames(
        discriminator: com.fasterxml.jackson.databind.JsonNode,
        variantArr: com.fasterxml.jackson.databind.JsonNode
    ): List<String> {
        val mapping = discriminator.get("mapping")
        if (mapping != null && mapping.isObject) {
            return mapping.fieldNames().asSequence().toList()
        }
        return variantArr.mapNotNull { v ->
            v.get("\$ref")?.takeIf { it.isTextual }?.asText()?.substringAfterLast("/")
        }
    }

    private fun joinPath(prefix: String, segment: String): String =
        if (prefix.isEmpty()) segment else "$prefix.$segment"

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

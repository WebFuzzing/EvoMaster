package org.evomaster.core.problem.asyncapi.service.fitness

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.EMConfig
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
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
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

        /**
         * Walk [payloadGene] and produce one schema-derivable target id per
         * (gene path, sampled enum value) pair:
         * `ENUM_VALUE_USED:<channel>:<op>:<path>=<value>`.
         *
         * Lifts the black-box target ceiling beyond the coarse-grained
         * delivery / reply signals: with a `send` operation that dispatches
         * on an enum field (e.g. `operation: triangle|bessjy|expint|...`),
         * the EA now has a target per branch instead of one for the whole
         * operation, giving MIO a gradient to explore each enum value at
         * least once.
         *
         * Path uses `gene.name` for the leaf — collisions only happen if two
         * enums in different sub-objects share a field name; coverage still
         * fires correctly in that case, just under a shared id (acceptable
         * given the schema-derivable rule).  Pulled out as a pure function so
         * unit tests can assert the target set without spinning up a broker.
         */
        fun enumValueTargets(action: AsyncAPIAction, payloadGene: Gene): List<String> {
            return payloadGene.flatView()
                .filterIsInstance<EnumGene<*>>()
                .map { g ->
                    "ENUM_VALUE_USED:${action.channelAddress}:${action.operationName}:${g.name}=${g.getValueAsRawString()}"
                }
        }

        /**
         * Walk [payloadGene] and produce one schema-derivable target id per
         * (gene path, sampled boolean value) pair:
         * `BOOLEAN_VALUE_USED:<channel>:<op>:<path>=<true|false>`.
         *
         * Mirrors [enumValueTargets] for [BooleanGene] — REST's
         * `handleAdvancedBlackBoxCriteria` handles enums and booleans in the
         * same `when` arm, so this brings the AsyncAPI side into line.  Kept
         * as a separate helper (rather than overloading ENUM_VALUE_USED) so
         * the target family is unambiguous on inspection: an enum literal
         * and a boolean literal are conceptually different inputs even when
         * the EA treats them similarly.
         */
        fun booleanValueTargets(action: AsyncAPIAction, payloadGene: Gene): List<String> {
            return payloadGene.flatView()
                .filterIsInstance<BooleanGene>()
                .map { g ->
                    "BOOLEAN_VALUE_USED:${action.channelAddress}:${action.operationName}:${g.name}=${g.getValueAsRawString()}"
                }
        }

        /**
         * Walk [payloadGene] and produce one schema-derivable target per
         * (optional field, present|absent) pair:
         * `FIELD_PRESENCE:<channel>:<op>:<path>=<present|absent>`.
         *
         * RestActionBuilderV3 wraps every JSON-Schema property *not* listed in
         * the schema's `required` array in an [OptionalGene]; this hook reads
         * the wrapper's `isActive` flag at sample time so MIO has a gradient
         * pulling each optional field through both presence states.  Required
         * fields stay non-optional (per the schema) and are not represented
         * here — exploring required-field omission requires a deliberate
         * builder change that violates the declared schema and is tracked
         * separately.
         *
         * Pulled out as a pure function so unit tests can assert the target
         * set without spinning up a broker.
         */
        fun fieldPresenceTargets(action: AsyncAPIAction, payloadGene: Gene): List<String> {
            return payloadGene.flatView()
                .filterIsInstance<OptionalGene>()
                .map { g ->
                    val state = if (g.isActive) "present" else "absent"
                    "FIELD_PRESENCE:${action.channelAddress}:${action.operationName}:${g.name}=$state"
                }
        }

        /**
         * Walk [payloadGene] and produce one target per numeric / string
         * boundary the sampled value happens to land on:
         * `BOUNDARY_HIT:<channel>:<op>:<path>=<at-min|at-max|at-min-length|at-max-length>`.
         *
         * Targets only fire when the value sits *exactly* on a schema-declared
         * bound — this is the schema-derivable signal that the EA explored a
         * boundary (which is where most off-by-one and overflow bugs live).
         * Genes whose bounds are the type-level extremes (Int.MIN/MAX, default
         * string limits) are skipped because hitting those carries no signal.
         *
         * `pattern` constraints are out of scope for v1 — regex satisfaction is
         * a binary signal that's already covered indirectly by the schema-
         * validity check on the reply, so adding it here would mostly be
         * duplicate noise.
         */
        /**
         * For each optional field in the matched reply-variant schema (any
         * property NOT listed in the variant's `required` array), emit one
         * schema-derivable target reading whether the actual reply payload
         * contained the field:
         * `REPLY_FIELD_PRESENCE:<variant>:<channel>:<op>:<field>=<present|absent>`.
         *
         * Reply payloads in real-world AsyncAPI schemas are usually richer
         * than request payloads — error replies tend to carry `code` always
         * but `details` only sometimes, success replies tend to have optional
         * progress / pagination fields.  Without this hook the EA has no
         * signal to push the SUT toward emitting the optional fields.
         *
         * Pulled out as a pure function (companion object) so unit tests can
         * assert the target set directly off a hand-rolled JsonNode pair
         * without spinning up a broker or running a full evaluation.
         */
        fun replyFieldPresenceTargets(
            action: AsyncAPIAction,
            variantName: String?,
            parsedReply: JsonNode,
            variantSchema: JsonNode
        ): List<String> {
            if (!parsedReply.isObject) return emptyList()
            val properties = variantSchema.get("properties") ?: return emptyList()
            if (!properties.isObject) return emptyList()
            val required = variantSchema.get("required")?.takeIf { it.isArray }
                ?.map { it.asText() }?.toSet() ?: emptySet()
            val variantTag = variantName ?: "default"
            val out = mutableListOf<String>()
            properties.fieldNames().forEach { name ->
                if (name in required) return@forEach
                val state = if (parsedReply.has(name)) "present" else "absent"
                out += "REPLY_FIELD_PRESENCE:$variantTag:${action.channelAddress}:${action.operationName}:$name=$state"
            }
            return out
        }

        /**
         * Output-side mirror of [replyFieldPresenceTargets]. For each declared
         * property in the matched output variant, emit a per-field presence
         * target tagged `present` or `absent` based on the observed payload.
         *
         * Pure function, exposed for unit testing the field-presence ladder
         * without spinning up a broker.
         */
        fun outputFieldPresenceTargets(
            action: AsyncAPIAction,
            messageId: String,
            payload: JsonNode,
            variantSchema: JsonNode
        ): List<String> {
            if (!payload.isObject || !variantSchema.isObject) return emptyList()
            val properties = variantSchema.get("properties") ?: return emptyList()
            if (!properties.isObject) return emptyList()
            val out = mutableListOf<String>()
            properties.fieldNames().forEach { fieldName ->
                val status = if (payload.has(fieldName)) "present" else "absent"
                out.add(
                    "OUTPUT_FIELD_PRESENCE:${action.channelAddress}:$messageId:$fieldName=$status"
                )
            }
            return out
        }

        fun boundaryTargets(action: AsyncAPIAction, payloadGene: Gene): List<String> {
            val out = mutableListOf<String>()
            payloadGene.flatView().forEach { g ->
                when (g) {
                    is NumberGene<*> -> {
                        val v = g.value
                        val effectiveMin = g.getMinimum()
                        val effectiveMax = g.getMaximum()
                        // Only emit when the schema actually constrained the
                        // gene; type-level extremes mean "unbounded".
                        val minIsConstrained = effectiveMin.toDouble() != Long.MIN_VALUE.toDouble()
                                && effectiveMin.toDouble() != Int.MIN_VALUE.toDouble()
                                && effectiveMin.toDouble() != -Double.MAX_VALUE
                        val maxIsConstrained = effectiveMax.toDouble() != Long.MAX_VALUE.toDouble()
                                && effectiveMax.toDouble() != Int.MAX_VALUE.toDouble()
                                && effectiveMax.toDouble() != Double.MAX_VALUE
                        if (minIsConstrained && v.toDouble() == effectiveMin.toDouble()) {
                            out += "BOUNDARY_HIT:${action.channelAddress}:${action.operationName}:${g.name}=at-min"
                        }
                        if (maxIsConstrained && v.toDouble() == effectiveMax.toDouble()) {
                            out += "BOUNDARY_HIT:${action.channelAddress}:${action.operationName}:${g.name}=at-max"
                        }
                    }
                    is StringGene -> {
                        val len = g.getValueAsRawString().length
                        if (g.minLength > 0 && len == g.minLength) {
                            out += "BOUNDARY_HIT:${action.channelAddress}:${action.operationName}:${g.name}=at-min-length"
                        }
                        if (g.maxLength < EMConfig.stringLengthHardLimit && len == g.maxLength) {
                            out += "BOUNDARY_HIT:${action.channelAddress}:${action.operationName}:${g.name}=at-max-length"
                        }
                    }
                }
            }
            return out
        }
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

        // Subclass hook: white-box overrides to reset SUT state before each
        // individual so coverage is measured against a clean baseline.  Black-
        // box defaults to a no-op (no driver to call).
        beforeIndividualEvaluation()

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
                    AsyncAPIAction.Kind.PUBLISH -> {
                        handlePublish(action, fv, correlationByPair, result)
                        // Fire-and-forget operations have no SUBSCRIBE_REPLY barrier
                        // after the publish, so the SUT's consumer handler may still
                        // be running by the time we process the next action.  Defer to
                        // [awaitConsumerSettled] — black-box uses the simple settle,
                        // white-box overrides with coverage-stabilisation polling.
                        // Skipped when a paired SUBSCRIBE_REPLY follows, because
                        // awaiting the reply already serves as the barrier.
                        if (!result.stopping && !isFollowedByReply(actions, index, action.pairId)) {
                            awaitConsumerSettled(action, index)
                        }
                    }
                    AsyncAPIAction.Kind.SUBSCRIBE_REPLY -> handleSubscribeReply(action, schema, fv, correlationByPair, result)
                    AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT -> handleSubscribeOutput(action, schema, fv, result)
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
     * Hook invoked once at the very start of each individual evaluation, after
     * the broker has been connected but before any actions run.  Default:
     * no-op (black-box).  White-box overrides this to reset the SUT so
     * coverage doesn't accumulate against a dirty baseline across individuals.
     */
    protected open fun beforeIndividualEvaluation() {
        // intentionally empty
    }

    private fun isFollowedByReply(
        actions: List<AsyncAPIAction>,
        index: Int,
        pairId: String
    ): Boolean {
        val next = actions.getOrNull(index + 1) ?: return false
        return next.kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY && next.pairId == pairId
    }

    /**
     * Default settle: a fixed sleep of [EMConfig.asyncApiFireAndForgetSettleMs].
     * Used by black-box (no driver to poll) and as the lower-bound floor before
     * white-box's stabilisation loop kicks in.  Exposed `protected` so the
     * white-box subclass can call it as part of the hybrid strategy.
     */
    protected fun applyFireAndForgetSettle() {
        val ms = config.asyncApiFireAndForgetSettleMs.toLong()
        if (ms <= 0) return
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Hook invoked after a fire-and-forget PUBLISH (a [PUBLISH][AsyncAPIAction.Kind.PUBLISH]
     * with no following [SUBSCRIBE_REPLY][AsyncAPIAction.Kind.SUBSCRIBE_REPLY]
     * twin).  Default: the simple settle from [applyFireAndForgetSettle].
     * White-box overrides this with coverage-stabilisation polling against the
     * EM Driver — see [AsyncAPIFitness.awaitConsumerSettled].
     */
    protected open fun awaitConsumerSettled(action: AsyncAPIAction, index: Int) {
        applyFireAndForgetSettle()
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

        // Render the templated channel address (e.g. `tenants/{tenantId}/orders`)
        // by reading each per-parameter gene's value.  Untemplated addresses
        // pass through untouched.
        val channelAddress = renderChannelAddress(action)

        val headers = mutableMapOf<String, ByteArray>()
        action.correlationHeaderName?.let { name ->
            headers[name] = correlationId.toByteArray(StandardCharsets.UTF_8)
        }
        // User-defined headers (auth tokens, tenant ids, tracing) live in a
        // separate gene tree from the payload so the EA can mutate them
        // independently.  Render each scalar property to a UTF-8 string and
        // stamp it on the Kafka record before publishing.
        val headersGene = action.headersParam()?.primaryGene()
        materialiseHeaders(headersGene)?.forEach { (k, v) -> headers[k] = v }

        // AsyncAPI 3.0 Kafka binding: message.bindings.kafka.key, if declared,
        // is materialised as the action's KEY_PARAM gene.  Stamping it on the
        // record tells the broker which partition to use — partition-aware
        // SUT logic (e.g. per-tenant handlers) needs this to be exercised.
        val kafkaKey = action.keyParam()?.primaryGene()?.getValueAsRawString()

        val outcome = broker.publish(
            channel = channelAddress,
            key = kafkaKey,
            headers = headers,
            payload = payloadJson.toByteArray(StandardCharsets.UTF_8)
        )

        when (outcome) {
            is MessageBrokerClient.PublishOutcome.Sent -> {
                coverLocal(fv, "DELIVERY_OK:${action.channelAddress}:${action.operationName}")
                // Gate the input-side coverage layer behind the same
                // --advancedBlackBoxCoverage flag REST uses for the equivalent
                // `handleAdvancedBlackBoxCriteria` machinery (default true).
                // Every per-input target (enum, boolean, presence, boundary,
                // per-variant message dispatch) sits inside the same gate —
                // they are all "advanced black-box coverage criteria" in
                // REST's terminology.
                if (config.advancedBlackBoxCoverage) {
                    // When an operation can dispatch multiple message types
                    // over one channel, this target records which variant was
                    // actually sent so MIO has a per-variant gradient.
                    coverLocal(fv, "PUBLISH_MESSAGE_TYPE:${action.channelAddress}:${action.operationName}=${action.messageId}")
                    payloadGene?.let { gene ->
                        AbstractAsyncAPIFitness.enumValueTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.booleanValueTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.fieldPresenceTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.boundaryTargets(action, gene).forEach { coverLocal(fv, it) }
                    }
                    headersGene?.let { gene ->
                        AbstractAsyncAPIFitness.enumValueTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.fieldPresenceTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.boundaryTargets(action, gene).forEach { coverLocal(fv, it) }
                    }
                    action.channelParams().values.forEach { param ->
                        val gene = param.primaryGene()
                        AbstractAsyncAPIFitness.enumValueTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.fieldPresenceTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.boundaryTargets(action, gene).forEach { coverLocal(fv, it) }
                    }
                    // Kafka key gene: when bindings.kafka.key declares an enum
                    // (e.g. tenant routing keys), the existing enumValueTargets
                    // helper picks each value up automatically.  Same gate as
                    // the other input-side coverage so the flag's contract
                    // stays consistent.
                    action.keyParam()?.primaryGene()?.let { gene ->
                        AbstractAsyncAPIFitness.enumValueTargets(action, gene).forEach { coverLocal(fv, it) }
                        AbstractAsyncAPIFitness.boundaryTargets(action, gene).forEach { coverLocal(fv, it) }
                    }
                }
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

    /**
     * Substitute every `{paramName}` placeholder in the templated address
     * with the per-parameter gene's current value.  Returns the raw
     * address unchanged when the action has no channel parameters.
     */
    private fun renderChannelAddress(action: AsyncAPIAction): String {
        val params = action.channelParams()
        if (params.isEmpty()) return action.channelAddress
        var rendered = action.channelAddress
        params.forEach { (name, param) ->
            val value = param.primaryGene().getValueAsRawString()
            rendered = rendered.replace("{$name}", value)
        }
        return rendered
    }

    /**
     * Render a parsed headers gene tree (an [ObjectGene] of scalar
     * properties) into a Map ready for [MessageBrokerClient.publish].
     * Inactive [OptionalGene] fields are skipped — that's how the EA
     * fuzzes header omission.  Non-string scalars are converted via
     * [Gene.getValueAsRawString] so an `integer`-typed header value
     * (e.g. priority) still serialises sensibly.
     */
    private fun materialiseHeaders(headersGene: Gene?): Map<String, ByteArray>? {
        if (headersGene !is org.evomaster.core.search.gene.ObjectGene) return null
        val out = mutableMapOf<String, ByteArray>()
        headersGene.fields.forEach { field ->
            when (field) {
                is OptionalGene -> if (field.isActive) {
                    out[field.name] = field.gene.getValueAsRawString().toByteArray(StandardCharsets.UTF_8)
                }
                else -> out[field.name] = field.getValueAsRawString().toByteArray(StandardCharsets.UTF_8)
            }
        }
        return out
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

        // Reply channels can also be templated (e.g. tenant-scoped reply
        // topics).  Render the same way as PUBLISH so the consumer subscribes
        // to the rendered topic.
        val replyChannelAddress = renderChannelAddress(action)
        val outcome = broker.awaitFirstMatching(
            channel = replyChannelAddress,
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

    /**
     * Bracket a fixed-duration listen window on a SUT-produced channel and
     * attribute schema-derivable targets to whatever messages arrived.
     *
     * The schema doesn't encode causality between a publish and an emitted
     * event, so we never claim "publish X caused output Y" — the window-only
     * design is documented at §13.4 of the thesis as the proof-of-concept.
     */
    private fun handleSubscribeOutput(
        action: AsyncAPIAction,
        schema: AsyncAPISchema,
        fv: FitnessValue,
        result: ActionResult
    ) {
        val windowMs = config.asyncApiOutputObservationWindowMs.toLong()
        if (windowMs <= 0) {
            // Observation disabled by flag; nothing to do, no targets emitted.
            return
        }
        val outputChannel = renderChannelAddress(action)
        val collected = broker.collectAllWithin(outputChannel, windowMs)

        if (collected.isEmpty()) {
            coverLocal(fv, "OUTPUT_NOTHING:$outputChannel")
            result.addResultValue("output", "nothing")
            return
        }
        coverLocal(fv, "OUTPUT_RECEIVED:$outputChannel")
        result.addResultValue("output", "received:${collected.size}")

        if (!config.advancedBlackBoxCoverage) {
            // The bare OUTPUT_RECEIVED / OUTPUT_NOTHING targets are always
            // emitted because they're the baseline observation signal; the
            // richer per-variant / per-field targets sit behind the same
            // gate as the publish-side advanced families.
            return
        }

        val candidateMessageIds = (listOf(action.messageId) + action.additionalReplyMessageIds)
            .filter { it.isNotBlank() }

        // For each observed message, try every declared variant. The
        // schema-validity gradient is per-message; the per-variant target
        // fires on the first matching message of each declared variant.
        val variantsSeen = mutableSetOf<String>()
        var anyInvalid = false

        for (received in collected) {
            val text = received.payload.toString(StandardCharsets.UTF_8)
            val parsed: JsonNode = try {
                mapper.readTree(text)
            } catch (_: Exception) {
                coverLocal(fv, "OUTPUT_SCHEMA_INVALID:$outputChannel")
                anyInvalid = true
                continue
            }

            var matchedId: String? = null
            var matchedSchema: JsonNode? = null
            for (messageId in candidateMessageIds) {
                val message = schema.messages[messageId] ?: continue
                val payloadSchema = resolvePayloadNode(message, schema) ?: continue
                val match = matchVariants(parsed, payloadSchema, schema)
                if (match.anyValid) {
                    matchedId = messageId
                    matchedSchema = match.matchedVariantSchema ?: payloadSchema
                    break
                }
            }
            if (matchedId != null) {
                coverLocal(fv, "OUTPUT_SCHEMA_VALID:$outputChannel:$matchedId")
                if (variantsSeen.add(matchedId)) {
                    coverLocal(fv, "OUTPUT_MESSAGE_TYPE:$outputChannel=$matchedId")
                }
                matchedSchema?.let { variantSchema ->
                    AbstractAsyncAPIFitness.outputFieldPresenceTargets(action, matchedId, parsed, variantSchema)
                        .forEach { coverLocal(fv, it) }
                }
            } else {
                if (candidateMessageIds.isEmpty()) {
                    // Bare channel with no declared messages: count the
                    // arrival as schema-valid (anything is acceptable).
                    coverLocal(fv, "OUTPUT_SCHEMA_VALID:$outputChannel:_any")
                } else {
                    coverLocal(fv, "OUTPUT_SCHEMA_INVALID:$outputChannel")
                    anyInvalid = true
                }
            }
        }

        if (anyInvalid) result.addResultValue("outputSchemaInvalid", "true")
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

        // AsyncAPI 3.0 lets `reply.messages: [...]` enumerate several reply
        // variants on a channel.  Try every declared variant and emit the
        // signal for the first whose schema validates.
        val candidateMessageIds = listOf(action.messageId) + action.additionalReplyMessageIds
        var matchedAgainstId: String? = null
        var winningMatch: VariantMatch? = null
        for (messageId in candidateMessageIds) {
            val message = schema.messages[messageId] ?: continue
            val replyPayloadSchema = resolvePayloadNode(message, schema) ?: continue
            val match = matchVariants(parsed, replyPayloadSchema, schema)
            if (match.anyValid) {
                matchedAgainstId = messageId
                winningMatch = match
                break
            }
        }

        if (winningMatch == null) {
            // No schema available at all → unknown rather than invalid (keeps
            // the existing "no payload schema" semantics).
            val anyResolvable = candidateMessageIds.any {
                schema.messages[it]?.let { m -> resolvePayloadNode(m, schema) } != null
            }
            if (!anyResolvable) {
                coverLocal(fv, "REPLY_SCHEMA_VALID:${action.channelAddress}:${action.operationName}")
                result.addResultValue("schemaValid", "unknown")
            } else {
                coverLocal(fv, "REPLY_SCHEMA_INVALID:${action.channelAddress}:${action.operationName}")
                result.addResultValue("schemaValid", "false")
            }
            return
        }

        coverLocal(fv, "REPLY_SCHEMA_VALID:${action.channelAddress}:${action.operationName}")
        result.addResultValue("schemaValid", "true")
        winningMatch.matchedVariantName?.let { variant ->
            coverLocal(fv, "REPLY_VARIANT:${variant}:${action.channelAddress}:${action.operationName}")
            result.addResultValue("variant", variant)
        }
        // Reply-side advanced coverage targets (per-variant-actually-fired
        // and per-optional-field presence) sit behind the same flag as the
        // publish-side advanced coverage — they are the reply analog of
        // PUBLISH_MESSAGE_TYPE / FIELD_PRESENCE, same contract.
        if (config.advancedBlackBoxCoverage) {
            if (action.additionalReplyMessageIds.isNotEmpty() && matchedAgainstId != null) {
                coverLocal(fv, "REPLY_MESSAGE_TYPE:${action.channelAddress}:${action.operationName}=$matchedAgainstId")
                result.addResultValue("replyMessageType", matchedAgainstId!!)
            }
            winningMatch.matchedVariantSchema?.let { variantSchema ->
                AbstractAsyncAPIFitness.replyFieldPresenceTargets(
                    action, winningMatch.matchedVariantName, parsed, variantSchema
                ).forEach { coverLocal(fv, it) }
            }
        }
    }


    private fun resolvePayloadNode(message: AsyncAPIMessage, schema: AsyncAPISchema): JsonNode? {
        return message.payloadInline ?: message.payloadSchemaRef?.let { schema.componentSchemas[it] }
    }

    private data class VariantMatch(
        val anyValid: Boolean,
        val matchedVariantName: String?,
        val matchedVariantSchema: JsonNode?
    )

    private fun matchVariants(payload: JsonNode, schemaNode: JsonNode, schema: AsyncAPISchema): VariantMatch {
        val variants: List<Pair<String, JsonNode>> = when {
            schemaNode.has("oneOf") -> resolveVariants(schemaNode.get("oneOf"), schema)
            schemaNode.has("anyOf") -> resolveVariants(schemaNode.get("anyOf"), schema)
            else -> listOf("default" to schemaNode)
        }

        var matchedName: String? = null
        var matchedSchema: JsonNode? = null
        for ((name, variant) in variants) {
            if (matchesShallow(payload, variant)) {
                matchedName = name
                matchedSchema = variant
                break
            }
        }
        return VariantMatch(
            anyValid = matchedName != null,
            matchedVariantName = matchedName,
            matchedVariantSchema = matchedSchema
        )
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

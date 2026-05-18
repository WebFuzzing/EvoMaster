package org.evomaster.core.problem.asyncapi.service.sampler

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.asyncapi.builder.AsyncAPIActionBuilder
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.asyncapi.schema.AsyncAPISchema
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.remote.SutProblemException
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Random sampler for AsyncAPI individuals.  Actions come from the parsed
 * schema's `send`/`request` operations; each PUBLISH may carry a paired
 * SUBSCRIBE_REPLY immediately after it.
 *
 * Smart sampling is intentionally not implemented in the starter slice — the
 * EA explores via gene mutation and structure mutation only.
 */
class AsyncAPISampler : ApiWsSampler<AsyncAPIIndividual>() {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncAPISampler::class.java)
    }

    @Inject
    protected lateinit var configuration: EMConfig

    /** Operation name → ordered list of paired actions (PUBLISH, optional SUBSCRIBE_REPLY). */
    private val operationTemplates = LinkedHashMap<String, List<AsyncAPIAction>>()

    /**
     * SUBSCRIBE_OUTPUT templates lifted out of [operationTemplates] so the
     * sampler can append them once per individual (after the last PUBLISH)
     * rather than treating them as samplable operations. Built once at
     * [initialize] time and copied per individual.
     */
    private val outputSubscribeTemplates = mutableListOf<AsyncAPIAction>()

    var parsedSchema: AsyncAPISchema? = null
        private set

    @PostConstruct
    fun initialize() {
        log.debug("Initializing AsyncAPISampler (blackBox={}, bbAsyncApiUrl={})",
            configuration.blackBox, configuration.bbAsyncApiUrl)

        val schemaUrl = resolveSchemaUrl()
        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(schemaUrl)
        parsedSchema = schema

        val built = AsyncAPIActionBuilder(configuration).build(schema)
        // Split the builder output: PUBLISH-bearing entries are sampleable
        // (operationTemplates); bare SUBSCRIBE_OUTPUT entries are appended
        // once per individual (outputSubscribeTemplates).
        built.operations.forEach { (key, actions) ->
            if (actions.any { it.kind == AsyncAPIAction.Kind.PUBLISH }) {
                operationTemplates[key] = actions
            } else {
                outputSubscribeTemplates.addAll(
                    actions.filter { it.kind == AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT }
                )
            }
        }

        // Populate the inherited actionCluster (one entry per PUBLISH action)
        // so search internals that iterate over actionCluster see the AsyncAPI
        // ones too.  Keys mirror the action's getName() output.
        operationTemplates.values.flatten().forEach { action ->
            actionCluster[action.getName()] = action
        }

        if (operationTemplates.isEmpty()) {
            throw SutProblemException(
                "No triggerable AsyncAPI operations found in schema at $schemaUrl." +
                        " The engine drives the SUT via `action: receive` operations" +
                        " (= channels the SUT consumes from); this schema only declares" +
                        " `action: send` operations, which are observation-only and produce" +
                        " SUBSCRIBE_OUTPUT actions without PUBLISH counterparts." +
                        " Pick a schema that declares at least one `action: receive`" +
                        " operation, or extend the SUT's contract to declare its input" +
                        " channels."
            )
        }

        log.info(
            "AsyncAPI operation cluster initialised with {} operation(s) + {} output-observation channel(s)",
            operationTemplates.size, outputSubscribeTemplates.size
        )
    }

    /**
     * Black-box reads the schema URL from `--bbAsyncApiUrl`; white-box reads
     * it from the EM Driver's `SutInfoDto.asyncAPIProblem.asyncApiUrl`.  The
     * driver call is done here on first use so the rest of the search loop
     * can stay agnostic.
     */
    private fun resolveSchemaUrl(): String {
        if (configuration.blackBox) {
            if (configuration.bbAsyncApiUrl.isBlank()) {
                throw SutProblemException("Missing --bbAsyncApiUrl for AsyncAPI black-box mode")
            }
            return configuration.bbAsyncApiUrl
        }

        // White-box: ask the EM Driver where the schema lives.
        rc.checkConnection()
        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }
        val infoDto = rc.getSutInfo()
            ?: throw SutProblemException("Failed to retrieve the info about the system under test")
        val problem = infoDto.asyncAPIProblem
            ?: throw SutProblemException(
                "EM Driver did not report an AsyncAPI problem; check the controller's getProblemInfo() implementation"
            )
        // Pick up driver-supplied defaults (e.g. preferred output format) so
        // generated tests get the right language even when --outputFormat is
        // left at its DEFAULT.
        updateConfigBasedOnSutInfoDto(infoDto)
        val url = problem.asyncApiUrl
        if (url.isNullOrBlank()) {
            throw SutProblemException(
                "EM Driver's AsyncAPIProblem must set asyncApiUrl (inline asyncApiSchema is not supported in this slice)"
            )
        }
        return url
    }

    override fun sampleAtRandom(): AsyncAPIIndividual {
        val maxActions = configuration.maxTestSize.coerceAtLeast(1)
        val length = randomness.nextInt(1, maxActions)
        val actions = mutableListOf<AsyncAPIAction>()

        repeat(length) {
            val opKey = randomness.choose(operationTemplates.keys.toList())
            val pair = operationTemplates[opKey]!!.map { it.copy() as AsyncAPIAction }
            // Pair was sampled fresh so gene state is independent.
            actions.addAll(pair)
            // Re-thread pairId through the copies so PUBLISH and SUBSCRIBE_REPLY
            // remain matched after the per-action copy().
            if (pair.size > 1) {
                val sharedPairId = "evm-${randomness.nextInt(0, Int.MAX_VALUE).toString(16)}"
                pair.forEach { syncPairId(it, sharedPairId, actions) }
            }
        }

        // Tail of per-individual output-observation actions, one per
        // SUT-produced channel declared in the schema. Positioned after every
        // PUBLISH so the listen-window brackets any events the SUT may emit.
        outputSubscribeTemplates.forEach { template ->
            actions.add(template.copy() as AsyncAPIAction)
        }

        actions.forEach { it.doInitialize(randomness) }

        val individual = AsyncAPIIndividual(SampleType.RANDOM, actions)
        individual.doGlobalInitialize(searchGlobalState)
        return individual
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        // Seeded tests are not supported for AsyncAPI in the starter slice.
    }

    /**
     * Replace [pairId] across the copies in [batch] so the published action
     * and its subscribe-reply twin agree.  Implemented as a small helper to
     * keep [sampleAtRandom] readable; we do not mutate the original templates.
     */
    private fun syncPairId(action: AsyncAPIAction, sharedPairId: String, batch: MutableList<AsyncAPIAction>) {
        val replacement = AsyncAPIAction(
            operationName = action.operationName,
            channelAddress = action.channelAddress,
            channelName = action.channelName,
            kind = action.kind,
            pairId = sharedPairId,
            messageId = action.messageId,
            parameters = action.parameters
                .map { (it as org.evomaster.core.problem.asyncapi.param.AsyncAPIParam).copy() as org.evomaster.core.problem.asyncapi.param.AsyncAPIParam }
                .toMutableList(),
            replyBinding = action.replyBinding,
            correlationHeaderName = action.correlationHeaderName,
            auth = action.auth
        )
        val idx = batch.indexOf(action)
        if (idx >= 0) batch[idx] = replacement
    }
}

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

    var parsedSchema: AsyncAPISchema? = null
        private set

    @PostConstruct
    fun initialize() {
        log.debug("Initializing AsyncAPISampler (blackBox={}, bbAsyncApiUrl={})",
            configuration.blackBox, configuration.bbAsyncApiUrl)

        if (!configuration.blackBox) {
            throw IllegalStateException(
                "AsyncAPI white-box is not implemented in this build; run with --blackBox true"
            )
        }
        if (configuration.bbAsyncApiUrl.isBlank()) {
            throw SutProblemException("Missing --bbAsyncApiUrl for AsyncAPI black-box mode")
        }

        val schema = AsyncAPIAccess.getAsyncAPIFromLocation(configuration.bbAsyncApiUrl)
        parsedSchema = schema

        val built = AsyncAPIActionBuilder(configuration).build(schema)
        operationTemplates.putAll(built.operations)

        // Populate the inherited actionCluster (one entry per PUBLISH action)
        // so search internals that iterate over actionCluster see the AsyncAPI
        // ones too.  Keys mirror the action's getName() output.
        operationTemplates.values.flatten().forEach { action ->
            actionCluster[action.getName()] = action
        }

        if (operationTemplates.isEmpty()) {
            throw SutProblemException(
                "No usable AsyncAPI operations found in schema at ${configuration.bbAsyncApiUrl}." +
                        " Only `send`/`request` operations are exercisable for black-box."
            )
        }

        log.info("AsyncAPI operation cluster initialised with {} operation(s)", operationTemplates.size)
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

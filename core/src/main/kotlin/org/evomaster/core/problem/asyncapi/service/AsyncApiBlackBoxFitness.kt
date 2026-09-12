package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.asyncapi.models.AsyncApiCorrelationId
import com.webfuzzing.asyncapi.models.AsyncApiReply
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Publishes the messages of a test through the driver and turns what comes back into targets.
 *
 * The targets are the AsyncAPI analogue of REST's `(status x endpoint)`: for every operation,
 * what publishing to it was seen to do ([AsyncApiOutcome]), and, when a reply came back, which
 * of the messages the contract declares for the reply it was recognised as. A contract that
 * enumerates a result and an error thus gives the search two things to reach.
 *
 * Two outcomes are faults: a promised reply that never arrives, and a reply matching none of
 * the declared messages. A message the driver could not publish is neither; it is a broken
 * setup, and the test stops there.
 */
class AsyncApiBlackBoxFitness : ApiWsFitness<AsyncApiIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AsyncApiBlackBoxFitness::class.java)

        /**
         * Prefix of the `(outcome x operation)` targets, written as PREFIX:OUTCOME:action.
         */
        const val OUTCOME_TARGET_PREFIX = "ASYNCAPI_OUTCOME"

        /**
         * Prefix of the `(declared reply x operation)` targets, written as PREFIX:messageId:action.
         */
        const val REPLY_TARGET_PREFIX = "ASYNCAPI_REPLY"

        private const val DEFAULT_CONTENT_TYPE = "application/json"

        private val mapper = ObjectMapper()
    }

    @Inject
    private lateinit var asyncApiSampler: AsyncApiSampler

    /**
     * Tells this run's correlation ids from those of an earlier run against the same broker.
     * Drawn from [randomness], so that a seeded run is reproducible.
     */
    private val runId: String by lazy { Integer.toHexString(randomness.nextInt()) }

    /**
     * How many messages this run has published, which is what makes each correlation id unique.
     */
    private var published = 0L

    override fun doCalculateCoverage(
        individual: AsyncApiIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<AsyncApiIndividual>? {

        rc.resetSUT()

        val actionResults: MutableList<ActionResult> = mutableListOf()

        doDbCalls(individual.seeInitializingActions().filterIsInstance<SqlAction>(), actionResults = actionResults)

        val fv = FitnessValue(individual.size().toDouble())

        val actions = individual.seeMainExecutableActions().filterIsInstance<AsyncApiAction>()

        for ((index, action) in actions.withIndex()) {
            val ok = publish(action, index, actionResults, fv)
            if (!ok) {
                break
            }
        }

        val dto = updateFitnessAfterEvaluation(targets, allTargets, fullyCovered, descriptiveIds, individual, fv)
            ?: return null
        handleExtra(dto, fv)

        return EvaluatedIndividual(
            fv,
            individual.copy() as AsyncApiIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    /**
     * @return whether the message reached the wire, so that the test can go on
     */
    private fun publish(
        action: AsyncApiAction,
        index: Int,
        actionResults: MutableList<ActionResult>,
        fv: FitnessValue
    ): Boolean {

        searchTimeController.waitForRateLimiter()

        val result = AsyncApiCallResult(action.getLocalId())
        actionResults.add(result)

        val dto = getActionDto(action, index)
        dto.asyncApiCall = toDto(action)

        val reply = rc.executeNewAsyncApiActionAndGetReply(dto)

        if (reply == null || !reply.published) {
            /*
                Not a finding about the service: the driver could not put the message on the
                wire, or could not be reached at all. Nothing published after this point would
                mean anything, so the test stops here, and no target is registered for it.
             */
            result.setOutcome(AsyncApiOutcome.PUBLISH_FAILED)
            result.setErrorMessage(reply?.errorMessage ?: "No response from the driver")
            result.stopping = true
            return false
        }

        record(reply, result)
        handleTargets(fv, action, result, index)

        return true
    }

    private fun record(reply: AsyncApiReplyDto, result: AsyncApiCallResult) {

        val outcome = when {
            !reply.replyExpected -> AsyncApiOutcome.PUBLISHED
            reply.replyReceived -> AsyncApiOutcome.REPLIED
            else -> AsyncApiOutcome.NO_REPLY
        }

        result.setOutcome(outcome)
        reply.waitedMs?.let { result.setWaitedMs(it) }

        if (outcome == AsyncApiOutcome.REPLIED) {
            reply.replyPayload?.let { result.setReplyPayload(it) }
            result.setCorrelationMatched(reply.correlationMatched)
        }
    }

    private fun handleTargets(fv: FitnessValue, action: AsyncApiAction, result: AsyncApiCallResult, index: Int) {

        val name = action.getName()
        val outcome = result.getOutcome()!!

        fv.updateTarget(idMapper.handleLocalTarget("$OUTCOME_TARGET_PREFIX:${outcome.name}:$name"), 1.0, index)

        when (outcome) {

            AsyncApiOutcome.REPLIED -> handleReplyTargets(fv, action, result, index)

            AsyncApiOutcome.NO_REPLY -> {
                val fault = idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.ASYNCAPI_NO_REPLY, name)
                fv.updateTarget(idMapper.handleLocalTarget(fault), 1.0, index)
            }

            AsyncApiOutcome.PUBLISHED, AsyncApiOutcome.PUBLISH_FAILED -> Unit
        }
    }

    private fun handleReplyTargets(fv: FitnessValue, action: AsyncApiAction, result: AsyncApiCallResult, index: Int) {

        val name = action.getName()
        val document = asyncApiSampler.document
        val operation = document.operations[action.operationId] ?: return
        val declared = document.replyMessagesOf(operation)

        if (declared.isEmpty()) {
            //the contract says a reply comes, but not what it is: nothing to recognise it as
            return
        }

        val recognised = AsyncApiReplyClassifier.classify(result.getReplyPayload(), declared, document.componentSchemas)

        if (recognised == null) {
            val fault = idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY, name)
            fv.updateTarget(idMapper.handleLocalTarget(fault), 1.0, index)
            return
        }

        result.setReplyMessage(recognised.id)
        fv.updateTarget(idMapper.handleLocalTarget("$REPLY_TARGET_PREFIX:${recognised.id}:$name"), 1.0, index)
    }

    /**
     * Everything the driver needs to publish the message and wait for its reply, resolved
     * against the document so that the driver never has to read it.
     */
    private fun toDto(action: AsyncApiAction): AsyncApiActionDto {

        val document = asyncApiSampler.document
        val message = document.messages[action.messageId]
        val channel = document.channels[action.channelName]

        val dto = AsyncApiActionDto()
        dto.operationId = action.operationId
        dto.channelName = action.channelName
        dto.messageId = action.messageId

        /*
            A channel may declare no address, meaning it is decided at run time. The driver is
            then given the channel's name and left to map it, being the one that knows the broker.
         */
        dto.address = channel?.address ?: action.channelName

        dto.payload = action.parameters.firstOrNull { it.name == AsyncApiParam.PAYLOAD }
            ?.gene?.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)
        dto.contentType = message?.contentType ?: document.defaultContentType ?: DEFAULT_CONTENT_TYPE
        dto.headers = headersOf(action)

        dto.correlationId = "$runId-${published++}"
        message?.correlationId?.let {
            dto.correlationLocation = if (it.source == AsyncApiCorrelationId.Source.HEADER) {
                AsyncApiActionDto.CORRELATION_IN_HEADER
            } else {
                AsyncApiActionDto.CORRELATION_IN_PAYLOAD
            }
            dto.correlationPointer = it.pointer
        }

        action.replyTemplate?.let { reply ->
            dto.replyAddress = replyAddressOf(reply, action)
            if (dto.replyAddress != null) {
                dto.replyTimeoutMs = config.asyncApiReplyTimeoutMs.toLong()
            }
        }

        return dto
    }

    /**
     * The headers gene as a map, by way of its own JSON printing, which is what knows which
     * optional headers are on.
     */
    private fun headersOf(action: AsyncApiAction): MutableMap<String, String> {

        val headers = LinkedHashMap<String, String>()

        val gene = action.parameters.firstOrNull { it.name == AsyncApiParam.HEADERS }?.gene
            ?: return headers

        val json = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)

        val node = try {
            mapper.readTree(json)
        } catch (e: JsonProcessingException) {
            log.warn("The headers of '{}' did not print as JSON: {}", action.getName(), e.message)
            return headers
        }

        if (node.isObject) {
            node.fields().forEach { (name, value) ->
                headers[name] = if (value.isValueNode) value.asText() else value.toString()
            }
        }

        return headers
    }

    /**
     * Where the driver should wait for the reply, or null when there is nowhere to wait yet.
     */
    private fun replyAddressOf(reply: AsyncApiReply, action: AsyncApiAction): String? {

        val channelName = reply.channelName

        if (channelName == null) {
            /*
                The reply address is announced inside the request (reply.address.location) rather
                than fixed by the contract. Supporting that means minting an address and stamping
                it into the message, which is not done yet; until then such an operation is
                published without waiting.
             */
            LoggingUtil.uniqueUserWarn(
                "Operation '${action.operationId}' announces its reply address at run time, which is" +
                        " not supported yet: its replies will not be waited for"
            )
            return null
        }

        return asyncApiSampler.document.channels[channelName]?.address ?: channelName
    }
}

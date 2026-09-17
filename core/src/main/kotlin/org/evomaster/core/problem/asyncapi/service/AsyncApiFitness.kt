package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.asyncapi.models.AsyncApiChannel
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
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Publishes the messages of a test through the driver and turns what comes back into targets:
 * for every operation what publishing to it did, and which of the replies the contract declares
 * was recognised.
 */
class AsyncApiFitness : ApiWsFitness<AsyncApiIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AsyncApiFitness::class.java)

        /**
         * Prefix of the `(outcome x operation)` targets, written as PREFIX:OUTCOME:action.
         */
        private const val OUTCOME_TARGET_PREFIX = "ASYNCAPI_OUTCOME"

        /**
         * Prefix of the `(declared reply x operation)` targets, written as PREFIX:messageId:action.
         */
        private const val REPLY_TARGET_PREFIX = "ASYNCAPI_REPLY"

        private const val TARGET_SEPARATOR = ":"

        private const val CORRELATION_SEPARATOR = "-"

        private const val DEFAULT_CONTENT_TYPE = "application/json"

        /**
         * What a {placeholder} in a channel address starts with.
         */
        private const val PARAMETER_OPENING = "{"

        private val mapper = ObjectMapper()

        /**
         * The id of the target covered when publishing to [actionName] had [outcome].
         */
        private fun getOutcomeTargetId(outcome: AsyncApiOutcome, actionName: String): String =
            listOf(OUTCOME_TARGET_PREFIX, outcome.name, actionName).joinToString(TARGET_SEPARATOR)

        /**
         * The id of the target covered when a reply to [actionName] was recognised as [messageId].
         */
        private fun getReplyTargetId(messageId: String, actionName: String): String =
            listOf(REPLY_TARGET_PREFIX, messageId, actionName).joinToString(TARGET_SEPARATOR)
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

        if (reply == null || reply.published != true) {
            /*
                Not a finding about the service: the driver could not put the message on the
                wire, could not be reached at all, or did not say. Nothing published after this
                point would mean anything, so the test stops here, and no target is registered.
             */
            result.setOutcome(AsyncApiOutcome.PUBLISH_FAILED)
            result.setErrorMessage(describeFailure(reply))
            result.stopping = true
            return false
        }

        val outcome = record(reply, result)
        handleTargets(fv, action, result, outcome, index)

        return true
    }

    /**
     * Copy what the driver reported onto the result, and say what it amounts to.
     */
    private fun record(reply: AsyncApiReplyDto, result: AsyncApiCallResult): AsyncApiOutcome {

        /*
            Every flag here may be absent: they are boxed so that a driver which did not set one
            can be told from a driver that set it to false. What was not said is read as not
            having happened.
         */
        val outcome = when {
            reply.replyExpected != true -> AsyncApiOutcome.PUBLISHED
            reply.replyReceived == true -> AsyncApiOutcome.REPLIED
            else -> AsyncApiOutcome.NO_REPLY
        }

        result.setOutcome(outcome)
        reply.waitedMs?.let { result.setWaitedMs(it) }

        if (outcome == AsyncApiOutcome.REPLIED) {
            reply.replyPayload?.let { result.setReplyPayload(it) }
            /*
                Only when the driver actually checked. A driver that does not track correlation
                says nothing here, which must not be recorded as the service having failed to
                echo the id back.
             */
            reply.correlationMatched?.let { result.setCorrelationMatched(it) }
        }

        return outcome
    }

    private fun handleTargets(
        fv: FitnessValue,
        action: AsyncApiAction,
        result: AsyncApiCallResult,
        outcome: AsyncApiOutcome,
        index: Int
    ) {

        val name = action.getName()

        fv.updateTarget(idMapper.handleLocalTarget(getOutcomeTargetId(outcome, name)), 1.0, index)

        when (outcome) {

            AsyncApiOutcome.REPLIED -> handleReplyTargets(fv, action, result, index)

            AsyncApiOutcome.NO_REPLY ->
                handleFault(fv, result, ExperimentalFaultCategory.ASYNCAPI_NO_REPLY, name, index)

            /*
                Nothing more to aim at. PUBLISH_FAILED never reaches here -- publishing gives up
                before this is called -- but the compiler wants every outcome named.
             */
            AsyncApiOutcome.PUBLISHED, AsyncApiOutcome.PUBLISH_FAILED -> Unit
        }
    }

    /**
     * Why a message did not go out, as far as can be told from what came back.
     */
    private fun describeFailure(reply: AsyncApiReplyDto?): String {

        if (reply == null) {
            return "No response from the driver"
        }

        return reply.errorMessage
            ?: if (reply.published == null) {
                "The driver did not report whether the message was published"
            } else {
                "The driver could not publish the message"
            }
    }

    /**
     * Register a fault, unless the user has switched off the category it belongs to.
     *
     * It goes both on the fitness value, where the search can aim at it, and on the action
     * result, which is where the reports count faults from.
     */
    private fun handleFault(
        fv: FitnessValue,
        result: AsyncApiCallResult,
        category: ExperimentalFaultCategory,
        actionName: String,
        index: Int
    ) {
        if (!config.isEnabledFaultCategory(category)) {
            return
        }

        val descriptiveId = idMapper.getFaultDescriptiveId(category, actionName)
        fv.updateTarget(idMapper.handleLocalTarget(descriptiveId), 1.0, index)
        result.addFault(DetectedFault(category, actionName, null))
    }

    private fun handleReplyTargets(fv: FitnessValue, action: AsyncApiAction, result: AsyncApiCallResult, index: Int) {

        val name = action.getName()
        val document = asyncApiSampler.document
        val operation = document.operations[action.operationId]

        if (operation == null) {
            LoggingUtil.uniqueUserWarn(
                "No operation '" + action.operationId + "' in the document, so its replies cannot be recognised"
            )
            return
        }
        val declared = document.replyMessagesOf(operation)

        if (declared.isEmpty()) {
            //the contract says a reply comes, but not what it is: nothing to recognise it as
            return
        }

        val payload = result.getReplyPayload()

        if (payload.isNullOrBlank()) {
            /*
                A reply did arrive, but carries no body to match against the contract -- an
                acknowledgement, or a transport that answers in metadata. Nothing was declared
                to be wrong, so this is not the undeclared-reply fault.
             */
            return
        }

        val recognised = AsyncApiReplyClassifier.classify(payload, declared, document.componentSchemas)

        if (recognised == null) {
            handleFault(fv, result, ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY, name, index)
            return
        }

        result.setReplyMessage(recognised.id)
        fv.updateTarget(idMapper.handleLocalTarget(getReplyTargetId(recognised.id, name)), 1.0, index)
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

        dto.address = getAddress(channel, action.channelName)

        dto.payload = action.parameters.firstOrNull { it.name == AsyncApiParam.PAYLOAD }
            ?.gene?.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)
        dto.contentType = message?.contentType ?: document.defaultContentType ?: DEFAULT_CONTENT_TYPE
        dto.headers = LinkedHashMap(buildHeaders(action))

        dto.correlationId = runId + CORRELATION_SEPARATOR + published++
        message?.correlationId?.let {
            dto.correlationLocation = if (it.source == AsyncApiCorrelationId.Source.HEADER) {
                AsyncApiActionDto.CORRELATION_IN_HEADER
            } else {
                AsyncApiActionDto.CORRELATION_IN_PAYLOAD
            }
            dto.correlationPointer = it.pointer
        }

        action.replyTemplate?.let { reply ->
            dto.replyAddress = getReplyAddress(reply, action)
            if (dto.replyAddress != null) {
                dto.replyTimeoutMs = config.asyncApiReplyTimeoutMs.toLong()
            }
        }

        return dto
    }

    /**
     * The headers to publish alongside the body: key is the header name as the document declares
     * it, value is what to send under it, as text.
     *
     * They are read back from the gene's own JSON printing, which is what knows which optional
     * headers are on. A header whose value prints as JSON null is left out rather than sent as
     * the text "null".
     */
    private fun buildHeaders(action: AsyncApiAction): Map<String, String> {

        val headers = LinkedHashMap<String, String>()

        val gene = action.parameters.firstOrNull { it.name == AsyncApiParam.HEADERS }?.gene
            ?: return headers

        val json = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)

        val node = try {
            mapper.readTree(json)
        } catch (e: Exception) {
            log.warn("The headers of '{}' did not print as JSON: {}", action.getName(), e.message)
            return headers
        }

        if (!node.isObject) {
            log.warn("The headers of '{}' printed as {}, not as an object, so none are sent",
                action.getName(), json)
            return headers
        }

        node.fields().forEach { (name, value) ->
            if (!value.isNull) {
                headers[name] = if (value.isValueNode) value.asText() else value.toString()
            }
        }

        return headers
    }

    /**
     * Where the driver should wait for the reply, or null when there is nowhere to wait yet.
     */
    private fun getReplyAddress(reply: AsyncApiReply, action: AsyncApiAction): String? {

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

        return getAddress(asyncApiSampler.document.channels[channelName], channelName)
    }

    /**
     * Where a message on [channel] actually goes on the wire.
     *
     * Usually the channel's address, but a protocol binding may override it: the Kafka binding
     * carries its own topic, and a document that uses one often declares no address at all. A
     * channel that declares neither is decided at run time, so the driver is given the channel's
     * name and left to map it, being the one that knows the broker.
     *
     * Publishing to the wrong destination is silent -- the messages simply reach nobody -- so it
     * is worth taking the binding into account rather than assuming the address is the whole story.
     */
    private fun getAddress(channel: AsyncApiChannel?, channelName: String): String {

        if (channel == null) {
            return channelName
        }

        val protocol = asyncApiSampler.document.serversOf(channel).firstOrNull()?.protocol

        /*
            A channel that declares no address of its own is normal for Kafka, where the topic
            lives in the binding. The protocol is only known when the document declares a server,
            so when there is no address to keep, the binding's topic is taken whatever it says:
            it is the one destination the document actually names.
         */
        val address = channel.effectiveAddress(protocol)
            ?: channel.bindings?.kafkaTopic
            ?: return channelName

        if (address.contains(PARAMETER_OPENING)) {
            /*
                The address has {placeholders} backed by the channel's parameters, which are not
                filled in yet. Publishing to it verbatim reaches nobody.
             */
            LoggingUtil.uniqueUserWarn(
                "The address of channel '" + channelName + "' has parameters that are not supported yet: " + address
            )
        }

        return address
    }
}

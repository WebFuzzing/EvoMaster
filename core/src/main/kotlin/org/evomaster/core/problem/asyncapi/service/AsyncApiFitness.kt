package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.asyncapi.models.AsyncApiChannel
import com.webfuzzing.asyncapi.models.AsyncApiCorrelationId
import com.webfuzzing.asyncapi.models.AsyncApiReply
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.ApiWsFitness
import org.evomaster.core.problem.asyncapi.classifier.AsyncApiEmissionClassifier
import org.evomaster.core.problem.asyncapi.classifier.AsyncApiReplyClassifier
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.asyncapi.param.UpdateForAsyncApiPayloadParam
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceFitnessHandler
import org.evomaster.core.problem.util.ParserDtoUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.taint.TaintAnalysis
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Publishes the messages of a test through the driver and turns what comes back into targets:
 * for every operation what publishing to it did, and which of the replies the contract declares
 * was recognised. In white-box mode, also which declared send addresses the service emitted to
 * while handling the message.
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

        /**
         * Prefix of the targets covered when a reply matched no declared message, written as
         * PREFIX:action.
         */
        private const val UNRECOGNISED_REPLY_TARGET_PREFIX = "ASYNCAPI_UNRECOGNISED_REPLY"

        /**
         * Prefix of the `(declared send address x operation)` targets, covered when handling a
         * message made the service emit to an address a `send` operation declares, written as
         * PREFIX:address:action, with the address as the document writes it.
         */
        private const val EMITTED_TARGET_PREFIX = "ASYNCAPI_EMITTED"

        /**
         * Prefix of the `(declared sent message x operation)` targets, covered when what the
         * service emitted to a declared send address was recognised as one of the messages
         * declared for it, written as PREFIX:messageId:action.
         */
        private const val EMITTED_MESSAGE_TARGET_PREFIX = "ASYNCAPI_EMITTED_MESSAGE"

        /**
         * How many distinct addresses the document does not declare are reported as faults for
         * one operation over a run. An address can be derived from data, e.g. a topic per user,
         * and without a cap the search would be rewarded for just varying that data. The
         * addresses already reported keep being reported.
         */
        private const val MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION = 10

        private const val TARGET_SEPARATOR = ":"

        private const val CORRELATION_SEPARATOR = "-"

        /**
         * Separator inside a JSON Pointer, whose last segment names the field.
         */
        private const val PATH_SEPARATOR = "/"

        /**
         * How a JSON Pointer writes a [PATH_SEPARATOR] that is part of a name.
         */
        private const val ESCAPED_SLASH = "~1"

        /**
         * How a JSON Pointer writes a [TILDE] that is part of a name.
         */
        private const val ESCAPED_TILDE = "~0"

        private const val TILDE = "~"

        /**
         * What a correlation id's header is called when the document declares no field for it.
         */
        private const val DEFAULT_CORRELATION_HEADER = "correlationId"

        /**
         * What the variable holding a reply is called, before the action's index. The same
         * `res_0` every other problem type writes.
         *
         * Named here rather than by the writer because it is part of what a driver is asked to
         * render against, so it has to be settled while the search runs. RPC does the same.
         */
        private const val REPLY_VARIABLE_PREFIX = "res_"

        private const val DEFAULT_CONTENT_TYPE = "application/json"

        /**
         * What a reference from the schema of one parsed class to another starts with, the class
         * name following. Those schemas are OpenAPI ones, so this is OpenAPI's, not AsyncAPI's.
         */
        private const val DTO_SCHEMA_REF_PREFIX = "#/components/schemas/"

        /**
         * What a reference is quoted with in those schemas, which are JSON.
         */
        private const val JSON_QUOTE = "\""

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

        /**
         * The id of the target covered when a reply to [actionName] matched no declared message.
         */
        private fun getUnrecognisedReplyTargetId(actionName: String): String =
            listOf(UNRECOGNISED_REPLY_TARGET_PREFIX, actionName).joinToString(TARGET_SEPARATOR)

        /**
         * The id of the target covered when handling [actionName] made the service emit to the
         * send channel whose address the document writes as [address].
         */
        private fun getEmittedTargetId(address: String, actionName: String): String =
            listOf(EMITTED_TARGET_PREFIX, address, actionName).joinToString(TARGET_SEPARATOR)

        /**
         * The id of the target covered when handling [actionName] made the service emit a
         * message recognised as [messageId], one declared for a send channel.
         */
        private fun getEmittedMessageTargetId(messageId: String, actionName: String): String =
            listOf(EMITTED_MESSAGE_TARGET_PREFIX, messageId, actionName).joinToString(TARGET_SEPARATOR)
    }

    @Inject
    private lateinit var asyncApiSampler: AsyncApiSampler

    /**
     * The HTTP services the SUT calls while it handles a message, mocked with WireMock as for
     * REST, when enabled.
     */
    @Inject
    private lateinit var externalServices: HttpWsExternalServiceFitnessHandler

    /**
     * Tells apart the correlation ids of the messages this run publishes.
     *
     * Drawn from [randomness], so that a run repeated under the same seed publishes the same
     * ids. Lazily, since the generator is injected after this object is built.
     *
     * Keeping a reply from an earlier run from being read as an answer to this one is not this
     * value's job but the driver's, which is the only side that can do it: see
     * `SutController.executeAsyncApiAction`.
     */
    private val runId: String by lazy { Integer.toHexString(randomness.nextInt()) }

    /**
     * How many messages this run has published, which is what makes each correlation id unique.
     */
    private var published = 0L

    /**
     * The undeclared addresses reported as faults: key is the name of the operation whose
     * message was being handled, value is the addresses, in the order first reported, at most
     * [MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION] of them.
     */
    private val undeclaredEmissions = LinkedHashMap<String, LinkedHashSet<String>>()

    /**
     * What the document says about the addresses the service emits to. Lazily, as the document
     * is only there once the sampler has been set up.
     */
    private val emissions: AsyncApiEmissionClassifier by lazy {
        AsyncApiEmissionClassifier(asyncApiSampler.document)
    }

    /**
     * What the driver last said about the service, kept for the schemas of the classes the
     * service read payloads into. Null until first needed; see [sutInfoWithSchemasOf].
     */
    private var sutInfo: SutInfoDto? = null

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

        //what the WireMock instances served over the whole test, for the harvester
        val servedRequests = mutableListOf<HttpExternalServiceRequest>()

        for ((index, action) in actions.withIndex()) {
            val ok = publish(action, index, actionResults, fv, servedRequests)
            if (!ok) {
                break
            }
        }

        /*
            There is no separate black-box fitness here, unlike REST and GraphQL: AsyncAPI publishes
            through the driver in both modes (see EMConfig.usesDriver()). Only in white-box mode is
            the coverage of the SUT read on top of the outcome and reply targets. In black-box mode,
            as for REST, it is not: it would add whatever the driver instruments to the targets,
            and, with the kill switch on, stop the service's threads after every test.
         */
        if (!config.blackBox) {
            val dto = updateFitnessAfterEvaluation(targets, allTargets, fullyCovered, descriptiveIds, individual, fv)
                ?: return null
            handleExtra(dto, fv)

            handleEmittedMessages(fv, actions, dto.additionalInfoList, actionResults.filterIsInstance<AsyncApiCallResult>())

            if (externalServices.isEnabled()) {
                handleExternalServices(individual, dto.additionalInfoList, actionResults, servedRequests)
            }

            if (epc.isInSearch() && config.expandAsyncApiPayloads) {
                //before taint analysis, as REST does: a replacement payload starts out untainted
                expandOpaquePayloads(actions, dto.additionalInfoList, actionResults.filterIsInstance<AsyncApiCallResult>())
            }

            if (epc.isInSearch() && config.isEnabledTaintAnalysis()) {
                /*
                    A tainted string in a payload or a header reaches the service inside the
                    message. The analysis matches what the service reports against the genes of
                    every action, so nested fields are covered, whichever thread consumed it.

                    At most one entry per result, not exactly one as GraphQL asserts: a driver that
                    could not be reached records no entry for its message.

                    Only during the search, as for REST and GraphQL: nothing mutates the genes later.
                 */
                Lazy.assert { dto.additionalInfoList.size <= actionResults.count { it is AsyncApiCallResult } }
                TaintAnalysis.doTaintAnalysis(individual, dto.additionalInfoList, randomness, config)
            }
        }

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
     * Keep on the result of each action the messages the service emitted while it was being
     * handled, as the instrumentation of its broker client recorded them, and turn them into
     * targets.
     *
     * Entry i of [infos] is for action i, as is entry i of [results]; there are never more
     * entries than results.
     *
     * A message that could not be published is skipped: whatever the service emitted while it
     * was being tried was caused by an earlier message, handled late.
     */
    private fun handleEmittedMessages(
        fv: FitnessValue,
        actions: List<AsyncApiAction>,
        infos: List<AdditionalInfoDto>,
        results: List<AsyncApiCallResult>
    ) {
        infos.zip(results).forEachIndexed { index, (info, result) ->
            if (!wasPublished(result)) {
                return@forEachIndexed
            }
            val emitted: List<EmittedMessageDto>? = info.emittedMessages
            if (!emitted.isNullOrEmpty()) {
                result.setEmittedMessages(emitted)
                handleEmittedTargets(fv, actions[index], result, emitted, index)
            }
        }
    }

    /**
     * Whether the message [result] is for reached the wire, so that what the service reported
     * while it was being handled can be credited to it.
     */
    private fun wasPublished(result: AsyncApiCallResult): Boolean =
        result.getOutcome().let { it != null && it != AsyncApiOutcome.PUBLISH_FAILED }

    /**
     * What the messages the service emitted while [action] was being handled say about it.
     *
     * One emitted to an address a `send` operation declares covers a target for that address,
     * and one more for the declared message it was recognised as, if any. One emitted to an
     * address the document does not declare at all is a fault, up to
     * [MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION] per operation.
     *
     * The reply address of [action] is left out, as the reply targets already cover it. The
     * reply addresses of other operations are declared, so a message there is neither a target
     * nor a fault.
     *
     * The address [action] itself publishes to still covers its target, but is left out of what
     * a generated test observes, as the test would collect its own message there.
     */
    private fun handleEmittedTargets(
        fv: FitnessValue,
        action: AsyncApiAction,
        result: AsyncApiCallResult,
        emitted: List<EmittedMessageDto>,
        index: Int
    ) {
        val name = action.getName()
        val replyAddress = getReplyAddressOf(action)
        val ownAddress = getAddress(asyncApiSampler.document.channels[action.channelName], action.channelName)

        //what a generated test observes: the concrete addresses, in the order first emitted to
        val declared = LinkedHashSet<String>()

        emitted.forEach { message ->

            //the client did not say where it went, so there is nothing to tell about it
            val address = message.address ?: return@forEach

            if (address == replyAddress) {
                return@forEach
            }

            val channel = emissions.sendChannelOf(address)

            if (channel != null) {
                if (address != ownAddress) {
                    declared.add(address)
                }
                fv.updateTarget(idMapper.handleLocalTarget(getEmittedTargetId(channel.address, name)), 1.0, index)
                handleEmittedMessageTarget(fv, message, channel, name, index)
                return@forEach
            }

            if (!emissions.isDeclared(address) && isReportedAsUndeclared(name, address)) {
                handleFault(fv, result, ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_EMISSION, name, index, address)
            }
        }

        if (declared.isNotEmpty()) {
            result.setDeclaredEmissionAddresses(declared.toList())
        }
    }

    /**
     * Whether emitting to [address], which the document does not declare, while a message of
     * [actionName] was handled is reported as a fault: it is, if it already was, or if the
     * operation has fewer than [MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION] such addresses
     * so far, in which case [address] becomes one of them.
     */
    private fun isReportedAsUndeclared(actionName: String, address: String): Boolean {

        val reported = undeclaredEmissions.getOrPut(actionName) { LinkedHashSet() }

        if (reported.contains(address)) {
            return true
        }

        if (reported.size >= MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION) {
            LoggingUtil.uniqueWarn(log, "Handling messages of '$actionName', the service emitted to more than" +
                    " $MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION addresses the document does not declare." +
                    " Only the first $MAX_UNDECLARED_EMISSION_ADDRESSES_PER_OPERATION are reported as faults.")
            return false
        }

        reported.add(address)
        return true
    }

    /**
     * Cover the target of the message declared for [channel] that [message] was recognised as,
     * if any. Matching none is not a fault, unlike for a reply: what a service sends is often
     * described loosely, and an object turned into bytes inside the client is only recorded by
     * its class.
     */
    private fun handleEmittedMessageTarget(
        fv: FitnessValue,
        message: EmittedMessageDto,
        channel: AsyncApiEmissionClassifier.SendChannel,
        actionName: String,
        index: Int
    ) {
        val payload = message.payload

        //a cut payload is not JSON any more, and would be judged on its missing end
        if (payload.isNullOrBlank() || message.payloadTruncated == true || channel.messages.isEmpty()) {
            return
        }

        val recognised = AsyncApiReplyClassifier.classify(payload, channel.messages, asyncApiSampler.document.componentSchemas)
            ?: return

        fv.updateTarget(idMapper.handleLocalTarget(getEmittedMessageTargetId(recognised.id, actionName)), 1.0, index)
    }

    /**
     * For every message whose payload the document says nothing about, line up the shape of the
     * class the service was seen reading it into, to replace the payload at the next mutation
     * (see [UpdateForAsyncApiPayloadParam]). The AsyncAPI counterpart of what REST does for a
     * body the schema leaves out.
     *
     * A class is credited to the action the controller had open when the service parsed it.
     * That is only right if the service read the message before the next one was published,
     * which is what a settle window (asyncApiSettleMs) is for; without one, a late read lands on
     * the next message, or on none. The harm is bounded: only an opaque payload is replaced, a
     * wrong class only reaches less, and the archive keeps what reaches more.
     *
     * A class another reported class refers to is not a candidate: the first read of a class
     * also reports the classes of its fields. Among what is left, several reads in one window,
     * one is chosen at random, again at every evaluation, so all get tried. Only object genes
     * are candidates, as an opaque payload is published as a JSON object. A message read as a
     * list of some class is not told apart, and is replaced by one object of it.
     *
     * A message that could not be published is skipped: whatever the service read meanwhile
     * was sent before it.
     */
    private fun expandOpaquePayloads(
        actions: List<AsyncApiAction>,
        additionalInfoList: List<AdditionalInfoDto>,
        results: List<AsyncApiCallResult>
    ) {

        //one entry per action the controller was told about, so fewer when a test stopped early
        for (index in 0 until minOf(actions.size, additionalInfoList.size, results.size)) {

            if (!wasPublished(results[index])) {
                continue
            }

            val action = actions[index]

            val payload = action.parameters
                .firstOrNull { it is AsyncApiParam && it.name == AsyncApiParam.PAYLOAD } as AsyncApiParam?

            if (payload == null || !payload.opaque
                || action.parameters.any { it is UpdateForAsyncApiPayloadParam }) {
                continue
            }

            val reported = additionalInfoList[index].parsedDtoNames

            if (reported.isNullOrEmpty()) {
                continue
            }

            val info = sutInfoWithSchemasOf(reported) ?: continue
            val schemas = info.unitsInfoDto.parsedDtos

            //sorted, as the controller reports them in a set of no particular order
            val known = reported.filter { schemas.containsKey(it) }.sorted()

            if (known.size < reported.size) {
                LoggingUtil.uniqueWarn(log, "The driver reported that these classes were parsed, but has no schema" +
                        " for some of them: ${reported.sorted()}")
            }

            val genes = ParserDtoUtil.getOrParseDtoWithSutInfo(info, config.enableSchemaConstraintHandling)

            val outermost = known
                .filter { name -> known.none { other -> other != name && schemas.getValue(other).contains(refTo(name)) } }
                //classes that refer to each other leave none out: any of them may be the message
                .ifEmpty { known }
                .filter { genes[it] is ObjectGene }

            if (outermost.isEmpty()) {
                continue
            }

            val name = if (outermost.size == 1) outermost.first() else randomness.choose(outermost)
            val gene = genes.getValue(name).copy()
            gene.doInitialize(randomness)

            action.addParam(UpdateForAsyncApiPayloadParam(gene))
        }
    }

    /**
     * How the schema of one parsed class refers to the class named [name].
     */
    private fun refTo(name: String) = JSON_QUOTE + DTO_SCHEMA_REF_PREFIX + name + JSON_QUOTE

    /**
     * What the driver says about the service, with the schemas of every class in [names] it has
     * one for, or null if it could not be asked.
     *
     * The driver records the schema of a class when the service first reads one, so it is
     * asked again for a class not in the last fetch. The whole info is fetched, as this happens
     * at most once per class.
     */
    private fun sutInfoWithSchemasOf(names: Collection<String>): SutInfoDto? {

        val known = sutInfo
        if (known != null && names.all { known.unitsInfoDto?.parsedDtos?.containsKey(it) == true }) {
            return known
        }

        val fetched = rc.getSutInfo() ?: return null
        sutInfo = fetched

        return fetched.takeIf { it.unitsInfoDto?.parsedDtos != null }
    }

    /**
     * Act on the HTTP services the SUT was seen calling while the test was executed.
     */
    private fun handleExternalServices(
        individual: AsyncApiIndividual,
        infoDto: List<AdditionalInfoDto>,
        actionResults: List<ActionResult>,
        servedRequests: List<HttpExternalServiceRequest>
    ) {
        val started = externalServices.handleExternalServiceInfo(individual, infoDto)
        if (started) {
            /*
                A WireMock instance was started because of this test, so it ran with that service
                unreachable. Kept out of the archive, as REST does.
             */
            actionResults.forEach { it.deathSentence = true }
        }

        externalServices.harvestActualResponses(servedRequests)
    }

    /**
     * @param servedRequests where to add the requests the WireMock instances served while the
     *                       message was handled
     * @return whether the message reached the wire, so that the test can go on
     */
    private fun publish(
        action: AsyncApiAction,
        index: Int,
        actionResults: MutableList<ActionResult>,
        fv: FitnessValue,
        servedRequests: MutableList<HttpExternalServiceRequest>
    ): Boolean {

        searchTimeController.waitForRateLimiter()

        val result = AsyncApiCallResult(action.getLocalId())
        actionResults.add(result)

        val dto = getActionDto(action, index)
        dto.asyncApiCall = toDto(action, index)

        val mocked = if (externalServices.isEnabled()) externalServices.prepareForMainAction(action) else null

        val reply = rc.executeNewAsyncApiActionAndGetReply(dto)

        if (reply == null || reply.published != true) {
            /*
                Not a finding about the service: the driver could not put the message on the
                wire, could not be reached at all, or did not say. Nothing published after this
                point would mean anything, so the test stops here, and no target is registered.
                Nor is any request the WireMock instances served meanwhile: it was made for an
                earlier message.
             */
            result.setOutcome(AsyncApiOutcome.PUBLISH_FAILED)
            result.setErrorMessage(describeFailure(reply))
            result.stopping = true
            return false
        }

        if (mocked != null) {
            /*
                Read once the driver returns, after the settle window if there is one. A later
                call is credited to the next message, or to none, like the code it covers, so
                without a window a call made on the service's consumer thread is usually missed.
             */
            servedRequests.addAll(externalServices.registerServedRequests(fv, index, mocked))
        }

        val outcome = record(reply, result)
        if (dto.asyncApiCall.replyVariable != null) {
            result.setReplyVariableName(dto.asyncApiCall.replyVariable)
            recordWhatWasPublished(action, dto.asyncApiCall, result)
        }
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
        reply.settledMs?.let { result.setSettledMs(it) }
        reply.settleEndedBy?.let { result.setSettleEndedBy(it) }

        //what the generated test will publish with, when the driver rendered it
        //lines that are all blank are no script: the writer would publish nothing and assert on it
        reply.testScript?.takeIf { lines -> lines.any { it.isNotBlank() } }?.let { result.setTestScript(it) }

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

            AsyncApiOutcome.PUBLISHED -> Unit

            /*
                Publishing gives up before this is called, so getting here means the caller
                changed. There is no `else` branch on purpose: a newly added outcome should fail
                to compile here rather than fall through unnoticed at run time.
             */
            AsyncApiOutcome.PUBLISH_FAILED ->
                throw IllegalStateException("A message that was not published reached target handling")
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
        index: Int,
        context: String? = null
    ) {
        if (!config.isEnabledFaultCategory(category)) {
            return
        }

        //the context tells apart faults of one category on one operation, eg two addresses
        val postfix = if (context == null) actionName else actionName + TARGET_SEPARATOR + context
        val descriptiveId = idMapper.getFaultDescriptiveId(category, postfix)
        fv.updateTarget(idMapper.handleLocalTarget(descriptiveId), 1.0, index)
        result.addFault(DetectedFault(category, actionName, context))
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
            /*
                Registered apart from the fault, and not subject to it being enabled: a reply that
                matches nothing the contract declares is a behaviour worth reaching whether or not
                it is also reported as a fault. Without this the search would have nothing to aim
                at here, since no declared message was matched either.
             */
            fv.updateTarget(idMapper.handleLocalTarget(getUnrecognisedReplyTargetId(name)), 1.0, index)
            handleFault(fv, result, ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY, name, index)
            return
        }

        result.setReplyMessage(recognised.id)
        fv.updateTarget(idMapper.handleLocalTarget(getReplyTargetId(recognised.id, name)), 1.0, index)
    }

    override fun getActionDto(action: Action, index: Int): ActionDto {
        val dto = super.getActionDto(action, index)
        if (externalServices.isEnabled()) {
            externalServices.addMappings(dto, index, action.getRoot() as AsyncApiIndividual)
        }
        return dto
    }

    /**
     * Everything the driver needs to publish the message and wait for its reply, resolved
     * against the document so that the driver never has to read it.
     */
    private fun toDto(action: AsyncApiAction, index: Int): AsyncApiActionDto {

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

        if (config.createTests) {
            /*
                A transport the contract cannot describe is published by lines the driver renders
                while the search runs, so it is told which language to render them in. Not asked
                for when no test will be written, so a driver need not spend time on it.

                SutInfoDto.OutputFormat is the driver's own enum and has no Python, so a Python
                run asks for no script at all. The reply variable is named here either way: the
                core owns that name, whoever writes the lines that assign to it.
             */
            dto.outputFormat = SutInfoDto.OutputFormat.values()
                .firstOrNull { it.name == config.outputFormat.name }

            dto.replyVariable = REPLY_VARIABLE_PREFIX + index
        }

        dto.correlationId = runId + CORRELATION_SEPARATOR + published++
        message?.correlationId?.let {
            dto.correlationLocation = if (it.source == AsyncApiCorrelationId.Source.HEADER) {
                AsyncApiActionDto.CorrelationLocation.HEADER
            } else {
                AsyncApiActionDto.CorrelationLocation.PAYLOAD
            }
            dto.correlationPointer = it.pointer
        }

        action.replyTemplate?.let { reply ->
            dto.replyAddress = getReplyAddress(reply, action)
            if (dto.replyAddress != null) {
                dto.replyTimeoutMs = config.asyncApiReplyTimeoutMs.toLong()
            }
        }

        if (!config.blackBox && config.asyncApiSettleMs > 0) {
            /*
                Only in white-box mode: the window exists so that the code a message reaches is
                credited to it, and a black-box run collects no code coverage to credit.
             */
            dto.settleMs = config.asyncApiSettleMs.toLong()
            dto.completion = when (config.asyncApiCompletion) {
                EMConfig.AsyncApiCompletion.FIXED -> AsyncApiActionDto.Completion.FIXED
                EMConfig.AsyncApiCompletion.COVERAGE_STABLE -> {
                    dto.stableMs = config.asyncApiStableMs.toLong()
                    AsyncApiActionDto.Completion.COVERAGE_STABLE
                }
                EMConfig.AsyncApiCompletion.HANDLER_RETURN -> AsyncApiActionDto.Completion.HANDLER_RETURN
                EMConfig.AsyncApiCompletion.CONSUMER_LAG -> AsyncApiActionDto.Completion.CONSUMER_LAG
            }
        }

        return dto
    }

    /**
     * Keep on the result what a generated test needs to publish this message again, so that the
     * writer does not resolve the document a second time. Only when a test will be written.
     */
    private fun recordWhatWasPublished(
        action: AsyncApiAction,
        call: AsyncApiActionDto,
        result: AsyncApiCallResult
    ) {
        val document = asyncApiSampler.document
        val channel = document.channels[action.channelName]
        val server = channel?.let { document.serversOf(it).firstOrNull() }

        result.setPublished(
            serverName = server?.name,
            broker = server?.host,
            protocol = server?.protocol,
            address = call.address,
            replyAddress = call.replyAddress,
            payload = call.payload,
            headersAsJson = mapper.writeValueAsString(call.headers ?: emptyMap<String, String>()),
            correlationHeader = correlationHeaderOf(call),
            replyTimeoutMs = call.replyTimeoutMs
        )
    }

    /**
     * The header a correlation id rides in, when it rides in one. The document names the field
     * with a pointer, and its last segment is the header's name, unescaped as JSON Pointer
     * says (`~1` then `~0`). The Kafka driver helper reads it the same way, so a generated test
     * looks for the reply under the same header the search did.
     */
    private fun correlationHeaderOf(call: AsyncApiActionDto): String? {

        if (call.correlationLocation != AsyncApiActionDto.CorrelationLocation.HEADER) {
            return null
        }

        return call.correlationPointer?.substringAfterLast(PATH_SEPARATOR)?.takeIf { it.isNotBlank() }
            ?.replace(ESCAPED_SLASH, PATH_SEPARATOR)?.replace(ESCAPED_TILDE, TILDE)
            ?: DEFAULT_CORRELATION_HEADER
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
     * Where the reply to [action] is awaited, or null when it declares none, or one whose address
     * is only known at run time. The same address [getReplyAddress] gives the driver, without
     * warning again about a reply address announced at run time.
     */
    private fun getReplyAddressOf(action: AsyncApiAction): String? {
        val channelName = action.replyTemplate?.channelName ?: return null
        return getAddress(asyncApiSampler.document.channels[channelName], channelName)
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

        val address = AsyncApiEmissionClassifier.wireAddressOf(asyncApiSampler.document, channel)
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

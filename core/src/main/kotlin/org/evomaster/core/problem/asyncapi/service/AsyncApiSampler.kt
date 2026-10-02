package org.evomaster.core.problem.asyncapi.service

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import com.webfuzzing.asyncapi.parser.AsyncApiParsingException
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.AsyncApiProblemDto
import org.evomaster.core.AnsiColor
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.asyncapi.builder.AsyncApiActionBuilder
import org.evomaster.core.problem.asyncapi.builder.AsyncApiGeneBuilder
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.warning.GeneralWarning
import org.evomaster.core.search.warning.WarningCategory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Samples test cases for an AsyncAPI service: sequences of messages to publish, drawn from the
 * operations the document declares.
 *
 * The same sampler serves white-box and black-box mode. The document always arrives through the
 * driver, since the driver is what holds the connection to the broker (see
 * [org.evomaster.core.EMConfig.usesDriver]).
 */
class AsyncApiSampler : ApiWsSampler<AsyncApiIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AsyncApiSampler::class.java)
    }

    /**
     * One individual per action, each publishing that one message. Sampled before anything
     * else, so that every operation is tried once before the search starts combining them.
     */
    private val adHocInitialIndividuals: MutableList<AsyncApiIndividual> = mutableListOf()

    /**
     * The document the actions were built from. The fitness needs it again at execution time,
     * to resolve addresses and to recognise which declared reply came back.
     */
    lateinit var document: AsyncApiDocument
        private set

    /**
     * Start the service through the driver, read its document, and build one action per
     * publishable message. Anything the parser or the builder had to skip is reported.
     */
    @PostConstruct
    fun initialize() {

        log.debug("Initializing {}", AsyncApiSampler::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
            ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val problem = infoDto.asyncApiProblem
            ?: throw SutProblemException("Missing problem definition object")

        document = readSchema(problem)

        val messages = AsyncApiActionBuilder.addActionsFromSchema(
            document,
            actionCluster,
            AsyncApiGeneBuilder.options(config)
        )
        handleMessages(document.warnings + messages)

        initSqlInfo(infoDto)

        initAdHocInitialIndividuals()

        if (config.seedTestCases) {
            initSeededTests(infoDto)
        }

        updateConfigBasedOnSutInfoDto(infoDto)

        log.debug("Done initializing {}", AsyncApiSampler::class.simpleName)
    }

    /**
     * The document, from wherever the driver said it is. The driver guarantees exactly one of
     * the two is given.
     */
    private fun readSchema(problem: AsyncApiProblemDto): AsyncApiDocument {

        try {
            if (!problem.schemaLocation.isNullOrBlank()) {
                return AsyncApiAccess.getAsyncApiFromLocation(problem.schemaLocation)
            }
            if (!problem.schemaText.isNullOrBlank()) {
                return AsyncApiAccess.parseFromText(problem.schemaText)
            }
        } catch (e: AsyncApiParsingException) {
            throw SutProblemException("Cannot read the AsyncAPI document: ${e.message}")
        }

        throw SutProblemException("No info on the AsyncAPI document was provided")
    }

    /*
        TODO Line for line the same as AbstractRestSampler.handleMessages. Once a third sampler
        needs it, hoist it to EnterpriseSampler.
     */
    /**
     * What the parser and the action builder had to skip. Reported to the user, and kept for
     * the final report.
     */
    private fun handleMessages(messages: List<String>) {

        if (messages.isEmpty()) {
            return
        }

        LoggingUtil.getInfoLogger().warn(
            AnsiColor.inRed(
                "There are ${messages.size} detected issues when analyzing the AsyncAPI document." +
                        " These are not necessarily problems in the document, but possible (temporary)" +
                        " limitations of EvoMaster itself."
            )
        )
        messages.forEachIndexed { index, s ->
            LoggingUtil.getInfoLogger().warn(AnsiColor.inYellow("$index: $s"))
            warningsAggregator.addWarning(GeneralWarning(WarningCategory.SCHEMA, s))
        }
    }

    /**
     * A test of one to `maxTestSize` messages, each a random action with fresh genes.
     */
    override fun sampleAtRandom(): AsyncApiIndividual {

        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())
        val actions = (0 until n).map { sampleRandomAction() }.toMutableList()

        return createIndividual(SampleType.RANDOM, actions)
    }

    /*
        TODO Message examples (AsyncApiMessage.getExamples) are parsed and never read. Sampling
        from them some of the time, as REST does with probRestExamples, would start the search
        from payloads the author knows the service accepts.
     */
    /**
     * A copy of one of the action templates, chosen at random, with its genes initialized.
     */
    fun sampleRandomAction(): AsyncApiAction {

        val action = randomness.choose(actionCluster).copy() as AsyncApiAction
        action.doInitialize(randomness)

        return action
    }

    /*
        TODO No AsyncAPI-specific strategy yet, only the single-message individuals and then
        random tests. The one that matters is chaining: a message whose payload needs an id
        that only the reply to a previous message produces.
     */
    /**
     * The next single-message individual while any are left, then a random test.
     */
    override fun smartSample(): AsyncApiIndividual {

        if (adHocInitialIndividuals.isNotEmpty()) {
            return adHocInitialIndividuals.removeAt(adHocInitialIndividuals.size - 1)
        }

        return sampleAtRandom()
    }

    /**
     * Whether single-message individuals are still waiting to be handed out.
     */
    override fun hasSpecialInitForSmartSampler(): Boolean {
        return adHocInitialIndividuals.isNotEmpty() && config.isEnabledSmartSampling()
    }

    /**
     * Prepare the single-message individuals again, so every operation is tried once more.
     */
    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }

    /**
     * Not supported yet: there is no format to read AsyncAPI test cases from.
     */
    override fun initSeededTests(infoDto: SutInfoDto?) {
        //TODO There is no format to read message-based test cases from yet, as Postman is for REST
        throw IllegalStateException("Seeding test cases is not supported for AsyncAPI yet")
    }

    /**
     * One individual per action, publishing that one message.
     */
    private fun initAdHocInitialIndividuals() {

        adHocInitialIndividuals.clear()

        actionCluster.values.forEach { template ->
            val action = template.copy() as AsyncApiAction
            action.doInitialize(randomness)
            adHocInitialIndividuals.add(createIndividual(SampleType.SMART, mutableListOf(action)))
        }
    }

    /**
     * Wrap [actions] as an individual ready for the search: tracked if tracking is on, and
     * with its global state and local ids set.
     *
     * @param sampleType how the individual came to be, which the structure mutators read to
     *                   choose how to change it
     */
    private fun createIndividual(
        sampleType: SampleType,
        actions: MutableList<AsyncApiAction>
    ): AsyncApiIndividual {

        val individual = AsyncApiIndividual(
            sampleType = sampleType,
            actions = actions,
            trackOperator = if (config.trackingEnabled()) this else null,
            index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX
        )
        individual.doGlobalInitialize(searchGlobalState)

        return individual
    }
}

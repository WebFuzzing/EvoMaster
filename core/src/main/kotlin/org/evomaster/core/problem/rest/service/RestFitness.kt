package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.SearchTimeController
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestFitness : AbstractRestFitness<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitness::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var sampler: RestSampler

    @Inject
    private lateinit var extraHeuristicsLogger: ExtraHeuristicsLogger

    @Inject
    private lateinit var searchTimeController: SearchTimeController


    private val client: Client = {
        val configuration = ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
                .property(ClientProperties.READ_TIMEOUT, 10_000)
                //workaround bug in Jersey client
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
        ClientBuilder.newClient(configuration)
    }.invoke()

    private lateinit var infoDto: SutInfoDto


    @PostConstruct
    private fun initialize() {

        log.debug("Initializing {}", RestFitness::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        log.debug("Done initializing {}", RestFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            rc.stopSUT()
            initialize()
        } catch (e: Exception) {
            log.warn("Failed to re-initialize the SUT: $e")
            return false
        }

        return true
    }

    override fun doCalculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        doInitializingActions(individual)

        individual.enforceCoherence()

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            rc.registerNewAction(i)
            val a = individual.seeActions()[i]

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(
                archive.notCoveredTargets().filter { !IdMapper.isLocal(it) },
                100).toSet()

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.seeActions().toMutableList(), actionResults)

        expandIndividual(individual, dto.additionalInfoList)

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults)

        /*
            TODO when dealing with seeding, might want to extend EvaluatedIndividual
            to keep track of AdditionalInfo
         */
    }

    private fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {
        if (configuration.heuristicsForSQL) {

            for (i in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[i]

                //TODO handling of toMaximize as well
                //TODO refactoring when will have other heuristics besides for SQL

                extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

                val toMinimize = extra.heuristics
                        .filter {
                            it != null
                                    && it.objective == HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                        }.map { it.value }
                        .toList()

                if (!toMinimize.isEmpty()) {
                    fv.setExtraToMinimize(i, toMinimize)
                }

                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }

            fv.aggregateDatabaseData()

            if(!fv.getViewOfAggregatedFailedWhere().isEmpty()) {
                searchTimeController.newIndividualsWithSqlFailedWhere()
            }
        }
    }

    /**
     * Based on what executed by the test, we might need to add new genes to the individual.
     * This for example can happen if we detected that the test is using headers or query
     * params that were not specified in the Swagger schema
     */
    override fun expandIndividual(
            individual: RestIndividual,
            additionalInfoList: List<AdditionalInfoDto>
    ) {

        if (individual.seeActions().size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
            log.warn("Length mismatch between ${individual.seeActions().size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in 0 until additionalInfoList.size) {

            val action = individual.seeActions()[i]
            val info = additionalInfoList[i]

            if (action !is RestCallAction) {
                continue
            }

            /*
                Those are OptionalGenes, which MUST be off by default.
                We are changing the genotype, but MUST not change the phenotype.
                Otherwise, the fitness value we just computed would be wrong.
             */

            info.headers
                    .filter { name ->
                        !action.parameters.any { it is HeaderParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        action.parameters.add(HeaderParam(it, OptionalGene(it, StringGene(it), false)))
                    }

            info.queryParameters
                    .filter { name ->
                        !action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach { name ->
                        action.parameters.add(QueryParam(name, OptionalGene(name, StringGene(name), false)))
                    }
        }
    }

    override fun doInitializingActions(ind: RestIndividual) {

        if (ind.dbInitialization.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return
        }

        val dto = DbActionTransformer.transform(ind.dbInitialization)

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            //this can happen if we do not handle all constraints
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
        }
    }

    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}
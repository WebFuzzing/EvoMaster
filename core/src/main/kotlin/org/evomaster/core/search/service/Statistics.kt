package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct


class Statistics : SearchListener {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Statistics::class.java)

        private const val DESCRIPTION_TARGET = "description"
        private const val TEST_INDEX = "indexOfTests"

        const val TEST_TIMEOUTS = "testTimeouts"
        const val DISTINCT_ACTIONS = "distinctActions"
        const val COVERED_2XX = "covered2xx"
        const val GQL_NO_ERRORS = "gqlNoErrors"
        const val LAST_ACTION_IMPROVEMENT = "lastActionImprovement";
        const val EVALUATED_ACTIONS = "evaluatedActions"
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var archive: Archive<*>

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject(optional = true)
    private var sampler: Sampler<*>? = null

    @Inject(optional = true)
    private var remoteController: RemoteController? = null

    @Inject
    private lateinit var oracles: PartialOracles

    /**
     * How often test executions did timeout
     */
    private var timeouts = 0

    /**
     * How often it was not possible to compute coverage for a test
     */
    private var coverageFailures = 0


   class Pair(val header: String, val element: String)


    /**
     * List, in chronological order, of statistics snapshots.
     * A snapshot could be taken for example every 5% of search
     * budget evaluations
     */
    private val snapshots: MutableMap<Double, List<Pair>> = mutableMapOf()

    private var snapshotThreshold = -1.0

    @PostConstruct
    private fun postConstruct() {
        snapshotThreshold = config.snapshotInterval
        time.addListener(this)
    }

    fun writeStatistics(solution: Solution<*>) {

        val data = getData(solution)
        val headers = data.map { it.header }.joinToString(",")
        val elements = data.map { it.element }.joinToString(",")

        val path = Paths.get(config.statisticsFile).toAbsolutePath()

        Files.createDirectories(path.parent)

        if (!Files.exists(path) or !config.appendToStatisticsFile) {
            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText("$headers\n")
        }

        path.toFile().appendText("$elements\n")
    }

    fun writeSnapshot() {
        if (snapshotThreshold <= 100) {
            if (snapshotThreshold + config.snapshotInterval < 100) {
                log.warn("Calling collection of snapshots too early: $snapshotThreshold")
            } else {
                //this happens if interval is not a divider of 100
                takeSnapshot()
            }
        }

        val headers = "interval," + snapshots.values.first().map { it.header }.joinToString(",")

        val path = Paths.get(config.snapshotStatisticsFile).toAbsolutePath()

        Files.createDirectories(path.parent)

        if (!Files.exists(path) or !config.appendToStatisticsFile) {
            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText("$headers\n")
        }

        snapshots.entries.stream()
                .sorted { o1, o2 -> o1.key.compareTo(o2.key) }
                .forEach {
                    val elements = it.value.map { it.element }.joinToString(",")
                    path.toFile().appendText("${it.key},$elements\n")
                }
    }


    fun reportTimeout() {
        timeouts++
    }

    fun reportCoverageFailure() {
        coverageFailures++
    }

    override fun newActionEvaluated() {
        if (snapshotThreshold <= 0) {
            //not collecting snapshot data
            return
        }

        val elapsed = 100 * time.percentageUsedBudget()

        if (elapsed > snapshotThreshold) {
            takeSnapshot()
        }
    }

    private fun takeSnapshot() {

        val solution = archive.extractSolution()

        val snap = getData(solution)

        val key = if (snapshotThreshold <= 100) snapshotThreshold else 100.0

        snapshots[key] = snap

        //next step
        snapshotThreshold += config.snapshotInterval
    }

    fun getData(solution: Solution<*>): List<Pair> {

        val sutInfo : SutInfoDto? = if(!config.blackBox || config.bbExperiments) {
            remoteController?.getSutInfo()
        } else {
            null
        }

        val unitsInfo = sutInfo?.unitsInfoDto
        val bootTimeInfo = sutInfo?.bootTimeInfoDto

        val targetsInfo = solution.overall.unionWithBootTimeCoveredTargets(null, idMapper, bootTimeInfo)
        val linesInfo = solution.overall.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, bootTimeInfo)
        val branchesInfo = solution.overall.unionWithBootTimeCoveredTargets(ObjectiveNaming.BRANCH, idMapper, bootTimeInfo)

        val rpcInfo = sutInfo?.rpcProblem

        val list: MutableList<Pair> = mutableListOf()

        list.apply {
            add(Pair("evaluatedTests", "" + time.evaluatedIndividuals))
            add(Pair("individualsWithSqlFailedWhere", "" + time.individualsWithSqlFailedWhere))
            add(Pair(EVALUATED_ACTIONS, "" + time.evaluatedActions))
            add(Pair("elapsedSeconds", "" + time.getElapsedSeconds()))
            add(Pair("generatedTests", "" + solution.individuals.size))
            add(Pair("generatedTestTotalSize", "" + solution.individuals.map{ it.individual.size()}.sum()))
            add(Pair("coveredTargets", "" + targetsInfo.total))
            add(Pair(LAST_ACTION_IMPROVEMENT, "" + time.lastActionImprovement))
            add(Pair(DISTINCT_ACTIONS, "" + distinctActions()))
            add(Pair("endpoints", "" + distinctActions()))
            add(Pair(COVERED_2XX, "" + covered2xxEndpoints(solution)))
            add(Pair(GQL_NO_ERRORS, "" + solution.overall.gqlNoErrors(idMapper).size))
            add(Pair("gqlErrors", "" + solution.overall.gqlErrors(idMapper, withLine = false).size))
            add(Pair("gqlErrorsPerLines", "" + solution.overall.gqlErrors(idMapper, withLine = true).size))
            // Statistics on faults found
            // errors5xx - counting only the number of endpoints with 5xx, and NOT last executed line
            add(Pair("errors5xx", "" + errors5xx(solution)))
            //distinct500Faults - counts the number of 500 (and NOT the other in 5xx), per endpoint, and distinct based on the last
            //executed line
            add(Pair("distinct500Faults", "" + solution.overall.potential500Faults(idMapper).size ))
            // failedOracleExpectations - the number of calls in the individual that fail one active partial oracle.
            // However, 5xx are not counted here.
            add(Pair("failedOracleExpectations", "" + failedOracle(solution)))
            /**
             * this is the total of all potential faults, eg distinct500Faults + failedOracleExpectations + any other
             * for RPC, this comprises internal errors, exceptions (declared and unexpected) and customized service errors
             */
            //potential oracle we are going to introduce.
            //Note: that 500 (and 5xx in general) MUST not be counted in failedOracles
            add(Pair("potentialFaults", "" + solution.overall.potentialFoundFaults(idMapper).size))

            // RPC statistics of sut and seeded tests
            add(Pair("numberOfRPCInterfaces", "${rpcInfo?.schemas?.size?:0}"))
            add(Pair("numberOfRPCFunctions", "${rpcInfo?.schemas?.sumOf { it.skippedEndpoints?.size ?: 0 }}"))
            add(Pair("numberOfRPCSeededTests", "${rpcInfo?.seededTestDtos?.size?:0}" ))

            // RPC
            add(Pair("rpcUnexpectedException", "" + solution.overall.rpcUnexpectedException(idMapper).size))
            add(Pair("rpcDeclaredException", "" + solution.overall.rpcDeclaredException(idMapper).size))
            add(Pair("rpcException", "" + solution.overall.rpcException(idMapper).size))
            add(Pair("rpcInternalError", "" + solution.overall.rpcInternalError(idMapper).size))
            add(Pair("rpcHandled", "" + solution.overall.rpcHandled(idMapper).size))
            add(Pair("rpcHandledAndSuccess", "" + solution.overall.rpcHandledAndSuccess(idMapper).size))
            add(Pair("rpcHandledButError", "" + solution.overall.rpcHandledButError(idMapper).size))
            add(Pair("rpcSpecifiedServiceError", "" + solution.overall.rpcServiceError(idMapper).size))

            add(Pair("numberOfBranches", "" + (unitsInfo?.numberOfBranches ?: 0)))
            add(Pair("numberOfLines", "" + (unitsInfo?.numberOfLines ?: 0)))
            add(Pair("numberOfReplacedMethodsInSut", "" + (unitsInfo?.numberOfReplacedMethodsInSut ?: 0)))
            add(Pair("numberOfReplacedMethodsInThirdParty", "" + (unitsInfo?.numberOfReplacedMethodsInThirdParty ?: 0)))
            add(Pair("numberOfTrackedMethods", "" + (unitsInfo?.numberOfTrackedMethods ?: 0)))
            add(Pair("numberOfInstrumentedNumberComparisons", "" + (unitsInfo?.numberOfInstrumentedNumberComparisons ?: 0)))
            add(Pair("numberOfUnits", "" + (unitsInfo?.unitNames?.size ?: 0)))

            add(Pair("coveredLines", "${linesInfo.total}"))
            add(Pair("coveredBranches", "${branchesInfo.total}"))

            // statistic info during sut boot time
            add(Pair("bootTimeCoveredTargets", "${targetsInfo.bootTime}"))
            add(Pair("bootTimeCoveredLines", "${linesInfo.bootTime}"))
            add(Pair("bootTimeCoveredBranches", "${branchesInfo.bootTime}"))

            // statistic info during search
            add(Pair("searchTimeCoveredTargets", "${targetsInfo.searchTime}"))
            add(Pair("searchTimeCoveredLines", "${linesInfo.searchTime}"))
            add(Pair("searchTimeCoveredBranches", "${branchesInfo.searchTime}"))

            // statistic info with seeded tests
            add(Pair("notExecutedSeededTests", "${sampler?.numberOfNotExecutedSeededIndividuals()?:0}"))
            add(Pair("seedingTimeCoveredTargets", "${targetsInfo.seedingTime}"))
            add(Pair("seedingTimeCoveredLines", "${linesInfo.seedingTime}"))
            add(Pair("seedingTimeCoveredBranches", "${branchesInfo.seedingTime}"))


            // statistic info for extractedSpecifiedDtos
            add(Pair("numOfExtractedSpecifiedDtos", "${unitsInfo?.extractedSpecifiedDtos?.size?:0}"))

            val codes = codes(solution)
            add(Pair("avgReturnCodes", "" + codes.average()))
            add(Pair("maxReturnCodes", "" + codes.maxOrNull()))

            add(Pair(TEST_TIMEOUTS, "$timeouts"))
            add(Pair("coverageFailures", "$coverageFailures"))
            add(Pair("clusteringTime", "${solution.clusteringTime}"))
            add(Pair("id", config.statisticsColumnId))
        }
        addConfig(list)

        return list
    }

    private fun distinctActions() : Int {
        if(sampler == null){
            return 0
        }
        return sampler!!.numberOfDistinctActions()
    }


    private fun addConfig(list: MutableList<Pair>) {

        val properties = EMConfig.getConfigurationProperties()
        properties.forEach { p ->
            list.add(Pair(p.name, p.getter.call(config).toString()))
        }
    }

    private fun errors5xx(solution: Solution<*>): Int {

        //count the distinct number of API paths for which we have a 5xx
        return solution.individuals
                .flatMap { it.evaluatedMainActions() }
                .filter {
                    it.result is HttpWsCallResult && (it.result as HttpWsCallResult).hasErrorCode()
                }
                .map { it.action.getName() }
                .distinct()
                .count()
    }

    private fun failedOracle(solution: Solution<*>): Int {

        //count the distinct number of API paths for which we have a failed oracle
        // NOTE: calls with an error code (5xx) are excluded from this count.
        return solution.individuals
                .flatMap { it.evaluatedMainActions() }
                .filter {
                    it.result is HttpWsCallResult
                            && it.action is RestCallAction
                            && !(it.result as HttpWsCallResult).hasErrorCode()
                            && oracles.activeOracles(it.action as RestCallAction, it.result as HttpWsCallResult).any { or -> or.value }
                }
                .map { it.action.getName() }
                .distinct()
                .count()
    }

    private fun covered2xxEndpoints(solution: Solution<*>) : Int {

        //count the distinct number of API paths for which we have a 2xx
        return solution.individuals
                .flatMap { it.evaluatedMainActions() }
                .filter {
                    it.result is HttpWsCallResult && (it.result as HttpWsCallResult).getStatusCode()?.let { c -> c in 200..299 } ?: false
                }
                .map { it.action.getName() }
                .distinct()
                .count()
    }

    private fun codes(solution: Solution<*>): List<Int> {

        return solution.individuals
                .flatMap { it.evaluatedMainActions() }
                .filter { it.result is HttpWsCallResult }
                .map { it.action.getName() }
                .distinct() //distinct names of actions, ie VERB:PATH
                .map { name ->
                    solution.individuals
                            .flatMap { it.evaluatedMainActions() }
                            .filter { it.action.getName() == name }
                            .map { (it.result as HttpWsCallResult).getStatusCode() }
                            .distinct()
                            .count()
                }
    }

    fun writeCoveredTargets(solution: Solution<*>, format : EMConfig.SortCoveredTargetBy){
        val path = Paths.get(config.coveredTargetFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.exists(path))
            log.info("The existing file on ${config.coveredTargetFile} is going to be replaced")
        val separator = "," // for csv format
        val content = mutableListOf<String>()

        when(format){
            EMConfig.SortCoveredTargetBy.NAME ->{
                content.add(DESCRIPTION_TARGET)
            }
            EMConfig.SortCoveredTargetBy.TEST ->{
                content.add(listOf(TEST_INDEX, DESCRIPTION_TARGET).joinToString(separator))
            }
        }

        if (archive.anyTargetsCoveredSeededTests()){
            content.addAll(getPrintContentForCoveredTargets(archive.exportCoveredTargetsAsPair(solution, true), separator, format))
            content.add(System.lineSeparator())
            content.add(System.lineSeparator())
            content.addAll(getPrintContentForCoveredTargets(archive.exportCoveredTargetsAsPair(solution, false), separator, format))
        }else{
            content.addAll(getPrintContentForCoveredTargets(archive.exportCoveredTargetsAsPair(solution), separator, format))
        }


        // append boot-time targets
        if(!config.blackBox || config.bbExperiments) {
            remoteController?.getSutInfo()?.bootTimeInfoDto?.targets?.map { it.descriptiveId }?.sorted()?.apply {
                if (isNotEmpty()){
                    content.add(System.lineSeparator())
                    content.addAll(this)
                }
            }
        }
        Files.write(path, content)
    }

    private fun getPrintContentForCoveredTargets(info: List<kotlin.Pair<String, List<Int>>>, separator: String, format : EMConfig.SortCoveredTargetBy) : MutableList<String>{
        val content = mutableListOf<String>()
        when(format){
            EMConfig.SortCoveredTargetBy.NAME ->{
                content.addAll(info.map { it.first }.sorted())
            }
            EMConfig.SortCoveredTargetBy.TEST ->{
                info.flatMap { it.second }.sorted().forEach {test->
                    /*
                        currently, only index of tests are outputted.
                        if there exists names of tests, we might refer to them.
                     */
                    content.addAll(info.filter { it.second.contains(test) }.map { c-> c.first }.sorted().map { listOf(test, it).joinToString(separator) })
                }
            }
        }
        return content
    }
}

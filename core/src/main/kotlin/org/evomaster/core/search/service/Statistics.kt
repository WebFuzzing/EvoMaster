package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.evomaster.core.utils.IncrementalAverage
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
        const val LAST_ACTION_IMPROVEMENT = "lastActionImprovement"
        const val EVALUATED_ACTIONS = "evaluatedActions"
        const val TOTAL_LINES = "numberOfLines"
        const val TOTAL_BRANCHES = "numberOfBranches"
        const val COVERED_LINES = "coveredLines"
        const val COVERED_BRANCHES = "coveredBranches"
        const val ELAPSED_SECONDS = "elapsedSeconds"
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

    @Inject(optional = true)
    private lateinit var aiResponseClassifier: AIResponseClassifier

    @Inject
    private lateinit var epc : ExecutionPhaseController

    @Inject(optional = true)
    private lateinit var callGraphService: CallGraphService

    /**
     * How often test executions did timeout
     */
    private var timeouts = 0

    /**
     * How often it was not possible to compute coverage for a test
     */
    private var coverageFailures = 0

    // sql heuristic evaluation statistics
    private var sqlParsingFailureCount = 0;
    private var sqlHeuristicEvaluationSuccessCount = 0;
    private var sqlHeuristicEvaluationFailureCount = 0;
    private val sqlRowsAverageCalculator = IncrementalAverage()

    // mongo heuristic evaluation statistic
    private var mongoHeuristicEvaluationSuccessCount = 0
    private var mongoHeuristicEvaluationFailureCount = 0
    private val mongoDocumentsAverageCalculator = IncrementalAverage()

    // redis heuristic evaluation statistic
    private var redisHeuristicEvaluationSuccessCount = 0
    private var redisHeuristicEvaluationFailureCount = 0
    private val redisDocumentsAverageCalculator = IncrementalAverage()

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

        // All headers, including AI-related ones
        val headers = "interval," + snapshots.values.first().joinToString(",") { it.header }

        val path = Paths.get(config.snapshotStatisticsFile).toAbsolutePath()
        Files.createDirectories(path.parent)

        if (!Files.exists(path) || !config.appendToStatisticsFile) {
            Files.deleteIfExists(path)
            Files.createFile(path)
            path.toFile().appendText("$headers\n")
        }

        snapshots.entries.stream()
            .sorted { o1, o2 -> o1.key.compareTo(o2.key) }
            .forEach { (key, pairs) ->
                val elements = pairs.joinToString(",") { it.element }
                path.toFile().appendText("$key,$elements\n")
            }
    }


    fun reportTimeout() {
        timeouts++
    }

    fun reportCoverageFailure() {
        coverageFailures++
    }

    fun reportNumberOfEvaluatedRowsForSqlHeuristic(numberOfEvaluatedRows: Int) {
        sqlRowsAverageCalculator.addValue(numberOfEvaluatedRows)
    }

    fun reportNumberOfEvaluatedDocumentsForMongoHeuristic(numberOfEvaluatedDocuments: Int) {
        mongoDocumentsAverageCalculator.addValue(numberOfEvaluatedDocuments)
        mongoDocumentsAverageCalculator.addValue(numberOfEvaluatedDocuments)
    }

    fun reportNumberOfEvaluatedDocumentsForRedisHeuristic(numberOfEvaluatedDocuments: Int) {
        redisDocumentsAverageCalculator.addValue(numberOfEvaluatedDocuments)
    }

    fun reportSqlParsingFailures(numberOfParsingFailures: Int) {
        if (numberOfParsingFailures<0) {
            throw IllegalArgumentException("Invalid number of parsing failures: $numberOfParsingFailures")
        }
        sqlParsingFailureCount++;
    }

    fun reportSqlHeuristicEvaluationSuccess() {
        sqlHeuristicEvaluationSuccessCount++
    }

    fun reportSqlHeuristicEvaluationFailure() {
        sqlHeuristicEvaluationFailureCount++
    }

    fun reportMongoHeuristicEvaluationSuccess() {
        mongoHeuristicEvaluationSuccessCount++
    }

    fun reportMongoHeuristicEvaluationFailure() {
        mongoHeuristicEvaluationFailureCount++
    }

    fun reportRedisHeuristicEvaluationSuccess() {
        redisHeuristicEvaluationSuccessCount++
    }

    fun reportRedisHeuristicEvaluationFailure() {
        redisHeuristicEvaluationFailureCount++
    }

    fun getMongoHeuristicsEvaluationCount(): Int = mongoHeuristicEvaluationSuccessCount + mongoHeuristicEvaluationFailureCount

    fun getSqlHeuristicsEvaluationCount(): Int = sqlHeuristicEvaluationSuccessCount + sqlHeuristicEvaluationFailureCount

    fun averageNumberOfEvaluatedRowsForSqlHeuristics(): Double = sqlRowsAverageCalculator.mean

    fun averageNumberOfEvaluatedDocumentsForMongoHeuristics(): Double = mongoDocumentsAverageCalculator.mean

    fun getRedisHeuristicsEvaluationCount(): Int = redisHeuristicEvaluationSuccessCount + redisHeuristicEvaluationFailureCount

    fun averageNumberOfEvaluatedDocumentsForRedisHeuristics(): Double = redisDocumentsAverageCalculator.mean

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
            add(Pair(ELAPSED_SECONDS, "" + time.getElapsedSeconds()))
            add(Pair("generatedTests", "" + solution.individuals.size))
            add(Pair("generatedTestTotalSize", "" + solution.individuals.map{ it.individual.size()}.sum()))
            add(Pair("coveredTargets", "" + targetsInfo.total))
            add(Pair(LAST_ACTION_IMPROVEMENT, "" + time.lastActionImprovement))
            add(Pair(DISTINCT_ACTIONS, "" + distinctActions()))
            add(Pair("endpoints", "" + distinctActions()))
            add(Pair(COVERED_2XX, "" + covered2xxEndpoints(solution)))
            add(Pair(GQL_NO_ERRORS, "" + solution.overall.gqlNoErrors(idMapper).size))
            add(Pair("gqlErrors", "" + solution.overall.gqlErrors(idMapper).size))
            // Statistics on faults found
            // errors5xx - counting only the number of endpoints with 5xx, and NOT last executed line
            add(Pair("errors5xx", "" + errors5xx(solution)))
            //distinct500Faults - counts the number of 500 (and NOT the other in 5xx), per endpoint, and distinct based on the last
            //executed line
            add(Pair("distinct500Faults", "" + solution.overall.potential500Faults(idMapper).size ))
            /**
             * this is the total of all potential faults, e.g. distinct500Faults + failedOracleExpectations + any other
             * for RPC, this comprises internal errors, exceptions (declared and unexpected) and customized service errors
             */
            add(Pair("potentialFaults", "" + solution.overall.potentialFoundFaults(idMapper).size))
            add(Pair("potentialFaultCategories", "" + solution.distinctDetectedFaultTypes().toList().sorted().joinToString("|")))
            add(Pair("potentialFaultsSummary", solution.detectedFaultsSummary()))

            // RPC statistics of sut and seeded tests
            add(Pair("numberOfRPCInterfaces", "${rpcInfo?.schemas?.size?:0}"))
            add(Pair("numberOfRPCFunctions", "${rpcInfo?.schemas?.sumOf { it.skippedEndpoints?.size ?: 0 }}"))
            add(Pair("numberOfRPCSeededTests", "${rpcInfo?.seededTestDtos?.size?:0}" ))
            add(Pair("numberOfRPCSeededTestsHaveMock", "${rpcInfo?.seededTestDtos?.filter { s-> s.value?.rpcFuctions?.isNotEmpty() == true &&  s.value?.rpcFuctions?.any { a -> a.mockObjectNeeded() } == true}?.size?:0}" ))

            // RPC
            add(Pair("rpcUnexpectedException", "" + solution.overall.rpcUnexpectedException(idMapper).size))
            add(Pair("rpcDeclaredException", "" + solution.overall.rpcDeclaredException(idMapper).size))
            add(Pair("rpcException", "" + solution.overall.rpcException(idMapper).size))
            add(Pair("rpcInternalError", "" + solution.overall.rpcInternalError(idMapper).size))
            add(Pair("rpcHandled", "" + solution.overall.rpcHandled(idMapper).size))
            add(Pair("rpcHandledAndSuccess", "" + solution.overall.rpcHandledAndSuccess(idMapper).size))
            add(Pair("rpcHandledButError", "" + solution.overall.rpcHandledButError(idMapper).size))
            add(Pair("rpcSpecifiedServiceError", "" + solution.overall.rpcServiceError(idMapper).size))

            add(Pair(TOTAL_BRANCHES, "" + (unitsInfo?.numberOfBranches ?: 0)))
            add(Pair(TOTAL_LINES, "" + (unitsInfo?.numberOfLines ?: 0)))
            add(Pair("numberOfReplacedMethodsInSut", "" + (unitsInfo?.numberOfReplacedMethodsInSut ?: 0)))
            add(Pair("numberOfReplacedMethodsInThirdParty", "" + (unitsInfo?.numberOfReplacedMethodsInThirdParty ?: 0)))
            add(Pair("numberOfTrackedMethods", "" + (unitsInfo?.numberOfTrackedMethods ?: 0)))
            add(Pair("numberOfInstrumentedNumberComparisons", "" + (unitsInfo?.numberOfInstrumentedNumberComparisons ?: 0)))
            add(Pair("numberOfUnits", "" + (unitsInfo?.unitNames?.size ?: 0)))

            add(Pair(COVERED_LINES, "${linesInfo.total}"))
            add(Pair(COVERED_BRANCHES, "${branchesInfo.total}"))

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

            // statistics info for Mongo Heuristics
            add(Pair("averageNumberOfEvaluatedDocumentsForMongoHeuristics","${averageNumberOfEvaluatedDocumentsForMongoHeuristics()}"))
            add(Pair("mongoHeuristicsEvaluationCount","${getMongoHeuristicsEvaluationCount()}"))

            // statistics info for SQL Heuristics
            add(Pair("sqlParsingFailureCount","$sqlParsingFailureCount"))
            add(Pair("averageNumberOfEvaluatedRowsForSqlHeuristics","${averageNumberOfEvaluatedRowsForSqlHeuristics()}"))
            add(Pair("sqlHeuristicsEvaluationFailures","$sqlHeuristicEvaluationFailureCount" ))
            add(Pair("sqlHeuristicsEvaluationCount","${getSqlHeuristicsEvaluationCount()}"))

            for(phase in ExecutionPhaseController.Phase.entries){
                add(Pair("phase_${phase.name}", "${epc.getPhaseDurationInSeconds(phase)}"))
            }
        }
        addConfig(list)

        // Adding AI data
        list.addAll(getAIData())

        return list
    }

    // For building AI metric pairs
    fun aiMetricsAsPairs(
        enabled: Boolean,
        type: String,
        accuracy: Double,
        precision: Double,
        sensitivity: Double,
        specificity: Double,
        npv: Double,
        f1: Double,
        mcc: Double,
        updateTimeNs: Long,
        updateCount: Long,
        classifyTimeNs: Long,
        classifyCount: Long,
        repairTimeNs: Long,
        repairCount: Long,
        observed2xxByAIModel: Long,
        observed4xxByAIModel: Long,
        observed3xxByAIModel: Long,
        observed5xxByAIModel: Long,
        observed400ByAIModel: Long,
        maxAccuracy : Double,
        maxPrecision : Double,
        maxSensitivity : Double,
        maxSpecificity : Double,
        maxNpv : Double,
        maxF1Score : Double,
        maxMcc : Double,
    ): List<Pair> = listOf(
        Pair("ai_model_enabled", enabled.toString()),
        Pair("ai_model_type", type),
        Pair("ai_accuracy", "%.4f".format(accuracy)),
        Pair("ai_precision", "%.4f".format(precision)),
        Pair("ai_sensitivity", "%.4f".format(sensitivity)),
        Pair("ai_specificity", "%.4f".format(specificity)),
        Pair("ai_npv", "%.4f".format(npv)),
        Pair("ai_f1Score400", "%.4f".format(f1)),
        Pair("ai_mcc400", "%.4f".format(mcc)),
        // Max metrics among all endpoints
        Pair("ai_max_Accuracy", "%.4f".format(maxAccuracy)),
        Pair("ai_max_Precision", "%.4f".format(maxPrecision)),
        Pair("ai_max_Sensitivity", "%.4f".format(maxSensitivity)),
        Pair("ai_max_Specificity", "%.4f".format(maxSpecificity)),
        Pair("ai_max_Npv", "%.4f".format(maxNpv)),
        Pair("ai_max_F1Score", "%.4f".format(maxF1Score)),
        Pair("ai_max_Mcc", "%.4f".format(maxMcc)),
        // timing in milliseconds
        Pair("ai_update_time_ms", "%.4f".format(updateTimeNs / 1_000_000.0)),
        Pair("ai_update_count", updateCount.toString()),
        Pair("ai_classify_time_ms", "%.4f".format(classifyTimeNs / 1_000_000.0)),
        Pair("ai_classify_count", classifyCount.toString()),
        Pair("ai_repair_time_ms", "%.4f".format(repairTimeNs / 1_000_000.0)),
        Pair("ai_repair_count", repairCount.toString()),
        Pair("observed_2xx_by_ai_model", observed2xxByAIModel.toString()),
        Pair("observed_3xx_by_ai_model", observed3xxByAIModel.toString()),
        Pair("observed_4xx_by_ai_model", observed4xxByAIModel.toString()),
        Pair("observed_5xx_by_ai_model", observed5xxByAIModel.toString()),
        Pair("observed_400_by_ai_model", observed400ByAIModel.toString()),
        )

    fun getAIData(): List<Pair> {
        // AI model is unable
        if (!config.isEnabledAIModelForResponseClassification()) {
            return aiMetricsAsPairs(
                enabled = false,
                type = "NONE",
                accuracy = 0.0,
                precision = 0.0,
                sensitivity = 0.0,
                specificity = 0.0,
                npv = 0.0,
                f1 = 0.0,
                mcc = 0.0,
                updateTimeNs = 0,
                updateCount = 0,
                classifyTimeNs = 0,
                classifyCount = 0,
                repairTimeNs = 0,
                repairCount = 0,
                observed2xxByAIModel = 0,
                observed3xxByAIModel = 0,
                observed4xxByAIModel = 0,
                observed5xxByAIModel = 0,
                observed400ByAIModel = 0,
                maxAccuracy = 0.0,
                maxPrecision = 0.0,
                maxSensitivity = 0.0,
                maxSpecificity = 0.0,
                maxNpv = 0.0,
                maxF1Score = 0.0,
                maxMcc = 0.0,
            )
        }

        // Compute metrics
        val metrics = aiResponseClassifier.viewInnerModel().estimateOverallMetrics()
        val aiStats = aiResponseClassifier.getStats()

        return aiMetricsAsPairs(
            enabled = true,
            type = config.aiModelForResponseClassification.name,
            accuracy = metrics.accuracy,
            precision = metrics.precision400,
            sensitivity = metrics.sensitivity400,
            specificity = metrics.specificity,
            npv = metrics.npv,
            f1 = metrics.f1Score400,
            mcc = metrics.mcc,
            updateTimeNs = aiStats.updateTimeNs,
            updateCount = aiStats.updateCount,
            classifyTimeNs = aiStats.classifyTimeNs,
            classifyCount = aiStats.classifyCount,
            repairTimeNs = aiStats.repairTimeNs,
            repairCount = aiStats.repairCount,
            observed2xxByAIModel = aiStats.observed2xxCount,
            observed3xxByAIModel = aiStats.observed3xxCount,
            observed4xxByAIModel = aiStats.observed4xxCount,
            observed5xxByAIModel = aiStats.observed5xxCount,
            observed400ByAIModel = aiStats.observed400Count,
            maxAccuracy = aiStats.maxAccuracy,
            maxPrecision = aiStats.maxPrecision,
            maxSensitivity = aiStats.maxSensitivity,
            maxSpecificity = aiStats.maxSpecificity,
            maxNpv = aiStats.maxNpv,
            maxF1Score = aiStats.maxF1Score,
            maxMcc = aiStats.maxMcc,
        )
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

//    private fun failedOracle(solution: Solution<*>): Int {
//
//        //count the distinct number of API paths for which we have a failed oracle
//        // NOTE: calls with an error code (5xx) are excluded from this count.
//        return solution.individuals
//                .flatMap { it.evaluatedMainActions() }
//                .filter {
//                    it.result is HttpWsCallResult
//                            && it.action is RestCallAction
//                            && !(it.result as HttpWsCallResult).hasErrorCode()
//                            //&& oracles.activeOracles(it.action as RestCallAction, it.result as HttpWsCallResult).any { or -> or.value }
//                }
//                .map { it.action.getName() }
//                .distinct()
//                .count()
//    }

    private fun covered2xxEndpoints(solution: Solution<*>) : Int {

        //count the distinct number of API paths for which we have a 2xx
        return solution.individuals
                .flatMap { it.evaluatedMainActions() }
                .filter {
                    it.result is HttpWsCallResult && (it.result as HttpWsCallResult).getStatusCode()?.let { c -> c in 200..299 } ?: false
                }
                // in phases like Security we might create calls that do not exist in schema
                .filter{ it.action is RestCallAction && callGraphService.isDeclared(it.action.verb,it.action.path)}
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

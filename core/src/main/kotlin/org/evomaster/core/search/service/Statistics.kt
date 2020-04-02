package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.service.RestSampler
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

    /**
     * How often test executions did timeout
     */
    private var timeouts = 0

    /**
     * How often it was not possible to compute coverage for a test
     */
    private var coverageFailures = 0


    private class Pair(val header: String, val element: String)


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

    private fun getData(solution: Solution<*>): List<Pair> {

        val unitsInfo = remoteController?.getSutInfo()?.unitsInfoDto

        val list: MutableList<Pair> = mutableListOf()

        list.apply {
            add(Pair("evaluatedTests", "" + time.evaluatedIndividuals))
            add(Pair("individualsWithSqlFailedWhere", "" + time.individualsWithSqlFailedWhere))
            add(Pair("evaluatedActions", "" + time.evaluatedActions))
            add(Pair("elapsedSeconds", "" + time.getElapsedSeconds()))
            add(Pair("generatedTests", "" + solution.individuals.size))
            add(Pair("generatedTestTotalSize", "" + solution.individuals.map{ it.individual.size()}.sum()))
            add(Pair("coveredTargets", "" + solution.overall.coveredTargets()))
            add(Pair("lastActionImprovement", "" + time.lastActionImprovement))
            add(Pair("endpoints", "" + numberOfEndpoints()))
            add(Pair("covered2xx", "" + covered2xxEndpoints(solution)))
            add(Pair("errors5xx", "" + errors5xx(solution)))
            add(Pair("potentialFaults", "" + solution.overall.potentialFoundFaults(idMapper).size))

            add(Pair("numberOfBranches", "" + (unitsInfo?.numberOfBranches ?: 0)))
            add(Pair("numberOfLines", "" + (unitsInfo?.numberOfLines ?: 0)))
            add(Pair("numberOfReplacedMethodsInSut", "" + (unitsInfo?.numberOfReplacedMethodsInSut ?: 0)))
            add(Pair("numberOfReplacedMethodsInThirdParty", "" + (unitsInfo?.numberOfReplacedMethodsInThirdParty ?: 0)))
            add(Pair("numberOfTrackedMethods", "" + (unitsInfo?.numberOfTrackedMethods ?: 0)))
            add(Pair("numberOfUnits", "" + (unitsInfo?.unitNames?.size ?: 0)))

            add(Pair("coveredLines", "" + solution.overall.coveredTargets(ObjectiveNaming.LINE, idMapper)))
            add(Pair("coveredBranches", "" + solution.overall.coveredTargets(ObjectiveNaming.BRANCH, idMapper)))

            val codes = codes(solution)
            add(Pair("avgReturnCodes", "" + codes.average()))
            add(Pair("maxReturnCodes", "" + codes.max()))

            add(Pair("testTimeouts", "$timeouts"))
            add(Pair("coverageFailures", "$coverageFailures"))

            add(Pair("ClusteringTime", "${solution.clusteringTime}"))

            add(Pair("id", config.statisticsColumnId))
        }
        addConfig(list)

        return list
    }

    private fun numberOfEndpoints() : Int {
        if(sampler == null || sampler !is RestSampler){
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
                .flatMap { it.evaluatedActions() }
                .filter {
                    it.result is RestCallResult && it.result.hasErrorCode()
                }
                .map { it.action.getName() }
                .distinct()
                .count()
    }

    private fun covered2xxEndpoints(solution: Solution<*>) : Int {

        //count the distinct number of API paths for which we have a 2xx
        return solution.individuals
                .flatMap { it.evaluatedActions() }
                .filter {
                    it.result is RestCallResult && it.result.getStatusCode()?.let { c -> c in 200..299 } ?: false
                }
                .map { it.action.getName() }
                .distinct()
                .count()
    }

    private fun codes(solution: Solution<*>): List<Int> {

        return solution.individuals
                .flatMap { it.evaluatedActions() }
                .filter { it.result is RestCallResult }
                .map { it.action.getName() }
                .distinct() //distinct names of actions, ie VERB:PATH
                .map { name ->
                    solution.individuals
                            .flatMap { it.evaluatedActions() }
                            .filter { it.action.getName() == name }
                            .map { (it.result as RestCallResult).getStatusCode() }
                            .distinct()
                            .count()
                }
    }

    fun writeCoveredTargets(solution: Solution<*>, format : EMConfig.SortCoveredTargetBy){
        val path = Paths.get(config.coveredTargetFile)
        if (path.parent != null) Files.createDirectories(path.parent)
        if (Files.exists(path))
            log.info("The existing file on ${config.coveredTargetFile} is going to be replaced")
        val info = archive.exportCoveredTargetsAsPair(solution)
        val separator = "," // for csv format

        val content = mutableListOf<String>()
        when(format){
            EMConfig.SortCoveredTargetBy.NAME ->{
                content.add(DESCRIPTION_TARGET)
                content.addAll(info.map { it.first }.sorted())
            }
            EMConfig.SortCoveredTargetBy.TEST ->{
                content.add(listOf(TEST_INDEX, DESCRIPTION_TARGET).joinToString(separator))
                info.flatMap { it.second }.sorted().forEach {test->
                    /*
                        currently, only index of tests are outputted.
                        if there exists names of tests, we might refer to them.
                     */
                    content.addAll(info.filter { it.second.contains(test) }.map { c-> c.first }.sorted().map { listOf(test, it).joinToString(separator) })
                }
            }
        }
        Files.write(path, content)
    }

}
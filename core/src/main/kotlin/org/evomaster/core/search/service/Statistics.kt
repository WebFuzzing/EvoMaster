package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.LoggingUtil
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct


class Statistics : SearchListener {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Statistics::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var archive: Archive<*>


    /**
     * How often test executions did timeout
     */
    private var timeouts = 0

    /**
     * How often it was not possible to compute coverage for a test
     */
    private var coverage_failures = 0


    private class Pair(val header: String, val element: String)

    /**
     * We might collect some info from the search, but
     * not only on the final solution, also on the
     * intermediate results
     */
    private class Snapshot(
            val coveredTargets : Int = 0,
            val reachedNonCoveredTargets : Int = 0
    )

    /**
     * List, in chronological order, of statistics snapshots.
     * A snapshot could be taken for example every 5% of search
     * budget evaluations
     */
    private val snapshots: MutableMap<Double,Snapshot> = mutableMapOf()

    private var snapshotThreshold = -1.0

    @PostConstruct
    private fun postConstruct(){
        snapshotThreshold = config.snapshotInterval
        time.addListener(this)
    }

    fun writeStatistics(solution: Solution<*>) {

        val data = getData(solution)
        val headers = data.map { it.header }.joinToString(",")
        val elements = data.map { it.element }.joinToString(",")

        val path = Paths.get(config.statisticsFile).toAbsolutePath()

        Files.createDirectories(path.parent)

        if(Files.exists(path) && config.appendToStatisticsFile){
            path.toFile().appendText("$elements\n")
        } else {
            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText("$headers\n")
            path.toFile().appendText("$elements\n")
        }
    }

    fun writeSnapshot(){
        if(snapshotThreshold <= 100){
            if(snapshotThreshold + config.snapshotInterval < 100){
                log.warn("Calling collection of snapshots too early: $snapshotThreshold")
            } else {
                //this happens if interval is not a divider of 100
                takeSnapshot()
            }
        }

        val properties = EMConfig.getConfigurationProperties()
        val confHeader = properties.map { it.name }.joinToString(",")
        val confValues = properties.map { it.getter.call(config).toString() }.joinToString(",")


        val path = Paths.get(config.snapshotStatisticsFile).toAbsolutePath()

        Files.createDirectories(path.parent)

        if(!Files.exists(path) or ! config.appendToStatisticsFile){
            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText("interval,covered,reachedNonCovered,$confHeader\n")
        }

        snapshots.entries.stream().sorted { o1, o2 ->  o1.key.compareTo(o2.key)}
                .forEach {
                    path.toFile().appendText("${it.key}," +
                            "${it.value.coveredTargets}," +
                            "${it.value.reachedNonCoveredTargets}," +
                            "$confValues\n")
                }
    }


    fun reportTimeout() {
        timeouts++
    }

    fun reportCoverageFailure() {
        coverage_failures++
    }

    override fun newActionEvaluated() {
        if(snapshotThreshold <= 0){
            //not collecting snapshot data
            return
        }

        val elapsed = 100 * time.percentageUsedBudget()

        if(elapsed > snapshotThreshold){
            takeSnapshot()
        }
    }

    private fun takeSnapshot() {

        val snap = Snapshot(
                coveredTargets = archive.numberOfCoveredTargets(),
                reachedNonCoveredTargets = archive.numberOfReachedButNotCoveredTargets()
        )

        val key = if(snapshotThreshold <= 100) snapshotThreshold else 100.0

        snapshots.put(key, snap)

        //next step
        snapshotThreshold += config.snapshotInterval
    }

    private fun getData(solution: Solution<*>): List<Pair> {

        val list: MutableList<Pair> = mutableListOf()

        list.apply {
            add(Pair("evaluatedTests", "" + time.evaluatedIndividuals))
            add(Pair("evaluatedActions", "" + time.evaluatedActions))
            add(Pair("elapsedSeconds", "" + time.getElapsedSeconds()))
            add(Pair("generatedTests", "" + solution.individuals.size))
            add(Pair("coveredTargets", "" + solution.overall.coveredTargets()))
            add(Pair("lastActionImprovement", "" + time.lastActionImprovement))
            add(Pair("errors5xx", "" + errors5xx(solution)))

            val codes = codes(solution)
            add(Pair("avgReturnCodes", "" + codes.average()))
            add(Pair("maxReturnCodes", "" + codes.max()))

            add(Pair("testTimeouts", "$timeouts"))
            add(Pair("coverageFailures", "$coverage_failures"))

            add(Pair("id", config.statisticsColumnId))
        }
        addConfig(list)

        return list
    }


    private fun addConfig(list: MutableList<Pair>){

        val properties = EMConfig.getConfigurationProperties()
        properties.forEach{p ->
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

    private fun codes(solution: Solution<*>): List<Int>{

        return solution.individuals
                .flatMap { it.evaluatedActions() }
                .filter { it.result is RestCallResult }
                .map { it.action.getName() }
                .distinct() //distinct names of actions, ie VERB:PATH
                .map { name ->
                    solution.individuals
                            .flatMap { it.evaluatedActions() }
                            .filter{ it.action.getName() == name}
                            .map { (it.result as RestCallResult).getStatusCode() }
                            .distinct()
                            .count()
                }
    }
}
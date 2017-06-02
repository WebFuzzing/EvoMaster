package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths


class Statistics {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    private class Pair(val header: String, val element: String)


    fun writeStatistics(solution: Solution<*>) {

        val data = getData(solution)
        val headers = data.map { d -> d.header }.joinToString(",")
        val elements = data.map { d -> d.element }.joinToString(",")

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

    private fun getData(solution: Solution<*>): List<Pair> {

        val list: MutableList<Pair> = mutableListOf()

        list.add(Pair("evaluatedTests", "" + time.evaluatedIndividuals))
        list.add(Pair("evaluatedActions", "" + time.evaluatedActions))
        list.add(Pair("elapsedSeconds", "" + time.getElapsedSeconds()))
        list.add(Pair("generatedTests", "" + solution.individuals.size))
        list.add(Pair("coveredTargets", "" + solution.overall.coveredTargets()))
        list.add(Pair("lastActionImprovement", "" + time.lastActionImprovement))
        list.add(Pair("errors5xx", "" + errors5xx(solution)))

        val codes = codes(solution)
        list.add(Pair("avgReturnCodes", "" + codes.average()))
        list.add(Pair("maxReturnCodes", "" + codes.max()))

        list.add(Pair("id", config.statisticsColumnId))
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
                .flatMap { i -> i.evaluatedActions() }
                .filter {
                    ea ->
                    ea.result is RestCallResult && ea.result.hasErrorCode()
                }
                .map {
                    ea -> ea.action.getName()
                }
                .distinct()
                .count()
    }

    private fun codes(solution: Solution<*>): List<Int>{

        return solution.individuals
                .flatMap { i -> i.evaluatedActions() }
                .filter {
                    ea -> ea.result is RestCallResult
                }
                .map { ea -> ea.action.getName() }
                .distinct() //distinct names of actions, ie VERB:PATH
                .map { name ->
                    solution.individuals
                            .flatMap { i -> i.evaluatedActions() }
                            .filter{ ea -> ea.action.getName() == name}
                            .map { ea -> (ea.result as RestCallResult).getStatusCode() }
                            .distinct()
                            .count()
                }
    }
}
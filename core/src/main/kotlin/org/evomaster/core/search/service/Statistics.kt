package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths


class Statistics {

    @Inject
    private lateinit var configuration: EMConfig

    @Inject
    private lateinit var time: SearchTimeController

    class Pair(val header: String, val element: String)


    fun writeStatistics(solution: Solution<*>) {

        val path = Paths.get(configuration.statisticsFile).toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        val data = getData(solution)
        val headers = data.map { d -> d.header }.joinToString(",")
        val elements = data.map { d -> d.element }.joinToString(",")

        path.toFile().appendText("$headers\n")
        path.toFile().appendText("$elements\n")
    }

    internal fun getData(solution: Solution<*>): List<Pair> {

        val list: MutableList<Pair> = mutableListOf()

        list.add(Pair("evaluatedTests", "" + time.evaluatedIndividuals))
        list.add(Pair("generatedTests", "" + solution.individuals.size))
        list.add(Pair("coveredTargets", "" + solution.overall.coveredTargets()))
        list.add(Pair("errors5xx", "" + errors5xx(solution)))

        //TODO reflection on all fields in EMConfig
        list.add(Pair("id", configuration.statisticsColumnId))
        list.add(Pair("algorithm", configuration.algorithm.name))

        return list
    }

    private fun errors5xx(solution: Solution<*>): Int {

        return solution.individuals
                .filter { i ->
                    i.results.any {
                        a ->
                        a is RestCallResult && a.hasErrorCode()
                    }
                }.count()
    }
}
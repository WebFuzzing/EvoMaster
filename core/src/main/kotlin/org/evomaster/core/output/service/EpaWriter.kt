package org.evomaster.core.output.service

import org.evomaster.core.epa.EPA
import org.evomaster.core.epa.Vertex
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionFilter
import java.nio.file.Files
import java.nio.file.Paths

class EpaWriter {
    fun writeEPA(solution: Solution<*>, epaFile: String) {
        val epa = EPA()
        for (i in solution.individuals) {
            var previousVertex: Vertex? = null
            var currentVertex: Vertex
            // notInitialized = it's the first action (no db handling previously)
            val notInitialized = i.individual.seeInitializingActions().isEmpty() && i.individual.seeActions(ActionFilter.ONLY_SQL).isEmpty()
            val restCallResults = i.seeResults(i.individual.seeMainExecutableActions()).filterIsInstance<RestCallResult>()
            for (rcr in restCallResults) {
                rcr.getPreviousEnabledEndpoints()?.let {
                    previousVertex = epa.createOrGetVertex(it, notInitialized)
                }
                val enabled = rcr.getEnabledEndpointsAfterAction()
                if (enabled?.enabledRestActions != null) {
                    currentVertex = epa.createOrGetVertex(enabled.enabledRestActions)
                    previousVertex?.let { epa.addDirectedEdge(it, currentVertex, enabled.associatedRestAction)}
                    previousVertex = currentVertex
                } else {
                    break // because of failed http requests we are missing one or more necessary elements to build the epa
                }
            }
        }
        writeToFile(epa, epaFile)
    }

    private fun writeToFile(epa: EPA, epaFile: String) {
        var path = Paths.get(epaFile).toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(epa.toDOT())

        path = Paths.get(epaFile+".txt").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(epa.adjacencyMap.toString())
    }
}
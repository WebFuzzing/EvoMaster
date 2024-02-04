package org.evomaster.core.output.service

import org.evomaster.core.epa.EPA
import org.evomaster.core.epa.Vertex
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths

class EpaWriter {
    fun writeEPA(solution: Solution<*>, epaFile: String) {
        val epa = EPA()
        for (i in solution.individuals) {
            var previousVertex: Vertex? = null
            var currentVertex: Vertex
            val restCallResults = i.getRestCallResults()
            for (rcr in restCallResults) {
                rcr.getPreviousEnabledEndpoints()?.let {
                    previousVertex = epa.createOrGetVertex(it, rcr.getIsInitialAction())
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
package org.evomaster.core.output.service

import org.evomaster.core.epa.EPA
import org.evomaster.core.epa.Vertex
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths

class EpaWriter {
    fun writeEPA(solution: Solution<*>) {
        val epa = EPA()
        for (i in solution.individuals) {
            var previousVertex = Vertex(false, 0, "")
            var currentVertex: Vertex
            val restCallResults = i.getRestCallResults()
            for (rcr in restCallResults) {
                rcr.getInitialEnabledEndpoints()?.let {
                    previousVertex = epa.createOrGetVertex(it, true)
                }
                val code = rcr.getStatusCode()
                if (code != null && code < 400) { // we only want what happens after the action if it is a valid action
                    //we could also add a check if the action just executed is enabled?
                    val enabled = rcr.getEnabledEndpointsAfterAction()
                    if (enabled?.enabledRestActions != null && enabled.associatedRestAction != null) {
                        currentVertex = epa.createOrGetVertex(enabled.enabledRestActions)
                        epa.addDirectedEdge(previousVertex, currentVertex, enabled.associatedRestAction)
                        previousVertex = currentVertex
                    } else {
                        break // because of failed http requests we are missing one or more necessary elements to build the epa
                    }
                }
            }
        }
        writeToFile(epa)
    }

    private fun writeToFile(epa: EPA) {
        val path = Paths.get("epa.dot").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(epa.toDOT())
    }
}
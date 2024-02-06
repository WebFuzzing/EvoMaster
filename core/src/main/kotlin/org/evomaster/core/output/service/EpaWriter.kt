package org.evomaster.core.output.service

import org.evomaster.core.epa.EPA
import org.evomaster.core.epa.Vertex
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionFilter
import java.nio.file.Files
import java.nio.file.Paths

class EpaWriter {
    private val graphHeader = "digraph {\n" +
            "beautify=true\n" +
            "graph [pad=\"3\", nodesep=\"4\", ranksep=\"5\"]\n" +
            "node [ margin=0.4 fontname=Helvetica ]\n" +
            "edge [fontname=Courier fontsize=14]\n" +
            "init [shape=box]\n"

    fun writeEPA(solution: Solution<*>, epaFile: String) {
        val epa = EPA()
        for (i in solution.individuals) {
            var previousVertex: Vertex? = null
            var currentVertex: Vertex
            val restCallResults = i.seeResults(i.individual.seeMainExecutableActions())
                .filterIsInstance<RestCallResult>()
            for (rcr in restCallResults) {
                rcr.getPreviousEnabledEndpoints()?.let {
                    // notInitialized = it's the first action (no db handling previously)
                    val notInitialized = i.individual.seeInitializingActions().isEmpty()
                            && i.individual.seeActions(ActionFilter.ONLY_SQL).isEmpty()
                    previousVertex = epa.createOrGetVertex(it, notInitialized)
                }
                val enabled = rcr.getEnabledEndpointsAfterAction()
                if (enabled?.enabledRestActions != null) {
                    currentVertex = epa.createOrGetVertex(enabled.enabledRestActions)
                    previousVertex?.let {
                        epa.addDirectedEdge(it, currentVertex, enabled.associatedRestAction)
                    }
                    previousVertex = currentVertex
                } else {
                    /*
                    * because of failed HTTP requests we are missing necessary
                    * data to build the EPA for the rest of the individual.
                    * */
                    break
                }
            }
        }
        writeToFile(epa, epaFile)
    }

    private fun writeToFile(epa: EPA, epaFile: String) {
        val path = Paths.get(epaFile).toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(toDOT(epa))
    }

    private fun toDOT(epa: EPA): String {
        val sb = StringBuilder()
        sb.append(graphHeader)
        epa.adjacencyMap.forEach { (vertex, edges) ->
            if (vertex.isInitial) {
                sb.append(String.format("init -> \"%s\"\n", vertex.enabledEndpoints))
            }
            edges.forEach {
                val edgeLabel = it.restActions.stream()
                    .map { a -> a.toString() }
                    .reduce { s: String, a: String -> String.format("%s, \\n%s", s, a) }
                if (edgeLabel.isPresent) {
                    sb.append(
                        String.format(
                            "\"%s\" -> \"%s\" [labeldistance=\"0.5\" label=\"%s\"]\n",
                            it.source.enabledEndpoints,
                            it.destination.enabledEndpoints,
                            edgeLabel.get()
                        )
                    )
                } else {
                    sb.append(
                        String.format(
                            "\"%s\" -> \"%s\" \n",
                            it.source.enabledEndpoints,
                            it.destination.enabledEndpoints
                        )
                    )
                }
            }
        }
        sb.append("}")
        return sb.toString()
    }
}
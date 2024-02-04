package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions

class EPA {

    val adjacencyMap = mutableMapOf<Vertex, MutableList<Edge>>()

    private val graphHeader = "digraph {\n" +
            "beautify=true\n" +
            "graph [pad=\"3\", nodesep=\"4\", ranksep=\"5\"]\n" +
            "node [ margin=0.4 fontname=Helvetica ]\n" +
            "edge [fontname=Courier fontsize=14]\n" +
            "init [shape=box]\n"

    fun createOrGetVertex(enabledEndpoints: RestActions, isInitial: Boolean = false): Vertex {
        val enabledEndpointsString = enabledEndpoints.toStringForEPA()
        var vertex: Vertex? = getVertex(enabledEndpointsString)
        if (vertex == null) {
            vertex = Vertex(isInitial, adjacencyMap.count(), enabledEndpointsString)
            adjacencyMap[vertex] = mutableListOf()
        }
        return vertex
    }

    fun addDirectedEdge(source: Vertex, destination: Vertex, restAction: RestAction) {
        var edge: Edge? = adjacencyMap[source]?.filter { e -> e.destination == destination}?.getOrNull(0)
        if (edge == null) {
            edge = Edge(source, destination)
            adjacencyMap[source]?.add(edge)
        }
        edge.addRestActionIfNecessary(restAction)
    }

    fun toDOT(): String {
        val sb = StringBuilder()
        sb.append(graphHeader)
        adjacencyMap.forEach { (vertex, edges) ->
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

    private fun getVertex(enabledEndpoints: String): Vertex? {
        return adjacencyMap.keys.filter { v -> v.enabledEndpoints == enabledEndpoints }.getOrNull(0)
    }
}
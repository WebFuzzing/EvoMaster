package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions

class EPA {

    val adjacencyMap = mutableMapOf<Vertex, MutableList<Edge>>()

    fun createOrGetVertex(enabledEndpoints: RestActions, isInitial: Boolean = false): Vertex {
        val enabledEndpointsString = enabledEndpoints.toStringForEPA()
        var vertex: Vertex? = getVertex(enabledEndpointsString)
        if (vertex == null) {
            vertex = Vertex(isInitial, adjacencyMap.count(), enabledEndpointsString)
            adjacencyMap[vertex] = mutableListOf()
        } else if (isInitial && !vertex.isInitial) {
            vertex.isInitial = true
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

    fun getVertexCount() : Int {
        return adjacencyMap.size
    }

    fun getEdgeCount() : Int {
        return adjacencyMap.map{ entry ->
            entry.value.sumOf {
                it.restActions.size
            }
        }.sum()
    }

    private fun getVertex(enabledEndpoints: String): Vertex? {
        return adjacencyMap.keys.filter { v -> v.enabledEndpoints == enabledEndpoints }.getOrNull(0)
    }
}
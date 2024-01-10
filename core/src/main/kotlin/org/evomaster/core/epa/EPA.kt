package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths

class EPA {

    private val adjacencyMap = mutableMapOf<Vertex, ArrayList<Edge>>()

    fun createOrGetVertex(enabledEndpoints: RestActions, isInitial: Boolean = false): Vertex {
        var vertex: Vertex? = getVertex(enabledEndpoints)
        if (vertex == null) {
            vertex = Vertex(isInitial, adjacencyMap.count(), enabledEndpoints)
            adjacencyMap[vertex] = arrayListOf()
        }
        return vertex
    }

    fun addDirectedEdge(source: Vertex, destination: Vertex, restAction: RestAction) {
        var edge: Edge? = adjacencyMap[source]?.filter { e -> e.destination == destination}?.getOrNull(0)
        if (edge == null) {
            edge = Edge(source, destination)
        }
        if (!edge.restActions.contains(restAction)) {
            edge.restActions.plus(restAction)
        }
        adjacencyMap[source]?.add(edge)
    }

    override fun toString(): String {
        return buildString {
            adjacencyMap.forEach { (vertex, edges) ->
                val edgeString = edges.joinToString { it.destination.enabledEndpoints.toString() }
                append("${vertex.enabledEndpoints} -> [$edgeString]\n")
            }
        }
    }
    private fun toDOT(): String {
        val sb = StringBuilder()
        sb.append("digraph { \n")
        adjacencyMap.forEach { (vertex, edges) ->
            if (vertex.isInitial) {
                sb.append("init [shape=box]\n")
                sb.append(String.format("init -> %d\n", vertex.index))
            }
            sb.append(String.format("%d [xlabel=\"%s\"]\n", vertex.index, vertex.enabledEndpoints))
            edges.forEach {
                sb.append(String.format("%d -> %d [xlabel=\"%s\"]\n", vertex.index, it.destination.index, it.restActions))
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun getVertex(enabledEndpoints: RestActions): Vertex? {
        return adjacencyMap.keys.filter { v -> v.enabledEndpoints == enabledEndpoints }.getOrNull(0)
    }

    fun write() {
        val path = Paths.get("epa.txt").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(toDOT())
    }
}
package org.evomaster.core.epa

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions
import org.evomaster.core.EMConfig
import java.nio.file.Files
import java.nio.file.Paths

class EPA {
    @Inject
    private lateinit var config: EMConfig

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
        val edge = Edge(source, destination, restAction)
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

    private fun getVertex(enabledEndpoints: RestActions): Vertex? {
        return adjacencyMap.keys.filter { v -> v.enabledEndpoints == enabledEndpoints }.getOrNull(0)
    }

    fun write() {
        val path = Paths.get(config.outputFolder,"epa.txt").toAbsolutePath()

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(toString())
    }
}
package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction

class Edge(var source: Vertex, var destination: Vertex) {
    var restActions: MutableSet<String> = mutableSetOf()

    fun addRestActionIfNecessary(restAction: RestAction) {
        val restString = restAction.toString()
        if (restString !in restActions) {
            restActions.add(restString)
        }
    }
}

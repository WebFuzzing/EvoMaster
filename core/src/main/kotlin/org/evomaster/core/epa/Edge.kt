package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction

class Edge {
    var source: Vertex
    var destination: Vertex
    var restActions: MutableSet<RestAction>

    constructor(source: Vertex, destination: Vertex, restAction: RestAction) {
        this.source = source
        this.destination = destination
        this.restActions = mutableSetOf()
        this.restActions.plus(restAction)
    }

    constructor(source: Vertex, destination: Vertex) {
        this.source = source
        this.destination = destination
        this.restActions = mutableSetOf()
    }
}

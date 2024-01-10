package org.evomaster.core.epa

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction

data class Edge(val source: Vertex, val destination: Vertex, val restAction: RestAction)

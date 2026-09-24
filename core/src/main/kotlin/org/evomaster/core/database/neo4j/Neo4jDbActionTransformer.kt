package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto

/** Transforms Neo4j actions into the controller's insertion commands. */
object Neo4jDbActionTransformer {

    /**
     * Puts the nodes and relationships of all the actions in one batch. Node ids are assigned in
     * order across the actions, and each relationship refers to the ids of its own action's nodes.
     */
    fun transform(actions: List<Neo4jDbAction>): Neo4jDatabaseCommandsDto {
        val dto = Neo4jDatabaseCommandsDto()
        var nextId = 0L
        actions.forEach { action ->
            val firstNodeId = nextId
            action.nodes.forEach { node ->
                dto.nodes.add(action.toNodeDto(nextId, node))
                nextId++
            }
            action.edges.forEach { edge ->
                dto.edges.add(action.toEdgeDto(edge, firstNodeId))
            }
        }
        return dto
    }
}

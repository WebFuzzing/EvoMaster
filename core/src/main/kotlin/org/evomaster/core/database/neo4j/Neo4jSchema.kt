package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto

/**
 * What the SUT's entity classes say about the nodes of each label: which properties they have and of
 * which type. Neo4j has no schema, so this is the closest thing to one, and it is what lets an inserted
 * node carry every property the SUT expects rather than only those the failed query mentioned.
 */
class Neo4jSchema(entities: List<Neo4jEntityDto>) {

    /**
     * Key -> a label
     *
     * Value -> the entities whose nodes carry that label, in the order the driver reported them
     */
    private val entitiesByLabel: Map<String, List<Neo4jEntityDto>> =
        entities.flatMap { e -> e.labels.map { it to e } }
            .groupBy({ it.first }, { it.second })

    fun isEmpty() = entitiesByLabel.isEmpty()

    /**
     * The properties a node with these labels is expected to have, without duplicates and without the
     * ones whose value is generated, as the SUT never assigns those either.
     */
    fun propertiesFor(labels: List<String>): List<Neo4jEntityPropertyDto> =
        labels.flatMap { entitiesByLabel[it] ?: emptyList() }
            .distinct()
            .flatMap { it.properties }
            .filter { it.isGenerated != true }
            .distinctBy { it.name }
}

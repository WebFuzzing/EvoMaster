package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Neo4jInsertBuilderTest {

    companion object {
        fun node(id: Long, vararg labels: String, properties: List<Neo4jInsertionEntryDto> = emptyList()) =
            Neo4jNodeInsertionDto().also {
                it.id = id
                it.labels = labels.toMutableList()
                it.properties = properties.toMutableList()
            }

        fun edge(type: String?, from: Long, to: Long, properties: List<Neo4jInsertionEntryDto> = emptyList()) =
            Neo4jEdgeInsertionDto().also {
                it.type = type
                it.fromNodeId = from
                it.toNodeId = to
                it.properties = properties.toMutableList()
            }

        fun entry(key: String, type: Neo4jPropertyTypeDto, value: String) = Neo4jInsertionEntryDto(key, type, value)

        /** MATCH (p:Player)-[:HAS_USER]->(u:User {username: 'ana'}) that found nothing. */
        fun playerWithUser(): Neo4jFailedQuery = Neo4jFailedQuery(
            "MATCH (p:Player)-[:HAS_USER]->(u:User {username: \$username}) RETURN p",
            listOf(node(0, "Player"), node(1, "User", properties = listOf(entry("username", Neo4jPropertyTypeDto.STRING, "ana")))),
            listOf(edge("HAS_USER", 0, 1, listOf(entry("since", Neo4jPropertyTypeDto.INTEGER, "2020"))))
        )
    }

    @Test
    fun buildsOneActionPerFailedQueryWithSeededGenes() {
        val actions = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), emptySet())

        val action = actions.single()
        assertEquals(2, action.nodes.size)
        assertEquals(listOf("Player"), action.nodes[0].labels)
        assertEquals(listOf("User"), action.nodes[1].labels)
        val username = action.nodes[1].properties.single()
        assertEquals("username", username.key)
        assertEquals("ana", (username.gene as StringGene).value)

        val edge = action.edges.single()
        assertEquals("HAS_USER", edge.type)
        assertEquals(0, edge.fromIndex)
        assertEquals(1, edge.toIndex)
        assertEquals(2020L, (edge.properties.single().gene as LongGene).value)
        assertEquals(2, action.seeTopGenes().size)
        assertEquals("MATCH (p:Player)-[:HAS_USER]->(u:User {username: \$username}) RETURN p", action.query)
    }

    @Test
    fun mapsEveryPropertyTypeToItsGene() {
        val query = Neo4jFailedQuery("q", listOf(node(0, "N", properties = listOf(
            entry("s", Neo4jPropertyTypeDto.STRING, "text"),
            entry("i", Neo4jPropertyTypeDto.INTEGER, "-7"),
            entry("f", Neo4jPropertyTypeDto.FLOAT, "1.5"),
            entry("b", Neo4jPropertyTypeDto.BOOLEAN, "true")))), emptyList())

        val genes = Neo4jInsertBuilder.buildInsertActions(listOf(query), emptySet()).single().nodes.single().properties.map { it.gene }

        assertEquals("text", (genes[0] as StringGene).value)
        assertEquals(-7L, (genes[1] as LongGene).value)
        assertEquals(1.5, (genes[2] as DoubleGene).value)
        assertTrue((genes[3] as BooleanGene).value)
    }

    @Test
    fun skipsQueriesThatCannotBeInsertedAndDuplicates() {
        val noNodes = Neo4jFailedQuery("q", emptyList(), emptyList())
        val badNumber = Neo4jFailedQuery("q", listOf(node(0, "N", properties = listOf(entry("i", Neo4jPropertyTypeDto.INTEGER, "ten")))), emptyList())
        val untypedEdge = Neo4jFailedQuery("q", listOf(node(0, "A"), node(1, "B")), listOf(edge(null, 0, 1)))
        val danglingEdge = Neo4jFailedQuery("q", listOf(node(0, "A")), listOf(edge("R", 0, 9)))

        val actions = Neo4jInsertBuilder.buildInsertActions(
            listOf(noNodes, badNumber, untypedEdge, danglingEdge, playerWithUser(), playerWithUser()), emptySet())

        assertEquals(1, actions.size)
        assertEquals(listOf("Player"), actions.single().nodes[0].labels)
    }

    @Test
    fun skipsInsertionsTheIndividualAlreadyHas() {
        val existing = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), emptySet()).single()

        val actions = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), setOf(existing.insertionKey()))

        assertTrue(actions.isEmpty())
    }
}

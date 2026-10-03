package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQueryDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jEntityPropertyDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
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

        fun property(name: String, type: Neo4jPropertyTypeDto, id: Boolean = false, generated: Boolean = false,
                     min: Long? = null, max: Long? = null, enumValues: List<String> = emptyList()) =
            Neo4jEntityPropertyDto().also {
                it.name = name; it.type = type; it.isId = id; it.isGenerated = generated
                it.minValue = min; it.maxValue = max; it.enumValues = enumValues.toMutableList()
            }

        fun entity(vararg labels: String, properties: List<Neo4jEntityPropertyDto>) =
            Neo4jEntityDto().also { it.className = "com.foo." + labels[0]; it.labels = labels.toMutableList(); it.properties = properties.toMutableList() }

        /** What arimaa declares for Player and User. */
        fun arimaaSchema() = Neo4jSchema(listOf(
            entity("Player", properties = listOf(
                property("id", Neo4jPropertyTypeDto.INTEGER, id = true),
                property("rating", Neo4jPropertyTypeDto.INTEGER),
                property("gamesPlayed", Neo4jPropertyTypeDto.INTEGER))),
            entity("User", "Account", properties = listOf(
                property("id", Neo4jPropertyTypeDto.INTEGER, id = true, generated = true),
                property("username", Neo4jPropertyTypeDto.STRING),
                property("email", Neo4jPropertyTypeDto.STRING),
                property("active", Neo4jPropertyTypeDto.BOOLEAN),
                property("score", Neo4jPropertyTypeDto.FLOAT)))
        ))

        /** MATCH (p:Player)-[:HAS_USER]->(u:User {username: 'ana'}) that found nothing. */
        fun playerWithUser(): Neo4jFailedQueryDto = Neo4jFailedQueryDto(
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
        val query = Neo4jFailedQueryDto("q", listOf(node(0, "N", properties = listOf(
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
        val noNodes = Neo4jFailedQueryDto("q", emptyList(), emptyList())
        val badNumber = Neo4jFailedQueryDto("q", listOf(node(0, "N", properties = listOf(entry("i", Neo4jPropertyTypeDto.INTEGER, "ten")))), emptyList())
        val untypedEdge = Neo4jFailedQueryDto("q", listOf(node(0, "A"), node(1, "B")), listOf(edge(null, 0, 1)))
        val danglingEdge = Neo4jFailedQueryDto("q", listOf(node(0, "A")), listOf(edge("R", 0, 9)))

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

    @Test
    fun completesTheNodesWithThePropertiesTheSchemaDeclares() {
        val action = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), emptySet(), arimaaSchema()).single()

        val player = action.nodes[0].properties
        assertEquals(listOf("id", "rating", "gamesPlayed"), player.map { it.key })
        assertTrue(player.all { !it.fromQuery && it.gene is LongGene })

        val user = action.nodes[1].properties
        assertEquals(listOf("username", "email", "active", "score"), user.map { it.key })
        assertTrue(user[0].fromQuery)
        assertEquals("ana", (user[0].gene as StringGene).value)
        assertTrue(user.drop(1).all { !it.fromQuery })
        assertTrue(user[1].gene is StringGene)
        assertTrue(user[2].gene is BooleanGene)
        assertTrue(user[3].gene is DoubleGene)

        assertEquals(1, action.edges.single().properties.size)
        assertEquals(8, action.seeTopGenes().size)
        assertEquals(6, action.seeSchemaGenes().size)
    }

    @Test
    fun schemaPropertiesDoNotChangeTheInsertionKey() {
        val bare = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), emptySet()).single()
        val completed = Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), emptySet(), arimaaSchema()).single()

        assertEquals(bare.insertionKey(), completed.insertionKey())
        assertTrue(Neo4jInsertBuilder.buildInsertActions(listOf(playerWithUser()), setOf(completed.insertionKey()), arimaaSchema()).isEmpty())
    }

    @Test
    fun labelsUnknownToTheSchemaAreLeftAsTheQuerySaysAndLabelsAreMerged() {
        val query = Neo4jFailedQueryDto("q", listOf(node(0, "Ghost"), node(1, "Account", "Player")), emptyList())
        val action = Neo4jInsertBuilder.buildInsertActions(listOf(query), emptySet(), arimaaSchema()).single()

        assertTrue(action.nodes[0].properties.isEmpty())
        assertEquals(listOf("username", "email", "active", "score", "id", "rating", "gamesPlayed"),
            action.nodes[1].properties.map { it.key })
    }

    @Test
    fun schemaBoundsAndEnumsShapeTheFreeGenes() {
        val schema = Neo4jSchema(listOf(entity("Adopter", properties = listOf(
            property("id", Neo4jPropertyTypeDto.STRING, id = true),
            property("budget", Neo4jPropertyTypeDto.INTEGER, min = Int.MIN_VALUE.toLong(), max = Int.MAX_VALUE.toLong()),
            property("level", Neo4jPropertyTypeDto.STRING, enumValues = listOf("BRONZE", "GOLD"))))))
        val query = Neo4jFailedQueryDto("q", listOf(node(0, "Adopter", properties = listOf(entry("id", Neo4jPropertyTypeDto.STRING, "a1")))), emptyList())

        val props = Neo4jInsertBuilder.buildInsertActions(listOf(query), emptySet(), schema).single().nodes.single().properties
        assertEquals(listOf("id", "budget", "level"), props.map { it.key })

        val budget = props[1].gene as LongGene
        assertEquals(Int.MIN_VALUE.toLong(), budget.min)
        assertEquals(Int.MAX_VALUE.toLong(), budget.max)

        val level = props[2].gene as EnumGene<*>
        assertEquals(listOf("BRONZE", "GOLD"), level.values)
        assertEquals("BRONZE", props[2].valueAsText())
    }
}

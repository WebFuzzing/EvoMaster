package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Neo4jDbActionTest {

    private fun personKnowsPerson(): Neo4jDbAction = Neo4jDbAction(
        listOf(
            Neo4jNodeTemplate(listOf("Person"), listOf(Neo4jPropertyGene("name", Neo4jPropertyTypeDto.STRING, StringGene("name", "Ana")))),
            Neo4jNodeTemplate(listOf("Person"), emptyList())
        ),
        listOf(Neo4jEdgeTemplate("KNOWS", 0, 1, listOf(Neo4jPropertyGene("since", Neo4jPropertyTypeDto.INTEGER, LongGene("since", 2020L))))),
        "MATCH (a:Person {name: 'Ana'})-[:KNOWS {since: 2020}]->(b:Person) RETURN b"
    )

    @Test
    fun actionExposesItsGenesAndCopiesThemIndependently() {
        val original = personKnowsPerson()

        val copy = original.copy() as Neo4jDbAction
        (copy.nodes[0].properties.single().gene as StringGene).value = "Luis"

        assertEquals("Neo4j_INSERT_Person_Person", original.getName())
        assertEquals(Neo4jDbAction::class.java.name, original.getActionGroupKey())
        assertEquals(2, original.seeTopGenes().size)
        assertSame(original.nodes[0].properties.single().gene, original.seeTopGenes()[0])
        assertSame(original.edges[0].properties.single().gene, original.seeTopGenes()[1])
        assertEquals("Ana", (original.nodes[0].properties.single().gene as StringGene).value)
        assertEquals("Luis", (copy.nodes[0].properties.single().gene as StringGene).value)
        assertEquals(original.query, copy.query)
    }

    @Test
    fun insertionKeyFollowsTheGeneValues() {
        val action = personKnowsPerson()
        val key = action.insertionKey()

        assertEquals(key, personKnowsPerson().insertionKey())
        assertTrue(key.contains("Person"))
        assertTrue(key.contains("name:STRING=Ana"))
        assertTrue(key.contains("e:KNOWS 0->1"))

        (action.nodes[0].properties.single().gene as StringGene).value = "Luis"
        assertNotEquals(key, action.insertionKey())
    }

    @Test
    fun actionResultTracksInsertionOutcome() {
        val result = Neo4jDbActionResult("source")

        assertFalse(result.getInsertExecutionResult())
        result.setInsertExecutionResult(true)

        assertTrue(result.getInsertExecutionResult())
        assertTrue(result.matchedType(personKnowsPerson()))
        assertTrue(result.copy().getInsertExecutionResult())
    }

    @Test
    fun executionAcceptsMissingDto() {
        val dto = Neo4jExecutionsDto()
        dto.failedQueries.add(Neo4jInsertBuilderTest.playerWithUser())

        assertEquals(1, Neo4jExecution.fromDto(dto).failedQueries.size)
        assertTrue(Neo4jExecution.fromDto(null).failedQueries.isEmpty())
    }
}

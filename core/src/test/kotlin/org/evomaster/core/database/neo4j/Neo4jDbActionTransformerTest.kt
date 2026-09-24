package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Neo4jDbActionTransformerTest {

    @Test
    fun nodeIdsAreAssignedAcrossActionsAndEdgesFollowTheirOwnNodes() {
        val first = Neo4jDbAction(
            listOf(Neo4jNodeTemplate(listOf("A"), emptyList()), Neo4jNodeTemplate(listOf("B"), emptyList())),
            listOf(Neo4jEdgeTemplate("R", 0, 1, emptyList())),
            "q1"
        )
        val second = Neo4jDbAction(
            listOf(Neo4jNodeTemplate(listOf("C"), emptyList()), Neo4jNodeTemplate(emptyList(), emptyList())),
            listOf(Neo4jEdgeTemplate("S", 1, 0, emptyList())),
            "q2"
        )

        val dto = Neo4jDbActionTransformer.transform(listOf(first, second))

        assertEquals(listOf(0L, 1L, 2L, 3L), dto.nodes.map { it.id })
        assertEquals(listOf(listOf("A"), listOf("B"), listOf("C"), emptyList()), dto.nodes.map { it.labels })
        assertEquals(2, dto.edges.size)
        assertEquals("R", dto.edges[0].type)
        assertEquals(0L, dto.edges[0].fromNodeId)
        assertEquals(1L, dto.edges[0].toNodeId)
        assertEquals("S", dto.edges[1].type)
        assertEquals(3L, dto.edges[1].fromNodeId)
        assertEquals(2L, dto.edges[1].toNodeId)
    }

    @Test
    fun propertyValuesTravelAsTextWithTheirType() {
        val action = Neo4jDbAction(
            listOf(Neo4jNodeTemplate(listOf("N"), listOf(
                Neo4jPropertyGene("s", Neo4jPropertyTypeDto.STRING, StringGene("s", "it's")),
                Neo4jPropertyGene("i", Neo4jPropertyTypeDto.INTEGER, LongGene("i", 30L)),
                Neo4jPropertyGene("f", Neo4jPropertyTypeDto.FLOAT, DoubleGene("f", 1.5)),
                Neo4jPropertyGene("b", Neo4jPropertyTypeDto.BOOLEAN, BooleanGene("b", false))))),
            emptyList(),
            "q"
        )

        val properties = Neo4jDbActionTransformer.transform(listOf(action)).nodes.single().properties

        assertEquals(listOf("it's", "30", "1.5", "false"), properties.map { it.value })
        assertEquals(
            listOf(Neo4jPropertyTypeDto.STRING, Neo4jPropertyTypeDto.INTEGER, Neo4jPropertyTypeDto.FLOAT, Neo4jPropertyTypeDto.BOOLEAN),
            properties.map { it.type }
        )
    }

    @Test
    fun transformsAnEmptyActionList() {
        val dto = Neo4jDbActionTransformer.transform(emptyList())
        assertTrue(dto.nodes.isEmpty())
        assertTrue(dto.edges.isEmpty())
    }
}

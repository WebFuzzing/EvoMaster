package org.evomaster.core.search.algorithms

import io.mockk.mockk
import io.mockk.verify
import org.evomaster.client.java.controller.api.dto.BranchObjectiveDto
import org.evomaster.client.java.controller.api.dto.ControlDependenceGraphDto
import org.evomaster.client.java.controller.api.dto.DependencyEdgeDto
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BranchDependencyGraphTest {

    private lateinit var idMapper: IdMapper
    private lateinit var graph: BranchDependencyGraph

    @BeforeEach
    fun setUp() {
        idMapper = mockk(relaxed = true)
        graph = BranchDependencyGraph(idMapper)
    }

    @Test
    fun test_addGraphsRegistersObjectivesAndRoots() {
        val dto = createGraph(
            objectives = listOf(1, 2),
            rootIds = listOf(1),
            edges = listOf(1 to 2)
        )

        graph.addGraphs(listOf(dto))

        assertEquals(setOf(1, 2), graph.getAllObjectives())
        assertEquals(setOf(1), graph.getRoots())
        verify { idMapper.addMapping(1, "desc_1") }
        verify { idMapper.addMapping(2, "desc_2") }
    }

    @Test
    fun test_getChildrenReturnsRegisteredEdges() {
        val dto = createGraph(
            objectives = listOf(10, 11, 12),
            rootIds = listOf(10),
            edges = listOf(10 to 11, 10 to 12)
        )

        graph.addGraphs(listOf(dto))

        assertEquals(setOf(11, 12), graph.getChildren(10))
        assertTrue(graph.getChildren(999).isEmpty())
    }

    @Test
    fun test_getAllObjectivesAggregatesAcrossGraphs() {
        val first = createGraph(
            objectives = listOf(1, 2),
            rootIds = listOf(1),
            edges = listOf(1 to 2)
        )
        val second = createGraph(
            objectives = listOf(20, 21),
            rootIds = listOf(20),
            edges = listOf(20 to 21)
        )

        graph.addGraphs(listOf(first, second))

        assertEquals(setOf(1, 2, 20, 21), graph.getAllObjectives())
        assertEquals(setOf(1, 20), graph.getRoots())
    }

    @Test
    fun test_childIsNotRootIfItHasParent() {
        val dto = createGraph(
            objectives = listOf(1, 2),
            rootIds = listOf(1, 2),
            edges = listOf(1 to 2)
        )

        graph.addGraphs(listOf(dto))

        assertEquals(setOf(1), graph.getRoots())
    }

    private fun createGraph(
        objectives: List<Int>,
        rootIds: List<Int>,
        edges: List<Pair<Int, Int>>
    ): ControlDependenceGraphDto {
        val dto = ControlDependenceGraphDto()
        dto.className = "Clazz"
        dto.methodName = "method"

        dto.objectives = objectives.map { id ->
            BranchObjectiveDto().apply {
                this.id = id
                this.descriptiveId = "desc_$id"
            }
        }
        dto.rootObjectiveIds = rootIds
        dto.edges = edges.map { (parent, child) ->
            DependencyEdgeDto().apply {
                parentObjectiveId = parent
                childObjectiveId = child
            }
        }
        return dto
    }
}



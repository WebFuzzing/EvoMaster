package org.evomaster.core.search.algorithms

import io.mockk.every
import io.mockk.mockk
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.client.java.instrumentation.shared.dto.ControlDependenceGraphDto
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MulticriteriaManagerTest {

    private lateinit var archive: Archive<*>
    private lateinit var idMapper: IdMapper
    private lateinit var manager: MulticriteriaManager

    @BeforeEach
    fun setUp() {
        archive = mockk(relaxed = true)
        idMapper = mockk(relaxed = true)
    }

    @Test
    fun test_initializationWithGraphs() {
        val dto = createSimpleGraphDto(rootId = 1, childId = 2)
        val graphs = listOf(dto)
        
        manager = MulticriteriaManager(archive, idMapper)
        manager.addControlDependenceGraphs(graphs)

        val roots = manager.getBranchRoots()
        assertTrue(roots.contains(1))
        assertEquals(1, roots.size)
        
        // Initially, current goals should be the roots
        val currentGoals = manager.getCurrentGoals()
        assertEquals(setOf(1), currentGoals)
    }

    @Test
    fun test_refreshGoals_expandsChildrenWhenParentIsCovered() {
    
        // Graph: 1 -> 2
        val dto = createSimpleGraphDto(rootId = 1, childId = 2)
        manager = MulticriteriaManager(archive, idMapper)
        manager.addControlDependenceGraphs(listOf(dto))

        // Mock archive to say 1 is covered
        every { archive.coveredTargets() } returns setOf(1)
        
        // Mock idMapper to return descriptive IDs if needed (though getCoveredGoals logic uses it mainly for logging/sanity)
        every { idMapper.getDescriptiveId(1) } returns ObjectiveNaming.BRANCH + "_1"

        manager.refreshGoals()
        val goals = manager.getCurrentGoals()

        // Assert
        // Since 1 is covered, it should expand to children of 1 (which is 2).
        assertEquals(setOf(2), goals)
    }

    @Test
    fun test_getCoveredGoals_filtersOutTargetsNotInGraph() {
        // Arrange
        // Graph has only ID 1.
        val dto = createSingleNodeGraphDto(id = 1)
        manager = MulticriteriaManager(archive, idMapper)
        manager.addControlDependenceGraphs(listOf(dto))

        // Archive reports 1 and 99 (ghost ID) as covered
        every { archive.coveredTargets() } returns setOf(1, 99)
        every { idMapper.getDescriptiveId(1) } returns ObjectiveNaming.BRANCH + "_1"
        every { idMapper.getDescriptiveId(99) } returns ObjectiveNaming.BRANCH + "_99"

        // Act
        val covered = manager.getCoveredGoals()

        // Assert
        // Should only contain 1, because 99 is not in the BranchDependencyGraph
        assertTrue(covered.contains(1))
        assertFalse(covered.contains(99))
        assertEquals(1, covered.size)
    }

    // --- Helpers ---

    private fun createSimpleGraphDto(rootId: Int, childId: Int): ControlDependenceGraphDto {
        val dto = ControlDependenceGraphDto()
        dto.className = "TestClass"
        dto.methodName = "testMethod"
        
        val rootObj = ControlDependenceGraphDto.BranchObjectiveDto()
        rootObj.id = rootId
        rootObj.descriptiveId = ObjectiveNaming.BRANCH + "_$rootId"
        
        val childObj = ControlDependenceGraphDto.BranchObjectiveDto()
        childObj.id = childId
        childObj.descriptiveId = ObjectiveNaming.BRANCH + "_$childId"
        
        dto.objectives = listOf(rootObj, childObj)
        dto.rootObjectiveIds = listOf(rootId)
        
        val edge = ControlDependenceGraphDto.DependencyEdgeDto()
        edge.parentObjectiveId = rootId
        edge.childObjectiveId = childId
        
        dto.edges = listOf(edge)
        
        return dto
    }

    private fun createSingleNodeGraphDto(id: Int): ControlDependenceGraphDto {
        val dto = ControlDependenceGraphDto()
        dto.className = "TestClass"
        dto.methodName = "testMethod"
        
        val obj = ControlDependenceGraphDto.BranchObjectiveDto()
        obj.id = id
        obj.descriptiveId = ObjectiveNaming.BRANCH + "_$id"
        
        dto.objectives = listOf(obj)
        dto.rootObjectiveIds = listOf(id)
        dto.edges = emptyList()
        
        return dto
    }
}


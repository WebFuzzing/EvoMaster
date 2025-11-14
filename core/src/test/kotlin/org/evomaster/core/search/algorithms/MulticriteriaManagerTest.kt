package org.evomaster.core.search.algorithms

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.evomaster.client.java.instrumentation.InstrumentationController
import org.evomaster.client.java.instrumentation.cfg.ControlFlowGraph
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.EMConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MulticriteriaManagerTest {

    private lateinit var m: MulticriteriaManager

    @BeforeEach
    fun setup() {
        mockkStatic(InstrumentationController::class)
        every { InstrumentationController.getControlFlowGraphs() } returns emptyList()
        every { InstrumentationController.getAllBranchTargetInfos() } returns emptyList()
        val archive = mockk<Archive<Any>>(relaxed = true)
        val idMapper = mockk<IdMapper>(relaxed = true)
        m = MulticriteriaManager(archive, idMapper, arrayOf(EMConfig.CoverageCriterion.BRANCH))
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun testGetAllCfgsDelegatesToInstrumentationController() {
        val cfgs = listOf(
            ControlFlowGraph("C1", "m1", "()V"),
            ControlFlowGraph("C2", "m2", "(I)I")
        )
        every { InstrumentationController.getControlFlowGraphs() } returns cfgs
        assertEquals(cfgs, m.getAllCfgs())
    }

    // @Test
    // fun testGetCurrentGoalsReturnsLinkedHashSetSnapshotPreservingInsertionOrder() {
    //     val m = newManager()
    //     val field = MulticriteriaManager::class.java.getDeclaredField("currentGoals")
    //     field.isAccessible = true
    //     @Suppress("UNCHECKED_CAST")
    //     val goals = field.get(m) as java.util.LinkedHashSet<Int>
    //     synchronized(goals) {
    //         goals.clear()
    //         goals.add(3)
    //         goals.add(1)
    //         goals.add(4)
    //     }
    //     val snapshot = m.getCurrentGoals()
    //     assertTrue(snapshot is java.util.LinkedHashSet<*>)
    //     assertEquals(listOf(3, 1, 4), snapshot.toList())
    // }

    // @Test
    // fun testGetBranchRootsDelegatesToGraph() {
    //     val m = newManager()
    //     val roots = linkedSetOf(10, 20)
    //     val g = mockk<BranchDependencyGraph>(relaxed = true)
    //     every { g.getRoots() } returns roots

    //     val gf = MulticriteriaManager::class.java.getDeclaredField("branchGraph")
    //     gf.isAccessible = true
    //     gf.set(m, g)

    //     assertEquals(roots, m.getBranchRoots())
    // }
}



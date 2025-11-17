package org.evomaster.core.search.algorithms
import org.evomaster.client.java.instrumentation.cfg.ControlFlowGraph
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MulticriteriaManagerTest {

    @Test
    fun testGetAllCfgsReturnsFromProvider() {
        val cfgs = listOf(
            ControlFlowGraph("C1", "m1", "()V"),
            ControlFlowGraph("C2", "m2", "(I)I")
        )
        val archive = Archive<Any>()
        val idMapper = IdMapper()
        val manager = MulticriteriaManager(
            archive,
            idMapper,
            arrayOf(EMConfig.CoverageCriterion.BRANCH),
            targetsProvider = { emptyList() },
            cfgsProvider = { cfgs }
        )
        assertEquals(cfgs, manager.getAllCfgs())
    }

    @Test
    fun testInitSeedsCurrentGoalsFromRoots() {
        val manager = MulticriteriaManager(
            Archive<Any>(),
            IdMapper(),
            arrayOf(EMConfig.CoverageCriterion.BRANCH),
            targetsProvider = { emptyList() },
            cfgsProvider = { emptyList() }
        )
        assertEquals(emptySet<Int>(), manager.getBranchRoots())
        assertEquals(emptySet<Int>(), manager.getCurrentGoals())
    }

    @Test
    fun testRefreshGoalsFallsBackToAllUncovered() {
        val ids = intArrayOf(10, 20, 30)
        val manager = MulticriteriaManager(
            Archive<Any>(),
            IdMapper(),
            arrayOf(EMConfig.CoverageCriterion.BRANCH),
            targetsProvider = { emptyList() },
            cfgsProvider = { emptyList() },
            idsProvider = { ids }
        )
        manager.refreshGoals()
        assertEquals(ids.toSet(), manager.getCurrentGoals())
    }

    @Test
    fun testGetUncoveredGoalsReturnsIdsMinusCoveredWhenCoveredEmpty() {
        val ids = intArrayOf(1, 2, 3)
        val manager = MulticriteriaManager(
            Archive<Any>(),
            IdMapper(),
            arrayOf(EMConfig.CoverageCriterion.BRANCH),
            targetsProvider = { emptyList() },
            cfgsProvider = { emptyList() },
            idsProvider = { ids }
        )
        assertEquals(ids.toSet(), manager.getUncoveredGoals())
    }

    @Test
    fun testGetCoveredGoalsReturnsEmptyWhenArchiveEmpty() {
        val manager = MulticriteriaManager(
            Archive<Any>(),
            IdMapper(),
            arrayOf(EMConfig.CoverageCriterion.BRANCH),
            targetsProvider = { emptyList() },
            cfgsProvider = { emptyList() }
        )
        assertEquals(emptySet<Int>(), manager.getCoveredGoals())
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



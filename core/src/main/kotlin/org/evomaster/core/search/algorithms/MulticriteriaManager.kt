package org.evomaster.core.search.algorithms

import org.evomaster.client.java.instrumentation.InstrumentationController
import org.evomaster.client.java.instrumentation.TargetInfo
import org.evomaster.client.java.instrumentation.cfg.ControlFlowGraph
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.slf4j.LoggerFactory

/**
 * MultiCriteriaManager (EvoMaster adaptation)
 *
 * Responsibilities:
 * - Maintain the dynamic set of current goals to optimize (numeric target ids).
 * - Expose access to a complete Control Flow Graph (CFG) view collected at instrumentation time.
 * - Build a BranchDependencyGraph once and extend it with criterion-specific dependencies.
 *
 * Supported criteria: BRANCH, LINE, METHOD (structure mirrors EvoSuite; LINE/METHOD are no-ops until targets exist).
 */
class MulticriteriaManager(
    private val archive: Archive<*>,
    private val idMapper: IdMapper,
    private val enabledCriteria: List<Criterion> = listOf(Criterion.BRANCH, Criterion.LINE, Criterion.METHOD)
) {

    enum class Criterion { BRANCH, LINE, METHOD }

    private val log = LoggerFactory.getLogger(MulticriteriaManager::class.java)

    /**
     * Current generation focus set of targets (numeric ids from ObjectiveRecorder mapping).
     */
    private val currentGoals: LinkedHashSet<Int> = LinkedHashSet()

    /**
     * Branch dependency graph (base graph). Extended via criterion adders.
     */
    private var branchGraph: BranchDependencyGraph? = null

    /**
     * Optional extra edges from non-branch criteria (parent -> child).
     * We keep them separate to avoid changing BranchDependencyGraph internals.
     */
    private val extraChildrenByParent: MutableMap<Int, MutableSet<Int>> = LinkedHashMap()
    private val extraParentsByChild: MutableMap<Int, MutableSet<Int>> = LinkedHashMap()
    private val extraRoots: MutableSet<Int> = LinkedHashSet()

    init {
        buildGraphsAndDependencies()
    }

    /**
     * Refresh the current goals using uncovered targets and the dependency graph(s).
     * Seeds with roots ∩ uncovered, then adds uncovered children of covered parents.
     */
    fun refreshGoals() {
        val uncovered: Set<Int> = getUncoveredGoals()
        val covered: Set<Int> = getCoveredGoals()
        val g = requireNotNull(branchGraph) { "BranchDependencyGraph was null; CFG/targets must be available" }

        synchronized(currentGoals) {
            currentGoals.clear()

            val seeded = (g.getRoots() + extraRoots).intersect(uncovered)
            val expanded: MutableSet<Int> = LinkedHashSet()

            fun childrenOf(p: Int): Set<Int> {
                val base = g.getChildren(p)
                val extra = extraChildrenByParent[p] ?: emptySet()
                return if (extra.isEmpty()) base else base + extra
            }

            for (p in covered) {
                for (c in childrenOf(p)) {
                    if (c in uncovered) {
                        expanded.add(c)
                    }
                }
            }

            val next = LinkedHashSet<Int>()
            next.addAll(seeded)
            next.addAll(expanded)
            if (next.isNotEmpty()) {
                currentGoals.addAll(next)
            } else {
                currentGoals.addAll(uncovered)
            }
        }
    }

    /**
     * Snapshot of current goals.
     */
    fun getCurrentGoals(): Set<Int> = synchronized(currentGoals) { LinkedHashSet(currentGoals) }

    /**
     * All CFGs discovered at instrumentation time.
     */
    fun getAllCfgs(): List<ControlFlowGraph> = InstrumentationController.getControlFlowGraphs()

    /**
     * Roots of the branch dependency graph (base roots only).
     */
    fun getBranchRoots(): Set<Int> = branchGraph?.getRoots() ?: emptySet()

    /**
     * (all CFG-derived branch ids) minus (archive-covered branch ids)
     */
    fun getUncoveredGoals(): Set<Int> {
        val all: Set<Int> = InstrumentationController.getAllBranchTargetIds().toSet()
        if (all.isEmpty()) return emptySet()
        val covered: Set<Int> = getCoveredGoals()
        return all.minus(covered)
    }

    /**
     * Use archive as source of covered targets, then keep only Branch targets
     */
    fun getCoveredGoals(): Set<Int> {
        val covered: Set<Int> = archive.coveredTargets()
        if (covered.isEmpty()) return emptySet()
        return covered
            .filter { id ->
                val desc = idMapper.getDescriptiveId(id)
                desc.startsWith(ObjectiveNaming.BRANCH)
            }
            .toSet()
    }

    /**
     * Build the base branch dependency graph and then incorporate criterion-specific dependencies.
     */
    @Synchronized
    private fun buildGraphsAndDependencies() {
        val cfgs = getAllCfgs()
        val targets: List<TargetInfo> = InstrumentationController.getAllBranchTargetInfos()
        val g = BranchDependencyGraph(cfgs, targets)
        g.build()
        branchGraph = g
        log.debug("Built BranchDependencyGraph: roots=${g.getRoots().size}")

        // Extend with additional criteria, mirroring EvoSuite’s switch (subset supported)
        for (c in enabledCriteria) {
            when (c) {
                Criterion.BRANCH -> {
                    // already handled by base graph
                }
                Criterion.LINE -> addDependencies4Line(cfgs)
                Criterion.METHOD -> addDependencies4Methods(cfgs)
            }
        }
    }

    /**
     * LINE criterion dependencies.
     * Note: Until EM exposes line targets as numeric ids, this is a safe no-op.
     * The structure is kept for parity with EvoSuite.
     */
    private fun addDependencies4Line(cfgs: List<ControlFlowGraph>) {
        // Placeholder for future: derive LINE ids from InstrumentationController.getTargetInfos and
        // connect nearest predecessor branch-ids to the line-id on the same method/line.
        // For now, we keep behavior identical to branch-only by doing nothing.
        log.debug("addDependencies4Line: no-op (LINE targets not available)")
    }

    /**
     * METHOD criterion dependencies.
     * Note: EM does not currently emit METHOD targets; keep as no-op while retaining EvoSuite structure.
     */
    private fun addDependencies4Methods(cfgs: List<ControlFlowGraph>) {
        log.debug("addDependencies4Methods: no-op (METHOD targets not available)")
    }

    /**
     * Convenience: log a brief summary of a few CFGs (class/method/counts).
     */
    fun logCfgSummary(limit: Int = 10) {
        val cfgs = getAllCfgs()
        log.debug("CFGs available: ${cfgs.size}")
        cfgs.take(limit).forEach { cfg ->
            log.debug("CFG: ${cfg.className}#${cfg.methodName}${cfg.descriptor} blocks=${cfg.blocksById.size}")
        }
    }

    /**
     * Validate that branch targets can be mapped to CFGs.
     */
    fun validateCfgCompletenessForBranchTargets(maxLogs: Int = 50) {
        val cfgs = getAllCfgs()
        val targets = InstrumentationController.getAllBranchTargetInfos()
        val graph = BranchDependencyGraph(cfgs, targets)
        graph.build()
        log.debug("CFG validation complete: roots=${graph.getRoots().size}")
    }
}



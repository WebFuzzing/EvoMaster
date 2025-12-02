package org.evomaster.core.search.algorithms

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.client.java.instrumentation.shared.dto.ControlDependenceGraphDto
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.logging.LoggingUtil
import java.util.LinkedHashSet

/**
 * MultiCriteriaManager for DynaMOSA
 */
class MulticriteriaManager(
    private val archive: Archive<*>,
    private val idMapper: IdMapper,
) {

    private val log = LoggingUtil.getInfoLogger()

    private val branchGraph: BranchDependencyGraph = BranchDependencyGraph(idMapper)

    /**
     * Current generation focus set of targets (numeric ids from ObjectiveRecorder mapping).
     */
    private val currentGoals: LinkedHashSet<Int> = LinkedHashSet()

    fun addControlDependenceGraphs(cdgs: List<ControlDependenceGraphDto>) {

        if (cdgs.isEmpty()) {
            return
        }
        branchGraph.addGraphs(cdgs)

        currentGoals.addAll(branchGraph.getRoots())
    }

    /**
     * Refresh the current goals using uncovered targets and the dependency graph(s).
     * Seeds with roots ∩ uncovered, then adds uncovered children of covered parents.
     */
    fun refreshGoals() {
        val uncovered: Set<Int> = getUncoveredGoals()
        val covered: Set<Int> = getCoveredGoals()
        currentGoals.clear()

        val branchRoots = branchGraph.getRoots()
        val seeded = branchRoots.intersect(uncovered)
        val expanded: MutableSet<Int> = LinkedHashSet()

        for (p in covered) {
            for (c in branchGraph.getChildren(p)) {
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

    /**
     * Snapshot of current goals.
     */
    fun getCurrentGoals(): Set<Int> = LinkedHashSet(currentGoals)

    /**
     * Roots of the branch dependency graph (base roots only).
     */
    fun getBranchRoots(): Set<Int> = branchGraph.getRoots()

    /**
     * (all CFG-derived branch ids) minus (archive-covered branch ids)
     */
    fun getUncoveredGoals(): Set<Int> {
        val allBranchObjectives = branchGraph.getAllObjectives()
        if (allBranchObjectives.isEmpty()) return emptySet()
        val covered: Set<Int> = getCoveredGoals()
        return allBranchObjectives.minus(covered)
    }

    /**
     * Use archive as source of covered targets, then keep only Branch targets
     */
    fun getCoveredGoals(): Set<Int> {
        // Get Covered Targets from the Archive
        val covered = archive.coveredTargets()
        if (covered.isEmpty()) {
            return emptySet()
        }

        val result = LinkedHashSet<Int>()
        val allObjectives = branchGraph.getAllObjectives()

        for (id in covered) {
            // Consider only the objectives that are part of the branch dependency graph

            if (allObjectives.contains(id)) {
                log.info("Covered target $id is in the branch dependency graph")
                val desc = idMapper.getDescriptiveId(id)
                log.info("Descriptive ID: $desc")
                result.add(id)
            } else {
                // Sanity check: if it's a branch according to its descriptive id but isn't in the branch dependency graph, warn
                val desc = idMapper.getDescriptiveId(id)
                if (desc != null && desc.startsWith(ObjectiveNaming.BRANCH)) {
                    log.warn("Covered target $id ($desc) looks like a Branch but is not in BranchDependencyGraph!")
                }
            }
        }
        return result
    }

}



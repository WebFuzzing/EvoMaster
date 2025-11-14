package org.evomaster.core.search.algorithms

import org.evomaster.client.java.instrumentation.BranchTargetDescriptor
import org.evomaster.client.java.instrumentation.InstrumentationController
import org.evomaster.client.java.instrumentation.cfg.ControlFlowGraph
import org.slf4j.LoggerFactory

/**
 * Branch dependency graph approximating EvoSuite's BranchFitnessGraph:
 * - Vertices: branch-side targets (numeric ids, true/false sides are distinct).
 * - Edges: from nearest predecessor branch basic blocks to current branch target.
 * - Roots: branchless-method entries and branches with no predecessor branches.
 *
 * Dependencies are derived from per-method CFGs. A basic block is considered "branching"
 * if it contains at least one branch instruction index as recorded in the CFG.
 */
class BranchDependencyGraph(
    private val cfgs: List<ControlFlowGraph>,
    private val allTargets: List<org.evomaster.client.java.instrumentation.TargetInfo>
) {

    private val log = LoggerFactory.getLogger(BranchDependencyGraph::class.java)

    private val childrenByParent: MutableMap<Int, MutableSet<Int>> = LinkedHashMap()
    private val parentsByChild: MutableMap<Int, MutableSet<Int>> = LinkedHashMap()
    private val roots: MutableSet<Int> = LinkedHashSet()

    init {
        build()
    }

    /**
     * Build the graph by mapping every branch target to its CFG block, then walking
     * backwards to nearest branching basic blocks to add edges from both sides of
     * those parent branches to the current target.
     */
    private fun build() {
        val mapDescToId: MutableMap<String, Int> = HashMap(allTargets.size)
        allTargets.forEach { ti ->
            if (ti.descriptiveId != null && ti.mappedId != null) {
                mapDescToId[ti.descriptiveId] = ti.mappedId
            }
        }

        // index CFGs per class for faster lookup
        val cfgsByClass: Map<String, List<ControlFlowGraph>> =
            cfgs.groupBy { it.className }

        // filter only branch targets (defensive: rely on parser)
        val branchTargets = allTargets.filter { it.descriptiveId != null }
            .mapNotNull { ti ->
                try {
                    val bd = InstrumentationController.parseBranchDescriptiveId(ti.descriptiveId)
                    Triple(ti.mappedId, bd, ti.descriptiveId)
                } catch (_: Throwable) {
                    null
                }
            }

        // Build child->parents edges
        for ((childId, desc, descrString) in branchTargets) {
            if (childId == null) continue
            val classSlash = desc.classNameDots.replace('.', '/')
            // TODO: Check if we should use the method name to locate the method CFG instead of the line number.
            val methodCfg = locateMethodCfgForLine(cfgsByClass[classSlash], desc.line)
            if (methodCfg == null) {
                log.debug("No CFG found for ${desc.classNameDots} line ${desc.line} (child $childId)")
                // roots.add(childId)
                continue
            }
            val branchInsnIdx = pickBranchInstructionIndex(methodCfg, desc.line, desc.positionInLine)
            if (branchInsnIdx == null) {
                // This should never happen as we should have a branch insn index for every branch target. TODO: check and understand if this actually happens and if we need to fix it.
                log.debug("No branch insn index at position ${desc.positionInLine} on line ${desc.line} (child $childId)")
                // roots.add(childId)
                continue
            }
            val blockId = methodCfg.blockIdForInstructionIndex(branchInsnIdx)
            if (blockId == null) {
                // This should never happen as we should have a block id for every branch insn index. TODO: check and understand if this actually happens and if we need to fix it.
                log.debug("No block id for branch insn index $branchInsnIdx (child $childId)")
                // roots.add(childId)
                continue
            }
            val parentBlocks = findNearestBranchingPredecessors(methodCfg, blockId)
            if (parentBlocks.isEmpty()) {
                log.debug("No parent blocks for branch insn index $branchInsnIdx (child $childId)")
                // roots.add(childId)
                continue
            }
            for (pb in parentBlocks) {
                val parentBranchInsnIdx = findBranchInsnInBlock(methodCfg, pb)
                if (parentBranchInsnIdx == null) {
                    // This should never happen as we should have a branch insn index for every parent block. TODO: check and understand if this actually happens and if we need to fix it.
                    log.debug("No parent branch insn index for parent block $pb (child $childId)")
                    // roots.add(childId)
                    continue
                }
                val parentLine = methodCfg.instructionIndexToLineNumber[parentBranchInsnIdx] ?: continue
                val parentPos = positionOfBranchOnLine(methodCfg, parentLine, parentBranchInsnIdx)
                if (parentPos == null) {
                    // This should never happen as we should have a position for every parent branch insn index. TODO: check and understand if this actually happens and if we need to fix it.
                    log.debug("No parent position for parent branch insn index $parentBranchInsnIdx (child $childId)")
                    // roots.add(childId)
                    continue
                }
                if methodCfg.instructionIndexToOpcode[parentBranchInsnIdx] == null) {
                    // This should never happen as we should have an opcode for every parent branch insn index. TODO: check and understand if this actually happens and if we need to fix it.
                    log.debug("No opcode for parent branch insn index $parentBranchInsnIdx (child $childId)")
                    // roots.add(childId)
                    continue
                }
                val parentClassDots = classSlash.replace('/', '.')
                // build descriptive ids for both sides of the parent
                val parentTrue = org.evomaster.client.java.instrumentation.shared.ObjectiveNaming.branchObjectiveName(
                    parentClassDots, parentLine, parentPos, true, parentOpcode
                )
                val parentFalse = org.evomaster.client.java.instrumentation.shared.ObjectiveNaming.branchObjectiveName(
                    parentClassDots, parentLine, parentPos, false, parentOpcode
                )
                val parentTrueId = mapDescToId[parentTrue]
                val parentFalseId = mapDescToId[parentFalse]
                if (parentTrueId != null) addEdge(parentTrueId, childId)
                if (parentFalseId != null) addEdge(parentFalseId, childId)
            }
        }

        // add as roots those without parents
        parentsByChild.keys.forEach { child ->
            if ((parentsByChild[child] ?: emptySet()).isEmpty()) {
                roots.add(child)
            }
        }
    }

    private fun addEdge(parent: Int, child: Int) {
        // This is a directed edge from the parent to the child.
        // The ids are the ids of the branch targets.
        childrenByParent.computeIfAbsent(parent) { LinkedHashSet() }.add(child)
        parentsByChild.computeIfAbsent(child) { LinkedHashSet() }.add(parent)
    }

    fun getRoots(): Set<Int> = LinkedHashSet(roots)

    fun getChildren(parent: Int): Set<Int> = childrenByParent[parent] ?: emptySet()

    private fun locateMethodCfgForLine(list: List<ControlFlowGraph>?, line: Int): ControlFlowGraph? {
        if (list == null) return null
        return list.firstOrNull { it.getInstructionIndicesForLine(line).isNotEmpty() }
    }

    private fun pickBranchInstructionIndex(cfg: ControlFlowGraph, line: Int, position: Int): Int? {
        val indices = cfg.getBranchInstructionIndicesForLine(line)
        if (position < 0 || position >= indices.size) return null
        return indices[position]
    }

    private fun findBranchInsnInBlock(cfg: ControlFlowGraph, blockId: Int): Int? {
        val block = cfg.blocksById[blockId] ?: return null
        // Prefer the last instruction if it's a branch
        for (idx in block.startInstructionIndex..block.endInstructionIndex) {
            if (cfg.branchInstructionIndices.contains(idx)) {
                // keep scanning to get the last branch inside the block
            }
        }
        var last: Int? = null
        for (idx in block.startInstructionIndex..block.endInstructionIndex) {
            if (cfg.branchInstructionIndices.contains(idx)) {
                last = idx
            }
        }
        return last
    }

    private fun isBranchingBlock(cfg: ControlFlowGraph, blockId: Int): Boolean {
        val block = cfg.blocksById[blockId] ?: return false
        for (i in block.startInstructionIndex..block.endInstructionIndex) {
            if (cfg.branchInstructionIndices.contains(i)) return true
        }
        return false
    }

    private fun findNearestBranchingPredecessors(cfg: ControlFlowGraph, startBlockId: Int): Set<Int> {
        val result: MutableSet<Int> = LinkedHashSet()
        val visited: MutableSet<Int> = HashSet()
        val queue: java.util.ArrayDeque<Int> = java.util.ArrayDeque()
        queue.add(startBlockId)
        visited.add(startBlockId)
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            val block = cfg.blocksById[curr] ?: continue
            val preds = block.predecessorBlockIds
            if (preds.isEmpty()) continue
            for (p in preds) {
                if (visited.contains(p)) continue
                visited.add(p)
                if (isBranchingBlock(cfg, p)) {
                    result.add(p)
                } else {
                    queue.addLast(p)
                }
            }
        }
        return result
    }

    private fun positionOfBranchOnLine(cfg: ControlFlowGraph, line: Int, insnIndex: Int): Int? {
        val list = cfg.getBranchInstructionIndicesForLine(line)
        for ((i, idx) in list.withIndex()) {
            if (idx == insnIndex) return i
        }
        return null
    }
}



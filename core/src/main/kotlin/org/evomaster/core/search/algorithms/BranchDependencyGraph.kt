package org.evomaster.core.search.algorithms

import org.evomaster.client.java.instrumentation.shared.dto.ControlDependenceGraphDto
import org.evomaster.core.search.service.IdMapper
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import org.slf4j.LoggerFactory

/**
 * Lightweight graph that captures parent→child dependencies between branch targets.
 * Nodes are identified by the numeric ids assigned by the instrumentation agent
 */

private val log = LoggerFactory.getLogger(BranchDependencyGraph::class.java)
class BranchDependencyGraph(
    private val idMapper: IdMapper
) {

    private val children: MutableMap<Int, MutableSet<Int>> = LinkedHashMap()
    private val roots: MutableSet<Int> = LinkedHashSet()
    private val objectives: MutableSet<Int> = LinkedHashSet()
    private val parentCounts: MutableMap<Int, Int> = LinkedHashMap()

    fun addGraphs(cdgs: List<ControlDependenceGraphDto>) {
        for (dto in cdgs) {
            // method objectives are the objectives that are specific to the method
            val methodObjectives = registerObjectives(dto)
            // register edges between the objectives
            registerEdges(dto)
            // register roots are the method objectives that are the roots of the graph
            registerRoots(dto, methodObjectives)
        }
    }

    private fun registerObjectives(dto: ControlDependenceGraphDto): Set<Int> {
        val methodObjectives = LinkedHashSet<Int>()

        for (obj in dto.objectives) {
            val id = obj.id
            
            // register the objective
            objectives.add(id)
            // method objectives are the objectives that are specific to the method
            methodObjectives.add(id)


            // register the parent counts.
            // if the objective is not in the parent counts, add it with a count of 0
            // this is used to determine the roots of the graph
            // if the parent counts is zero, then the objective is a root
            if (!parentCounts.containsKey(id)) {
                parentCounts[id] = 0
            }

            // add the descriptive id to the id mapper
            if (obj.descriptiveId != null) {
                idMapper.addMapping(id, obj.descriptiveId)
            }
        }

        return methodObjectives
    }

    private fun registerEdges(dto: ControlDependenceGraphDto) {
        for (edge in dto.edges) {
            // register the parent child relationship
            registerParentChild(edge.parentObjectiveId, edge.childObjectiveId)
        }
    }

    private fun registerParentChild(parentId: Int, childId: Int) {
        // register the parent child relationship
        // If the parent is not in the children map (meaning it is a new parent), add it with an empty set
        val set = children.getOrPut(parentId) { LinkedHashSet() }
        // add the child to the parent's set of children
        set.add(childId)
        // increment the parent count for the child
        val newCount = (parentCounts[childId] ?: 0) + 1
        // update the parent count for the child
        parentCounts[childId] = newCount
        // if the parent count is greater than 0, then the child is not a root, so remove it from the roots set
        if (newCount > 0) {
            roots.remove(childId)
        }
    }

    private fun registerRoots(dto: ControlDependenceGraphDto, methodObjectives: Set<Int>) {
        
        // potential roots are the root objectives that are specific to the method
        val potentialRoots = dto.rootObjectiveIds

        // register the roots, just if they are roots according to the parent counts
        for (root in potentialRoots) {
            if (isRoot(root)) {
                roots.add(root)
            }
        }
    }

    private fun isRoot(id: Int): Boolean = (parentCounts[id] ?: 0) == 0

    fun getRoots(): Set<Int> = LinkedHashSet(roots)

    fun getChildren(parent: Int): Set<Int> = children[parent]?.toSet() ?: emptySet()

    fun getAllObjectives(): Set<Int> = LinkedHashSet(objectives)
}


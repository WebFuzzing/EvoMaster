package org.evomaster.core.search.impact.impactinfocollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils

/**
 * created by manzh on 2020-07-08
 *
 * This is to collect impact regarding specializations of StringGene.
 * The specializations are modified during search,
 * e.g., String A with 1 specialization can be mutated to B with additional 2 specializations,
 * and the same A might be mutated to C with 3 additional specializations.
 * It might be improper to define the evolution like A-B-C or A-C-B.
 * To solve this problem, we defined the Hierarchy structure, like A-B and A-C.
 * In this case, A is defined as the parent of B and C.
 * When retrieving the impacts of a gene, e.g., B, the impacts are impacts of its parent plus the newly added ones.
 *
 * @property parent is to refer its parent that is nullable.
 * @property specializationGeneImpact represents the impacts of specialization or newly added specializations when the parent is not null.
 */
class HierarchySpecializationImpactInfo (
        val parent : HierarchySpecializationImpactInfo? = null,
        val specializationGeneImpact : MutableList<Impact>
) {

    fun next(addedSpecialization : MutableList<Gene>) : HierarchySpecializationImpactInfo{
        if (addedSpecialization.isEmpty()) throw IllegalArgumentException("there is no need to create next hierarchy")
        return HierarchySpecializationImpactInfo(this, addedSpecialization.map { ImpactUtils.createGeneImpact(it, it.name) }.toMutableList())
    }

    fun flattenImpacts() : MutableList<Impact>{
        val list = mutableListOf<Impact>()
        list.addAll(specializationGeneImpact)
        if (parent != null) list.addAll(0, parent.flattenImpacts())
        return list
    }

    fun copy() : HierarchySpecializationImpactInfo{
        return HierarchySpecializationImpactInfo(
                parent?.copy(),
                specializationGeneImpact.map { it.copy() }.toMutableList()
        )
    }
}
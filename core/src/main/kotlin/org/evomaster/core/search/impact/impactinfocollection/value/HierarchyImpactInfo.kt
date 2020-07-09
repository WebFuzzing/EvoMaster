package org.evomaster.core.search.impact.impactinfocollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils

/**
 * created by manzh on 2020-07-08
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
package org.evomaster.core.search.impact.impactinfocollection.value.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.MapGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 *
 * TODO need to further extend for elements
 *
 * MapGeneImpact now is shared by FixedMapGene and FlexibleMapGene
 * now only consider impacts of different size
 */
class MapGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                    val sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size")
) : CollectionImpact, GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun getSizeImpact(): Impact = sizeImpact

    override fun copy(): MapGeneImpact {
        return MapGeneImpact(
                shared.copy(),
                specific.copy(),
                sizeImpact = sizeImpact.copy())
    }

    override fun clone(): MapGeneImpact {
        return MapGeneImpact(
                shared.clone(),
                specific.clone(),
                sizeImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if(gc.current !is MapGene<*, *>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be MapGene")
        if (gc.previous != null && gc.previous !is MapGene<*, *>)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be MapGene")

        if (gc.previous != null && (gc.previous as MapGene<*, *>).getAllElements().size != gc.current.getAllElements().size)
            sizeImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num =  1)

        //TODO for elements
    }

    override fun validate(gene: Gene): Boolean = gene is MapGene<*, *>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${sizeImpact.getId()}" to sizeImpact)
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(sizeImpact)
    }
}
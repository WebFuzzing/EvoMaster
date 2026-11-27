package org.evomaster.core.search.impact.impactinfocollection.value.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.HeterogeneousListGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * Collection impact for [HeterogeneousListGene] (excluding [TupleGene], which uses
 * [org.evomaster.core.search.impact.impactinfocollection.value.TupleGeneImpact]).
 * Tracks size mutations via [sizeImpact] when the list is mutable (`!isFixed`).
 */
class HeterogeneousListGeneImpact(
    sharedImpactInfo: SharedImpactInfo,
    specificImpactInfo: SpecificImpactInfo,
    val sizeImpact: IntegerGeneImpact = IntegerGeneImpact("size")
) : CollectionImpact, GeneImpact(sharedImpactInfo, specificImpactInfo) {

    constructor(id: String) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun getSizeImpact(): Impact = sizeImpact

    override fun copy(): HeterogeneousListGeneImpact {
        return HeterogeneousListGeneImpact(
            shared.copy(),
            specific.copy(),
            sizeImpact = sizeImpact.copy()
        )
    }

    override fun clone(): HeterogeneousListGeneImpact {
        return HeterogeneousListGeneImpact(
            shared.clone(),
            specific.clone(),
            sizeImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(
        gc: MutatedGeneWithContext,
        noImpactTargets: Set<Int>,
        impactTargets: Set<Int>,
        improvedTargets: Set<Int>,
        onlyManipulation: Boolean
    ) {
        countImpactAndPerformance(
            noImpactTargets = noImpactTargets,
            impactTargets = impactTargets,
            improvedTargets = improvedTargets,
            onlyManipulation = onlyManipulation,
            num = gc.numOfMutatedGene
        )

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (!validate(gc.current)) {
            throw IllegalStateException(
                "gc.current (${gc.current::class.java.simpleName}) should be HeterogeneousListGene"
            )
        }
        if (gc.previous != null && !validate(gc.previous)) {
            throw IllegalStateException(
                "gc.previous (${gc.previous::class.java.simpleName}) should be HeterogeneousListGene"
            )
        }

        val currentList = gc.current as HeterogeneousListGene
        val previousList = gc.previous as? HeterogeneousListGene
        if (!currentList.isFixed && previousList != null && previousList.elements.size != currentList.elements.size) {
            sizeImpact.countImpactAndPerformance(
                noImpactTargets = noImpactTargets,
                impactTargets = impactTargets,
                improvedTargets = improvedTargets,
                onlyManipulation = onlyManipulation,
                num = 1
            )
        }
    }

    override fun validate(gene: Gene): Boolean = gene is HeterogeneousListGene && gene !is TupleGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${sizeImpact.getId()}" to sizeImpact)
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(sizeImpact)
    }
}
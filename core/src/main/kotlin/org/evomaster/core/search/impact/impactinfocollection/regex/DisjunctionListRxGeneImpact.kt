package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2020-07-08
 */
class DisjunctionListRxGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val disjunctions: List<DisjunctionRxGeneImpact>
        ) :  RxAtomImpact(sharedImpactInfo, specificImpactInfo){


    constructor(id : String, gene : DisjunctionListRxGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), gene.disjunctions.map { ImpactUtils.createGeneImpact(it, it.name) as DisjunctionRxGeneImpact}.toList())

    override fun copy(): DisjunctionListRxGeneImpact {
        return DisjunctionListRxGeneImpact(
                shared.copy(),
                specific.copy(),
                disjunctions.map { it.copy() }
        )
    }

    override fun clone(): DisjunctionListRxGeneImpact {
        return DisjunctionListRxGeneImpact(
                shared.clone(),
                specific.clone(),
                disjunctions.map { it.clone() }
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DisjunctionListRxGene

    override fun innerImpacts(): List<Impact> {
        return disjunctions
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(
                noImpactTargets = noImpactTargets,
                impactTargets = impactTargets,
                improvedTargets = improvedTargets,
                onlyManipulation = onlyManipulation,
                num = gc.numOfMutatedGene
        )

        check(gc)

        val cActive = (gc.current as DisjunctionListRxGene).activeDisjunction
        if (cActive < 0)
            throw IllegalStateException("none of disjunction is active, i.e., activeDisjunction < 0")

        val cImpact = disjunctions[cActive]

        val pActive = (gc.previous as? DisjunctionListRxGene)?.activeDisjunction?:-1

        val cgc = gc.mainPosition(
                previous = if (pActive < 0 || pActive != cActive )
                    null
                else
                    (gc.previous as DisjunctionListRxGene).disjunctions[pActive],
                current = gc.current.disjunctions[cActive],
                numOfMutatedGene = 1
        )
        cImpact.countImpactWithMutatedGeneWithContext(
                cgc, noImpactTargets, impactTargets, improvedTargets, onlyManipulation
        )

    }
}
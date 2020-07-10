package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2020-07-08
 */
class RegexGeneImpact(
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val listRxGeneImpact: DisjunctionListRxGeneImpact) :  GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String, gene: RegexGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), ImpactUtils.createGeneImpact(gene.disjunctions, gene.disjunctions.name) as DisjunctionListRxGeneImpact)

    override fun copy(): RegexGeneImpact {
        return RegexGeneImpact(
                shared.copy(),
                specific.copy(),
                listRxGeneImpact.copy()
        )
    }

    override fun clone(): RegexGeneImpact {
        return RegexGeneImpact(
                shared.clone(),
                specific.clone(),
                listRxGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is RegexGene

    override fun innerImpacts(): List<Impact> {
        return listOf(listRxGeneImpact)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mapOf("${getId()}-${listRxGeneImpact.getId()}" to listRxGeneImpact).plus(listRxGeneImpact.flatViewInnerImpact().map { "${getId()}-${it.key}" to it.value })
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext,
                                                       noImpactTargets: Set<Int>,
                                                       impactTargets: Set<Int>,
                                                       improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        check(gc)
        listRxGeneImpact.countImpactWithMutatedGeneWithContext(
                gc = gc.mainPosition(
                        current = (gc.current as RegexGene).disjunctions,
                        previous = (gc.previous as? RegexGene)?.disjunctions, numOfMutatedGene = gc.numOfMutatedGene),
                noImpactTargets = noImpactTargets,
                improvedTargets = improvedTargets,
                impactTargets = impactTargets,
                onlyManipulation = onlyManipulation
        )
    }
}
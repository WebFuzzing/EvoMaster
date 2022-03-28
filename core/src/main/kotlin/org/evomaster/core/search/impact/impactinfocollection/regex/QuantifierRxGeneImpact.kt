package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.QuantifierRxGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact

class QuantifierRxGeneImpact(
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val templateImpact : GeneImpact,
        //val atomsImpact: MutableList<GeneImpact>,
        val modifyLengthImpact: BinaryGeneImpact = BinaryGeneImpact("MODIFY_LENGTH")) :  RxAtomImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            gene: QuantifierRxGene
    ) : this(SharedImpactInfo(id), SpecificImpactInfo(), ImpactUtils.createGeneImpact(gene.template, gene.name))

    override fun copy(): QuantifierRxGeneImpact {
        return QuantifierRxGeneImpact(
                shared.copy(),
                specific.copy(),
                templateImpact.copy(),
                modifyLengthImpact.copy()
        )
    }

    override fun clone(): QuantifierRxGeneImpact {
        return QuantifierRxGeneImpact(
                shared.clone(),
                specific.clone(),
                templateImpact.clone(),
                modifyLengthImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is QuantifierRxGene

    override fun innerImpacts(): List<Impact> {
        return listOf(templateImpact).plus(modifyLengthImpact)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets, impactTargets, improvedTargets, onlyManipulation, num = gc.numOfMutatedGene)
        check(gc)

        val modifylength = (gc.current as QuantifierRxGene).atoms.size != (gc.previous as? QuantifierRxGene)?.atoms?.size
        if (modifylength){
            modifyLengthImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
            return
        }

    }
}
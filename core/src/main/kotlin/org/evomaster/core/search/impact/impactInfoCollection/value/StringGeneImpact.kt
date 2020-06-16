package org.evomaster.core.search.impact.impactInfoCollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.impactInfoCollection.*

/**
 * created by manzh on 2019-09-09
 */
class StringGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                        /**
                         * impacts on its specific type
                         * it might lead to an issue when the type of gene is dynamic, thus the type of the current might differ from the type of the previous
                         */
                        var specializationGeneImpact : List<Impact>
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            specializationGeneImpact : List<Impact> = mutableListOf()
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            specializationGeneImpact
    )

    constructor(id: String, gene : StringGene)
            : this(
            id,
            specializationGeneImpact = gene.specializationGenes.map { ImpactUtils.createGeneImpact(it, it.name) })

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(
                shared.copy(),
                specific.copy(),
                specializationGeneImpact = specializationGeneImpact.map { it.copy()})
    }

    override fun clone(): StringGeneImpact {
        return StringGeneImpact(
                shared.clone(),
                specific.clone(),
                specializationGeneImpact = specializationGeneImpact.map { it.clone()})
    }

    override fun validate(gene: Gene): Boolean = gene is StringGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        //FIXME regarding specializationGeneImpact
    }
}
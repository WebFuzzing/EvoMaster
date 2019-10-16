package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext

/**
 * created by manzh on 2019-09-09
 */
class StringGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        /**
         * impacts on its specific type
         * it might lead to an issue when the type of gene is dynamic, thus the type of the current might differ from the type of the previous
         */
        var specializationGeneImpact : List<Impact> = mutableListOf()
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, gene : StringGene)
            : this(
            id,
            specializationGeneImpact = gene.specializationGenes.map { ImpactUtils.createGeneImpact(it, it.name) })

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                specializationGeneImpact = specializationGeneImpact.map { it.copy()})
    }

    override fun validate(gene: Gene): Boolean = gene is StringGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        //FIXME regarding specializationGeneImpact
    }
}
package org.evomaster.core.search.impact.value

import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class StringGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        /**
         * impacts on its specific type
         * it might lead to an issue when the type of gene is dynamic, thus the type of the current might differ from the type of the previous
         */
        var specializationGeneImpact : List<Impact> = mutableListOf()
): GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    constructor(id: String, gene : StringGene)
            : this(
            id,
            specializationGeneImpact = gene.specializationGenes.map { ImpactUtils.createGeneImpact(it, it.name) })

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, specializationGeneImpact.map { it.copy()})
    }

    override fun validate(gene: Gene): Boolean = gene is StringGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)
        //FIXME regarding specializationGeneImpact
    }
}
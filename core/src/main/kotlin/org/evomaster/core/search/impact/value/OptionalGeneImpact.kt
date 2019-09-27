package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class OptionalGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val activeImpact : BinaryGeneImpact = BinaryGeneImpact("isActive"),
        val geneImpact: GeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    constructor(id : String, optionalGene: OptionalGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(optionalGene.gene, id))

    override fun copy(): OptionalGeneImpact {
        return OptionalGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, activeImpact.copy(), geneImpact.copy() as GeneImpact)
    }

    fun countActiveImpact(isActive : Boolean, hasImpact: Boolean, isWorse: Boolean){
        if (isActive)
            activeImpact._true.countImpactAndPerformance(hasImpact, isWorse)
        else
            activeImpact._false.countImpactAndPerformance(hasImpact, isWorse)
    }

    override fun validate(gene: Gene): Boolean = gene is OptionalGene
}
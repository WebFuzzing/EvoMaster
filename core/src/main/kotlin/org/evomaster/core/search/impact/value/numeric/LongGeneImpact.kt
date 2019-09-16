package org.evomaster.core.search.impact.value.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.LongGene

/**
 * created by manzh on 2019-09-09
 */
class LongGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        positionSensitive: Boolean = false
): NumericGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    override fun copy(): LongGeneImpact {
        return LongGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive)
    }

    override fun validate(gene: Gene): Boolean = gene is LongGene
}
package org.evomaster.core.search.impact.value.numeric

import org.evomaster.core.search.gene.DoubleGene
import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-09
 */
class DoubleGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false
): NumericGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive) {

    override fun copy(): DoubleGeneImpact {
        return DoubleGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive)
    }

    override fun validate(gene: Gene): Boolean = gene is DoubleGene
}
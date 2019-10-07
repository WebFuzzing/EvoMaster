package org.evomaster.core.search.impact.value.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene

/**
 * created by manzh on 2019-09-09
 */
class IntegerGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false
): NumericGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,positionSensitive) {

    override fun copy(): IntegerGeneImpact {
        return IntegerGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive)
    }

    override fun validate(gene: Gene): Boolean = gene is IntegerGene
}
package org.evomaster.core.search.impact.value

import org.evomaster.core.search.impact.GeneImpact

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
        positionSensitive: Boolean = false
): GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive)
    }

}
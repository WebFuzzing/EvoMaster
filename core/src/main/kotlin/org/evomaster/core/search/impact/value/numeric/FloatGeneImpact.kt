package org.evomaster.core.search.impact.value.numeric

/**
 * created by manzh on 2019-09-09
 */
class FloatGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        positionSensitive: Boolean = false
): NumericGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    override fun copy(): FloatGeneImpact {
        return FloatGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive)
    }

}
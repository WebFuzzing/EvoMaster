package org.evomaster.core.search.impact.value

import org.evomaster.core.search.impact.Impact

/**
 * created by manzh on 2019-09-09
 */
class GeneralImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0
) : Impact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    override fun copy(): GeneralImpact {
        return GeneralImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    }
}
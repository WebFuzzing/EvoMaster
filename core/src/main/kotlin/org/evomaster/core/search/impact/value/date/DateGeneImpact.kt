package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.impact.GeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DateGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    override fun copy(): DateGeneImpact {
        return DateGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    }
}
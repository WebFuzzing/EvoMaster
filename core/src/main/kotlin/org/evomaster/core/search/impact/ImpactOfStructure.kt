package org.evomaster.core.search.impact

/**
 * created by manzh on 2019-09-03
 */
class ImpactOfStructure (
        id: String,
        degree: Double,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0 ,
        timesOfNoImpacts: Int =0,
        counter: Int = 0
) :Impact (id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
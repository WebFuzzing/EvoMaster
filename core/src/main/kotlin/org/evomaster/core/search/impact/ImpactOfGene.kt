package org.evomaster.core.search.impact

/**
 * created by manzh on 2019-09-03
 */

class ImpactOfGene (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        positionSensitive: Boolean = false

):Impact( id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter){

    var positionSensitive: Boolean = positionSensitive
        private set

    override fun copy(): ImpactOfGene {
        return ImpactOfGene(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive)
    }

    fun confirmPositionSensitive(){
        positionSensitive = true
    }
}
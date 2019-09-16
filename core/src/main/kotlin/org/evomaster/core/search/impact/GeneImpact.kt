package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene

/**
 * created by manzh on 2019-09-03
 */

abstract class GeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false

):Impact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter){

    /**
     * whether the impact of gene varies with respect to different position
     */
    var positionSensitive: Boolean = positionSensitive
        private set


    fun confirmPositionSensitive(){
        positionSensitive = true
    }

    abstract fun validate(gene : Gene) : Boolean
}
package org.evomaster.core.search.impact

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times the element is manipulated
 * @property timesOfImpact presents how many times the change of the element (i.e., Gene, structure of individual) impacts the [Archive]
 * @property timesOfNoImpacts presents how many times the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 */
open class Impact (
        val id : String,
        var degree: Double,
        var timesToManipulate : Int = 0,
        var timesOfImpact : Int = 0,
        var timesOfNoImpacts : Int = 0,
        var counter : Int = 0
){
    open fun copy() : Impact{
        return Impact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    }

    fun countImpact(hasImpact:Boolean){
        timesToManipulate++
        if (hasImpact) {
            timesOfImpact++
            resetCounter()
        } else {
            counter++
            timesOfNoImpacts
        }
    }
    fun increaseDegree(delta : Double){
        degree += delta
    }

    private fun resetCounter(){
        counter = 0
    }
}

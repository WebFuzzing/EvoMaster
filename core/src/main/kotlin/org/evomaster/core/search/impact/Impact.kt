package org.evomaster.core.search.impact

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times the element is manipulated
 * @property timesOfImpact presents how many times the change of the element (i.e., Gene, structure of individual) impacts the [Archive]
 * @property timesOfNoImpacts presents how many times the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 * @property counter continuous times of no impact
 * @property wCounter continuous times of worse results
 */
abstract class Impact (
        val id : String,
        var degree: Double,
        var timesToManipulate : Int = 0,
        var timesOfImpact : Int = 0,
        var timesOfNoImpacts : Int = 0,
        var counter : Int = 0,
        var wCounter : Int = 0
){
    abstract fun copy() : Impact

    fun countImpactAndPerformance(hasImpact:Boolean, isWorse : Boolean){
        if (isWorse && !hasImpact)
            throw IllegalArgumentException("if isWorse is $isWorse, hasImpact must be true, but $hasImpact")

        timesToManipulate +=1
        if (hasImpact) {
            timesOfImpact +=1
            resetCounter()
            if (isWorse)
                wCounter +=1
            else
                wCounter = 0
        } else {
            counter+=1
            timesOfNoImpacts+=1
        }
    }
    fun increaseDegree(delta : Double){
        degree += delta
    }

    private fun resetCounter(){
        counter = 0
    }
}

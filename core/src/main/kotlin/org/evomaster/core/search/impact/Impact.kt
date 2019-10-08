package org.evomaster.core.search.impact

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times the element is manipulated
 * @property timesOfImpact presents how many times the change of the element (i.e., Gene, structure of individual) impacts the [Archive]
 * @property timesOfNoImpacts presents how many times the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 * @property counter continuous times of no impact
 * @property niCounter continuous times of results which does not contribute to an improvement
 */
abstract class Impact (
        val id : String,
        var degree: Double,
        var timesToManipulate : Int = 0,
        var timesOfImpact : Int = 0,
        var timesOfNoImpacts : Int = 0,
        var counter : Int = 0,
        var niCounter : Int = 0
){
    abstract fun copy() : Impact

    fun countImpactAndPerformance(hasImpact:Boolean, noImprovement : Boolean){

        timesToManipulate +=1
        if (hasImpact) {
            timesOfImpact +=1
            resetCounter()
            if (noImprovement)
                niCounter +=1
            else
                niCounter = 0
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

    open fun maxTimesOfNoImpact() : Int = 10


    companion object{
        fun toCSVHeader() : List<String> = listOf("id", "degree", "timesToManipulate", "timesOfImpact","timesOfNoImpacts","counter","niCounter")
    }
    fun toCSVCell() : List<String> = listOf(id, degree.toString(), timesToManipulate.toString(), timesOfImpact.toString(), timesOfNoImpacts.toString(),counter.toString(), niCounter.toString())
}

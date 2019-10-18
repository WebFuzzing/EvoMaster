package org.evomaster.core.search.impact

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times [value] the element is manipulated
 * @property timesOfImpact presents how many times [value] the change of the element (i.e., Gene, structure of individual) impacts the [Archive] with regards to target id [key]
 * @property timesOfNoImpacts presents how many times [value] the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 * @property conTimesOfNoImpacts continuous times [value] of no impact
 * @property noImpactFromImpact continuous times [value] of no impact but it had impact with regards to target id [key]
 * @property noImprovement continuous times [value] of results which does not contribute to an improvement with regards to target id [key]
 */
abstract class Impact(
        val id: String,
        var degree: Double = 0.0,
        var timesToManipulate: Int = 0,
        var timesOfNoImpacts: Int = 0,
        val timesOfImpact: MutableMap<Int, Int> = mutableMapOf(),
        val noImpactFromImpact: MutableMap<Int, Int> = mutableMapOf(),
        val noImprovement: MutableMap<Int, Int> = mutableMapOf()
){
    abstract fun copy() : Impact

    fun countImpactAndPerformance(impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){

        timesToManipulate += 1
        val hasImpact = impactTargets.isNotEmpty()
        if (hasImpact) {
            impactTargets.forEach { target ->
                if (onlyManipulation){
                    initMap(target, timesOfImpact)
                }else{
                    plusMap(target, timesOfImpact)
                    assignMap(target, noImpactFromImpact, 0)
                    if (improvedTargets.contains(target))
                        assignMap(target,noImprovement, 0)
                    else
                        plusMap(target, noImprovement)
                }
            }
            if (!onlyManipulation){
                noImpactFromImpact.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, noImpactFromImpact)
                }
                noImprovement.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, noImprovement)
                }
            }
        } else {
            noImpactFromImpact.keys.forEach { target->
                plusMap(target, noImpactFromImpact)
            }
            noImprovement.keys.forEach { target->
                plusMap(target, noImprovement)
            }
            timesOfNoImpacts +=1
        }
    }

    private fun plusMap(key : Int, map: MutableMap<Int, Int>){
        map.getOrPut(key){0}
        map.replace(key, map[key]!! + 1)
    }

    private fun assignMap(key : Int, map: MutableMap<Int, Int>, value : Int){
        map.getOrPut(key){0}
        map.replace(key, value)
    }

    private fun initMap(key : Int, map: MutableMap<Int, Int>){
        map.getOrPut(key){0}
    }

    fun increaseDegree(delta : Double){
        degree += delta
    }

    open fun maxTimesOfNoImpact() : Int = 10


    companion object{
        fun toCSVHeader() : List<String> = listOf("id", "degree", "timesToManipulate", "timesOfNoImpacts","timesOfImpact","noImpactFromImpact","noImprovement")
    }
    fun toCSVCell() : List<String> = listOf(
            id,
            degree.toString(),
            timesToManipulate.toString(),
            timesOfNoImpacts.toString(),
            timesOfImpact.map { "${it.key}->${it.value}" }.joinToString(";"),
            noImpactFromImpact.map { "${it.key}->${it.value}" }.joinToString(";"),
            noImprovement.map { "${it.key}->${it.value}" }.joinToString(";")
    )

    fun getMaxImpact() : Int = timesOfImpact.values.max()?:0

    fun getValueByImpactProperty(property: ImpactProperty, target : Int) : Int{
        return when(property){
            ImpactProperty.TIMES_NO_IMPACT -> timesOfNoImpacts
            ImpactProperty.TIMES_IMPACT -> timesOfImpact[target]?:-1
            ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT -> noImpactFromImpact[target]?:-1
            ImpactProperty.TIMES_CONS_NO_IMPROVEMENT -> noImpactFromImpact[target]?:-1
        }
    }
}

enum class ImpactProperty{
    TIMES_NO_IMPACT,
    TIMES_IMPACT,
    TIMES_CONS_NO_IMPACT_FROM_IMPACT,
    TIMES_CONS_NO_IMPROVEMENT
}
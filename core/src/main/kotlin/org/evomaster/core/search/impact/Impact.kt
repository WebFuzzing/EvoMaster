package org.evomaster.core.search.impact

open class Impact(
        val shared : SharedImpactInfo,
        val specific : SpecificImpactInfo
){
    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf()
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement))

    fun getId() = shared.id
    fun getTimesOfNoImpact() = shared.timesOfNoImpacts
    fun getTimesOfNoImpactWithTargets() = shared.timesOfNoImpactWithTargets
    fun getTimesToManipulate() = shared.timesToManipulate
    fun getDegree() = shared.degree

    fun getDegree(property: ImpactProperty, target: Int) = if (getTimesToManipulate() == 0) -1.0 else getValueByImpactProperty(property, target)/getTimesToManipulate().toDouble()
    fun getCounter(property: ImpactProperty, target: Int) = getValueByImpactProperty(property, target)

    fun getDegree(property: ImpactProperty, targets: Set<Int>, by: By) : Double{
        return targets.map { getDegree(property, it) }.filter { it != -1.0 }.run {
            if (isEmpty()) -1.0
            else{
                when(by){
                    By.MIN -> this.min()!!
                    By.MAX -> this.max()!!
                    By.AVG -> this.average()!!
                }
            }
        }
    }
    fun getCounter(property: ImpactProperty, targets: Set<Int>, by: By) : Int{
        val list = targets.map { getCounter(property, it) }.filter { it != -1 }
        if (list.isEmpty()) return -1
        return when(by){
            By.MIN -> list.min()?: throw IllegalArgumentException("min is null")
            By.MAX -> list.max()?: throw IllegalArgumentException("max is null")
            By.AVG -> list.average().toInt()
        }
    }

    fun getTimesOfImpacts() = shared.timesOfImpact

    fun getNoImpactsFromImpactCounter() = specific.noImpactFromImpact
    fun getNoImprovementCounter() = specific.noImprovement


    open fun copy(): Impact {
        return Impact(
                shared.copy(), specific.copy()
        )
    }

    open fun clone() : Impact{
        return Impact(
                shared, specific.copy()
        )
    }

    fun countImpactAndPerformance(noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){
        shared.timesToManipulate += 1
        val hasImpact = impactTargets.isNotEmpty()

        if (hasImpact) {
            impactTargets.forEach { target ->
                if (onlyManipulation){
                    initMap(target, shared.timesOfImpact)
                }else{
                    plusMap(target, shared.timesOfImpact)
                    assignMap(target, specific.noImpactFromImpact, 0)
                    if (improvedTargets.contains(target))
                        assignMap(target,specific.noImprovement, 0)
                    else
                        plusMap(target, specific.noImprovement)
                }
            }
            if (!onlyManipulation){
                specific.noImpactFromImpact.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, specific.noImpactFromImpact)
                }
                specific.noImprovement.keys.filter { !impactTargets.contains(it) }.forEach { k->
                    plusMap(k, specific.noImprovement)
                }
            }
        } else {
            specific.noImpactFromImpact.keys.forEach { target->
                plusMap(target, specific.noImpactFromImpact)
            }
            specific.noImprovement.keys.forEach { target->
                plusMap(target, specific.noImprovement)
            }
            shared.timesOfNoImpacts +=1
            noImpactTargets.forEach {
                plusMap(it, shared.timesOfNoImpactWithTargets)
            }
        }
    }

    private fun plusMap(key : Int, map: MutableMap<Int, Int>){
//        map.getOrPut(key){0}
//        map.replace(key, map[key]!! + 1)
        map.merge(key, 1){old, delta -> (old + delta)}
    }

    private fun assignMap(key : Int, map: MutableMap<Int, Int>, value : Int){
        map.getOrPut(key){0}
        map.replace(key, value)
    }

    private fun initMap(key : Int, map: MutableMap<Int, Int>){
        map.getOrPut(key){0}
    }

    fun increaseDegree(delta : Double){
        shared.degree += delta
    }

    open fun maxTimesOfNoImpact() : Int = 10


    companion object{
        fun toCSVHeader() : List<String> = listOf("id", "degree", "timesToManipulate", "timesOfNoImpacts","timesOfImpact","noImpactFromImpact","noImprovement")
    }
    fun toCSVCell() : List<String> = listOf(
            getId(),
            getDegree().toString(),
            getTimesToManipulate().toString(),
            getTimesOfNoImpact().toString(),
            getTimesOfImpacts().map { "${it.key}->${it.value}" }.joinToString(";"),
            getNoImpactsFromImpactCounter().map { "${it.key}->${it.value}" }.joinToString(";"),
            getNoImprovementCounter().map { "${it.key}->${it.value}" }.joinToString(";")
    )

    fun getMaxImpact() : Int = shared.timesOfImpact.values.max()?:0

    fun getValueByImpactProperty(property: ImpactProperty, target : Int) : Int{
        return when(property){
            ImpactProperty.TIMES_NO_IMPACT -> shared.timesOfNoImpacts
            ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET -> shared.timesOfNoImpactWithTargets[target]?:-1
            ImpactProperty.TIMES_IMPACT -> shared.timesOfImpact[target]?:-1
            ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT -> specific.noImpactFromImpact[target]?:-1
            ImpactProperty.TIMES_CONS_NO_IMPROVEMENT -> specific.noImpactFromImpact[target]?:-1
        }
    }
}

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times [value] the element is manipulated
 * @property timesOfImpact presents how many times [value] the change of the element (i.e., Gene, structure of individual) impacts the [Archive] with regards to target id [key]
 * @property timesOfNoImpacts presents how many times [value] the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 */
class SharedImpactInfo(
        val id: String,
        var degree: Double,
        var timesToManipulate: Int,
        var timesOfNoImpacts: Int,
        val timesOfNoImpactWithTargets : MutableMap<Int, Int> ,
        val timesOfImpact: MutableMap<Int, Int>){

    fun copy() : SharedImpactInfo{
        return SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact.toMutableMap())
    }

    fun clone() = this
}

/**
 * @property noImpactFromImpact continuous times [value] of no impact but it had impact with regards to target id [key]
 * @property noImprovement continuous times [value] of results which does not contribute to an improvement with regards to target id [key]
 */
class SpecificImpactInfo(
        val noImpactFromImpact: MutableMap<Int, Int> = mutableMapOf(),
        val noImprovement: MutableMap<Int, Int> = mutableMapOf()
){
    fun copy() : SpecificImpactInfo{
        return SpecificImpactInfo(noImpactFromImpact.toMutableMap(), noImprovement.toMutableMap())
    }

    fun clone() : SpecificImpactInfo = copy()
}

enum class ImpactProperty{
    TIMES_NO_IMPACT,
    TIMES_NO_IMPACT_WITH_TARGET,
    TIMES_IMPACT,
    TIMES_CONS_NO_IMPACT_FROM_IMPACT,
    TIMES_CONS_NO_IMPROVEMENT
}

enum class By{
    MIN,
    MAX,
    AVG
}
package org.evomaster.core.search.impact.impactinfocollection


/**
 * @property shared shared impacts. over a course of mutation, some gene impact info (e.g., times of manipulating the gene) should be shared that are collected by [shared].
 * @property specific there exist some impact info specific to 'current' gene, e.g., times of no impact from impact
 *
 * for instance, an evolution of a gene is 0:A-1:B-2:(C)-3:D-4:(E)-5:F-6:G-7:(H)-8:(I)-9:($J$), where
 *      (?) represents a gene is mutated, but no improvement
 *      $?$ represents a gene is mutated, but no impact
 *      number represents the order to mutate during search, and a gene is originated from sampling, represented by 0.
 *
 * regarding genes A, B, D, F, G, timesToManipulate (i.e.,9), timesOfNoImpact (i.e.,1), timesOfImpact(i.e.,8) are shared,
 * regarding gene G, noImpactFromImpact(i.e., 1) and noImprovement(i.e.,3) are specific
 */
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

    fun recentImprovement() = getNoImprovementCounter().any { it.value < 2 }

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
            "CM:${getTimesToManipulate()}",
            "CN:${getTimesOfNoImpact()}",
            "I:${getTimesOfImpacts().map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NI:${getTimesOfNoImpactWithTargets().map { "${it.key}->${it.value}" }.joinToString(";")}",
            "I->NI:${getNoImpactsFromImpactCounter().map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NV:${getNoImprovementCounter().map { "${it.key}->${it.value}" }.joinToString(";")}"
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
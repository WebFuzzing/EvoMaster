package org.evomaster.core.search.impact.impactinfocollection

import kotlin.math.pow


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
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf()
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement))

    fun getId() = shared.id
    fun getTimesOfNoImpact() = shared.timesOfNoImpacts
    fun getTimesOfNoImpactWithTargets() = shared.timesOfNoImpactWithTargets
    fun getTimesToManipulate() = shared.timesToManipulate
    fun getDegree() = shared.degree

    /**
     * @return whether there exist any recent improvement
     */
    fun recentImprovement() = getNoImprovementCounter().any { it.value < 2 }

    private fun getDegree(property: ImpactProperty, target: Int, singleImpactReward : Boolean) : Double?{
        return   if (property == ImpactProperty.E_IMPACT_DIVIDE_NO_IMPACT) getValueByImpactProperty(property, target, singleImpactReward)
            else if (manipulateTimesForTargets(target, singleImpactReward) == 0.0) 1.0
            else getValueByImpactProperty(property, target, singleImpactReward)?.div(manipulateTimesForTargets(target, singleImpactReward))
    }

    private fun getCounter(property: ImpactProperty, target: Int, singleImpactReward : Boolean) : Double?{
        return if (getTimesToManipulate() == 0) 1.0 else getValueByImpactProperty(property, target, singleImpactReward)
    }


    fun getDegree(property: ImpactProperty, targets: Set<Int>, by: By, singleImpactReward: Boolean) : Double?{
        return targets.mapNotNull { getDegree(property, it, singleImpactReward = singleImpactReward) }.run {
            when(by){
                By.MIN -> this.minOrNull()
                By.MAX -> this.maxOrNull()
                By.AVG -> this.average()
                By.SUM -> this.filter { it > 0.0 }.sum()
            }
        }
    }

    fun getCounter(property: ImpactProperty, targets: Set<Int>, by: By, singleImpactReward: Boolean) : Double?{
        val list = targets.mapNotNull { getCounter(property, it, singleImpactReward) }
        return when(by){
            By.MIN -> list.minOrNull()
            By.MAX -> list.maxOrNull()
            By.AVG -> list.average()
            By.SUM -> list.filter { it > 0.0}.sum()
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

    /**
     * @param noImpactTargets is a set of targets which has no changes with this mutation
     * @param impactTargets is a set of targets which have any impact (either better or worse) with this mutation
     * @param improvedTargets is a set of targets which have been improved with this mutation
     * @param onlyManipulation specifies if only collect time of mutations
     * @param num is a number of modifications with this mutation, e.g., hypermutation on genes in an individual
     */
    fun countImpactAndPerformance(noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean, num: Int){
        shared.timesToManipulate += 1
        val hasImpact = impactTargets.isNotEmpty()

        if (hasImpact) {
            impactTargets.forEach { target ->
                if (onlyManipulation){
                    initMap(target, shared.timesOfImpact)
                }else{
                    shared.singleImpact.merge(target, num == 1){old, delta -> (old || delta)}
                    plusMap(target, shared.timesOfImpact, num)
                    assignMap(target, specific.noImpactFromImpact, 0.0)
                    if (improvedTargets.contains(target))
                        assignMap(target,specific.noImprovement, 0.0)
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

    private fun plusMap(key : Int, map: MutableMap<Int, Double>, num: Int = 1){
        map.merge(key, 1.0/num){old, delta -> (old + delta)}
    }

    private fun assignMap(key : Int, map: MutableMap<Int, Double>, value : Double){
        map.getOrPut(key){0.0}
        map.replace(key, value)
    }

    private fun initMap(key : Int, map: MutableMap<Int, Double>){
        map.getOrPut(key){0.0}
    }

    fun increaseDegree(delta : Double){
        shared.degree += delta
    }

    open fun maxTimesOfNoImpact() : Int = 10


    companion object{
        fun toCSVHeader() : List<String> = listOf("id", "degree", "timesToManipulate", "timesOfNoImpacts","timesOfImpact","noImpactFromImpact","noImprovement")
    }

    /**
     * @return a list of string which reflects impact info with the header i.e., [toCSVHeader]
     * this is only used for debugging
     */
    fun toCSVCell(targets : Set<Int>? = null) : List<String> = listOf(
            getId(),
            getDegree().toString(),
            "CM:${getTimesToManipulate()}",
            "CNI:${getTimesOfNoImpact()}",
            "I:${getTimesOfImpacts().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NI:${getTimesOfNoImpactWithTargets().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "I->NI:${getNoImpactsFromImpactCounter().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}",
            "NV:${getNoImprovementCounter().filter { targets?.contains(it.key)?:true }.map { "${it.key}->${it.value}" }.joinToString(";")}"
    )

    /**
     * @return max times of impacts across various targets
     */
    fun getMaxImpact() : Double = shared.timesOfImpact.values.maxOrNull()?:0.0

    private fun getValueByImpactProperty(property: ImpactProperty, target : Int, singleImpactReward: Boolean) : Double?{
        return when(property){
            ImpactProperty.TIMES_NO_IMPACT -> shared.timesOfNoImpacts.toDouble()
            ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET -> shared.timesOfNoImpactWithTargets[target]
            ImpactProperty.TIMES_IMPACT -> shared.timesOfImpact[target]?.times(singleReward(singleImpactReward))
            ImpactProperty.TIMES_CONS_NO_IMPACT_FROM_IMPACT -> specific.noImpactFromImpact[target]
            ImpactProperty.TIMES_CONS_NO_IMPROVEMENT -> specific.noImpactFromImpact[target]
            ImpactProperty.E_IMPACT_DIVIDE_NO_IMPACT -> nl(target, divide = true)
            ImpactProperty.E_IMPACT_MINUS_NO_IMPACT -> nl(target, divide = false)
        }
    }

    /**
     * calculate impact verse noimpact with natural logarithms
     * @param target regarding a specific target
     * @param divide represent two different methods.
     *          when it is true, e^(impact/all)/e^(noimpact/all)
     *          when it is false, e^((impact-noimpact)/all)
     */
    private fun nl(target: Int, divide : Boolean) : Double{
        val impact = getValueByImpactProperty(ImpactProperty.TIMES_IMPACT, target, true)?:0.0
        val noImpact = getValueByImpactProperty(ImpactProperty.TIMES_NO_IMPACT_WITH_TARGET, target, true)?:0.0
        val sum = impact + noImpact
        if (sum == 0.0) return  Math.E.pow(sum)
        if (divide){
            val ei = Math.E.pow( (impact)/sum )
            val en = Math.E.pow( (noImpact)/sum )
            return ei/en
        }else{
            return Math.E.pow( (impact - noImpact)/sum)
        }
    }

    private fun manipulateTimesForTargets(target: Int, singleImpactReward: Boolean) : Double = (shared.timesOfNoImpactWithTargets[target]?:0.0) + (shared.timesOfImpact[target]?:0.0) * singleReward(singleImpactReward)

    private fun singleReward(reward : Boolean) = if (reward) 1.5 else 1.0
}

/**
 * @property id of impact, always refer to Gene of Action or structure of individual
 * @property degree of the impact
 * @property timesToManipulate presents how many times [value] the element is manipulated
 * @property timesOfImpact presents how many times [value] the change of the element (i.e., Gene, structure of individual) impacts the [Archive] with regards to target id [key]
 * @property timesOfNoImpacts presents how many times [value] the change of the element (i.e., Gene, structure of individual) did not impact the [Archive]
 * @property singleImpact presents whether ([value]) the impact on the target ([key]) is attributed by single modification.
 */
class SharedImpactInfo(
        val id: String,
        var degree: Double = 0.0,
        var timesToManipulate: Int = 0,
        var timesOfNoImpacts: Int = 0,
        val timesOfNoImpactWithTargets: MutableMap<Int, Double> = mutableMapOf(),
        val timesOfImpact: MutableMap<Int, Double> = mutableMapOf(),
        val singleImpact : MutableMap<Int, Boolean> = mutableMapOf()){

    fun copy() : SharedImpactInfo{
        return SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact.toMutableMap(), singleImpact.toMutableMap())
    }

    fun clone() = this
}

/**
 * @property noImpactFromImpact continuous times [value] of no impact but it had impact with regards to target id [key]
 * @property noImprovement continuous times [value] of results which does not contribute to an improvement with regards to target id [key]
 */
class SpecificImpactInfo(

        val noImpactFromImpact: MutableMap<Int, Double> = mutableMapOf(),
        val noImprovement: MutableMap<Int, Double> = mutableMapOf()
){
    fun copy() : SpecificImpactInfo{
        return SpecificImpactInfo(noImpactFromImpact.toMutableMap(), noImprovement.toMutableMap())
    }

    fun clone() : SpecificImpactInfo = copy()
}

enum class ImpactProperty{
    /**
     * get times of no impacts
     */
    TIMES_NO_IMPACT,

    /**
     * get times of no impact with respect to targets
     */
    TIMES_NO_IMPACT_WITH_TARGET,

    /**
     * get times of impact
     */
    TIMES_IMPACT,

    /**
     * get times of continuous no impact
     */
    TIMES_CONS_NO_IMPACT_FROM_IMPACT,

    /**
     * get times of continuous no improvement
     */
    TIMES_CONS_NO_IMPROVEMENT,

    /**
     * get a ratio of (times of impact)/(times of no impacts)
     */
    E_IMPACT_DIVIDE_NO_IMPACT,

    /**
     * get difference between (times of impact) and (times of no impact)
     */
    E_IMPACT_MINUS_NO_IMPACT
}

enum class By{
    MIN,
    MAX,
    AVG,
    SUM
}
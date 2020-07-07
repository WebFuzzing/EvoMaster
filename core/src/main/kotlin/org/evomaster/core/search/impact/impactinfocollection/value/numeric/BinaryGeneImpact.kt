package org.evomaster.core.search.impact.impactinfocollection.value.numeric

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneSelector

/**
 * created by manzh on 2019-09-09
 */
class BinaryGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val falseValue : Impact ,
        val trueValue : Impact
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            falseValue : Impact = Impact("false"),
            trueValue : Impact = Impact("true")
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            falseValue,
            trueValue)

    override fun copy(): BinaryGeneImpact {
        return BinaryGeneImpact(
                shared.copy(),
                specific.copy(),
                falseValue = falseValue.copy(),
                trueValue = trueValue.copy())
    }

    override fun clone(): BinaryGeneImpact {
        return BinaryGeneImpact(
                shared.clone(),
                specific.clone(),
                falseValue = falseValue.clone(),
                trueValue = trueValue.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is BooleanGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.current !is BooleanGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be BooleanGene")

        if (gc.current.value){
            trueValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }else
            falseValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-false" to falseValue, "${getId()}-true" to trueValue)
    }

    override fun maxTimesOfNoImpact(): Int = 3

    fun select(minManipulatedTimes : Int, preferTrue : Boolean, times : Int, targets: Set<Int>, selector: ArchiveGeneSelector) : Boolean{
        if (shared.timesToManipulate < minManipulatedTimes) return preferTrue
        val list = if (preferTrue) listOf(trueValue, falseValue) else listOf(falseValue, trueValue)
        val weights = selector.impactBasedOnWeights(list, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT), usingCounter = true)

        return if (weights[1]/weights[0] > times) !preferTrue else preferTrue
    }
}
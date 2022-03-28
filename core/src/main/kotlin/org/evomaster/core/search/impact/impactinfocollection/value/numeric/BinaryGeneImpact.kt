package org.evomaster.core.search.impact.impactinfocollection.value.numeric

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector

/**
 * created by manzh on 2019-09-09
 */
class BinaryGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val falseValue : Impact = Impact("false"),
        val trueValue : Impact = Impact("true")
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String) : this(SharedImpactInfo(id), SpecificImpactInfo())

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
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        if (gc.current !is BooleanGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be BooleanGene")

        if (gc.current.value){
            trueValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num =1)
        }else
            falseValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${falseValue.getId()}" to falseValue, "${getId()}-${trueValue.getId()}" to trueValue)
    }

    override fun maxTimesOfNoImpact(): Int = 3

    fun determinateSelect(minManipulatedTimes : Int, preferTrue : Boolean, times : Double, targets: Set<Int>, selector: ArchiveImpactSelector) : Boolean{
        if (shared.timesToManipulate < minManipulatedTimes) return preferTrue
        if (trueValue.getTimesToManipulate() == 0) return true
        if (falseValue.getTimesToManipulate() == 0) return false

        val list = if (preferTrue) listOf(trueValue, falseValue) else listOf(falseValue, trueValue)
        val weights = selector.impactBasedOnWeights(list, targets = targets, properties = arrayOf(ImpactProperty.TIMES_IMPACT), usingCounter = null)
        return if (weights[1] > times * weights[0]) !preferTrue else preferTrue
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(trueValue, falseValue)
    }
}
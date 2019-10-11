package org.evomaster.core.search.impact.value.numeric

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.GeneralImpact

/**
 * created by manzh on 2019-09-09
 */
class BinaryGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        conTimesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val _false : GeneralImpact = GeneralImpact("false"),
        val _true : GeneralImpact = GeneralImpact("true")
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        conTimesOfNoImpacts = conTimesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    override fun copy(): BinaryGeneImpact {
        return BinaryGeneImpact(id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                conTimesOfNoImpacts = conTimesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                _false = _false.copy(),
                _true = _true.copy())
    }

    override fun validate(gene: Gene): Boolean = gene is BooleanGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.current !is BooleanGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be BooleanGene")

        if (gc.current.value){
            _true.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }else
            _false.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("$id-false" to _false, "$id-true" to _true)
    }

    override fun maxTimesOfNoImpact(): Int = 3
}
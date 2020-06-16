package org.evomaster.core.search.impact.impactInfoCollection.value.date

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.TimeGene
import org.evomaster.core.search.impact.impactInfoCollection.*
import org.evomaster.core.search.impact.impactInfoCollection.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-16
 */
class TimeGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                     val hourGeneImpact: IntegerGeneImpact,
                     val minuteGeneImpact: IntegerGeneImpact,
                     val secondGeneImpact : IntegerGeneImpact
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
            hourGeneImpact: IntegerGeneImpact,
            minuteGeneImpact: IntegerGeneImpact,
            secondGeneImpact : IntegerGeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            hourGeneImpact, minuteGeneImpact, secondGeneImpact)

    constructor(id: String, gene : TimeGene)
            : this(id,
            hourGeneImpact = ImpactUtils.createGeneImpact(gene.hour, gene.hour.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            minuteGeneImpact = ImpactUtils.createGeneImpact(gene.minute, gene.minute.name)as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            secondGeneImpact = ImpactUtils.createGeneImpact(gene.second, gene.second.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): TimeGeneImpact {
        return TimeGeneImpact(
                shared.copy(),
                specific.copy(),
                hourGeneImpact = hourGeneImpact.copy(),
                minuteGeneImpact = minuteGeneImpact.copy(),
                secondGeneImpact = secondGeneImpact.copy())
    }

    override fun clone(): TimeGeneImpact {
        return TimeGeneImpact(
                shared.clone(),specific.clone(), hourGeneImpact = hourGeneImpact.clone(), minuteGeneImpact = minuteGeneImpact.clone(), secondGeneImpact = secondGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is TimeGene


    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current !is TimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be TimeGene")

        if (gc.previous != null && gc.previous !is TimeGene )
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be TimeGene")

        if (gc.previous == null || !gc.current.hour.containsSameValueAs((gc.previous as TimeGene).hour))
            hourGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.minute.containsSameValueAs((gc.previous as TimeGene).minute))
            minuteGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.second.containsSameValueAs((gc.previous as TimeGene).second))
            secondGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-hourGeneImpact" to hourGeneImpact,
                "${getId()}-minuteGeneImpact" to minuteGeneImpact,
                "${getId()}-secondGeneImpact" to secondGeneImpact
        )
    }
}
package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.TimeGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-16
 */
class TimeGeneImpact(
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val hourGeneImpact: IntegerGeneImpact,
        val minuteGeneImpact: IntegerGeneImpact,
        val secondGeneImpact : IntegerGeneImpact
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, gene : TimeGene)
            : this(id,
            hourGeneImpact = ImpactUtils.createGeneImpact(gene.hour, gene.hour.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            minuteGeneImpact = ImpactUtils.createGeneImpact(gene.minute, gene.minute.name)as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            secondGeneImpact = ImpactUtils.createGeneImpact(gene.second, gene.second.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): TimeGeneImpact {
        return TimeGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                hourGeneImpact = hourGeneImpact.copy(),
                minuteGeneImpact = minuteGeneImpact.copy(),
                secondGeneImpact = secondGeneImpact.copy())
    }

    override fun validate(gene: Gene): Boolean = gene is TimeGene


    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current !is TimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be TimeGene")

        if (gc.previous != null && gc.previous !is TimeGene )
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be TimeGene")

        if (gc.previous == null || !gc.current.hour.containsSameValueAs((gc.previous as TimeGene).hour))
            hourGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.minute.containsSameValueAs((gc.previous as TimeGene).minute))
            minuteGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.second.containsSameValueAs((gc.previous as TimeGene).second))
            secondGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-hourGeneImpact" to hourGeneImpact,
                "$id-minuteGeneImpact" to minuteGeneImpact,
                "$id-secondGeneImpact" to secondGeneImpact
        )
    }
}
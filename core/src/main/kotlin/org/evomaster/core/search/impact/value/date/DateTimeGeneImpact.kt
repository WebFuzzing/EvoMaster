package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateTimeGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext

/**
 * created by manzh on 2019-09-16
 */

class DateTimeGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val dateGeneImpact: DateGeneImpact,
        val timeGeneImpact: TimeGeneImpact
)  : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, gene : DateTimeGene)
            : this(id,
            dateGeneImpact = ImpactUtils.createGeneImpact(gene.date, gene.date.name) as? DateGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            timeGeneImpact = ImpactUtils.createGeneImpact(gene.time, gene.time.name)as? TimeGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateTimeGeneImpact {
        return DateTimeGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                dateGeneImpact = dateGeneImpact.copy(),
                timeGeneImpact = timeGeneImpact.copy())
    }

    override fun validate(gene: Gene): Boolean = gene is DateTimeGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current !is DateTimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateTimeGene")

        if (gc.previous != null && gc.previous !is DateTimeGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateTimeGene")

        if (gc.previous == null || !gc.current.date.containsSameValueAs((gc.previous as DateTimeGene).date)){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).date, current = gc.current.date)
            dateGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
        if (gc.previous == null || !gc.current.time.containsSameValueAs((gc.previous as DateTimeGene).time)){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).time, current = gc.current.time)
            timeGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("$id-dateGeneImpact" to dateGeneImpact).plus(dateGeneImpact.flatViewInnerImpact()).plus(mapOf("$id-timeGeneImpact" to timeGeneImpact)).plus(timeGeneImpact.flatViewInnerImpact())
    }

}
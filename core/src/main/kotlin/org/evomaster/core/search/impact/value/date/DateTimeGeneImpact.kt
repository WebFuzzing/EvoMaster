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
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0,
        niCounter : Int = 0,
        val dateGeneImpact: DateGeneImpact,
        val timeGeneImpact: TimeGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter) {

    constructor(id: String, gene : DateTimeGene)
            : this(id,
            dateGeneImpact = ImpactUtils.createGeneImpact(gene.date, gene.date.name) as? DateGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            timeGeneImpact = ImpactUtils.createGeneImpact(gene.time, gene.time.name)as? TimeGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateTimeGeneImpact {
        return DateTimeGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,dateGeneImpact, timeGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is DateTimeGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {

        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.previous == null && hasImpact) return

        if (gc.current !is DateTimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateTimeGene")

        if (gc.previous != null && gc.previous !is DateTimeGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateTimeGene")

        if (gc.previous == null || !gc.current.date.containsSameValueAs((gc.previous as DateTimeGene).date)){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).date, current = gc.current.date)
            dateGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
        }
        if (gc.previous == null || !gc.current.time.containsSameValueAs((gc.previous as DateTimeGene).time)){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).time, current = gc.current.time)
            timeGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
        }
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("$id-dateGeneImpact" to dateGeneImpact).plus(dateGeneImpact.flatViewInnerImpact()).plus(mapOf("$id-timeGeneImpact" to timeGeneImpact)).plus(timeGeneImpact.flatViewInnerImpact())
    }

}
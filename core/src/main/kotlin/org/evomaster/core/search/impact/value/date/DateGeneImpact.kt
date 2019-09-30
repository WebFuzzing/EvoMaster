package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DateGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0,
        val yearGeneImpact: IntegerGeneImpact,
        val monthGeneImpact: IntegerGeneImpact,
        val dayGeneImpact : IntegerGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    constructor(id: String, gene : DateGene)
            : this(id,
            yearGeneImpact = ImpactUtils.createGeneImpact(gene.year, gene.year.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            monthGeneImpact = ImpactUtils.createGeneImpact(gene.month, gene.month.name)as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            dayGeneImpact = ImpactUtils.createGeneImpact(gene.day, gene.day.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateGeneImpact {
        return DateGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, yearGeneImpact, monthGeneImpact, dayGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is DateGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {

        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.previous == null && hasImpact) return
        if (gc.current !is DateGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateGene")
        if (gc.previous !=null && gc.previous !is DateGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateGene")

        if (gc.previous == null || !gc.current.year.containsSameValueAs((gc.previous as DateGene).year))
            yearGeneImpact.countImpactAndPerformance(hasImpact, noImprovement  = noImprovement)
        if (gc.previous == null || !gc.current.month.containsSameValueAs((gc.previous as DateGene).month))
            monthGeneImpact.countImpactAndPerformance(hasImpact, noImprovement = noImprovement)
        if (gc.previous == null || !gc.current.day.containsSameValueAs((gc.previous as DateGene).day))
            dayGeneImpact.countImpactAndPerformance(hasImpact, noImprovement = noImprovement)
    }

}
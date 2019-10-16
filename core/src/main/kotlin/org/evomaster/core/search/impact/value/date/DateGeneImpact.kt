package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
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
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val yearGeneImpact: IntegerGeneImpact,
        val monthGeneImpact: IntegerGeneImpact,
        val dayGeneImpact : IntegerGeneImpact
)  : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, gene : DateGene)
            : this(id,
            yearGeneImpact = ImpactUtils.createGeneImpact(gene.year, gene.year.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            monthGeneImpact = ImpactUtils.createGeneImpact(gene.month, gene.month.name)as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            dayGeneImpact = ImpactUtils.createGeneImpact(gene.day, gene.day.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateGeneImpact {
        return DateGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                yearGeneImpact = yearGeneImpact.copy(),
                monthGeneImpact = monthGeneImpact.copy(),
                dayGeneImpact = dayGeneImpact.copy())
    }

    override fun validate(gene: Gene): Boolean = gene is DateGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is DateGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateGene")
        if (gc.previous !=null && gc.previous !is DateGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateGene")

        if (gc.previous == null || !gc.current.year.containsSameValueAs((gc.previous as DateGene).year))
            yearGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.month.containsSameValueAs((gc.previous as DateGene).month))
            monthGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.day.containsSameValueAs((gc.previous as DateGene).day))
            dayGeneImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-yearGeneImpact" to yearGeneImpact,
                "$id-monthGeneImpact" to monthGeneImpact,
                "$id-dayGeneImpact" to dayGeneImpact
        )
    }
}
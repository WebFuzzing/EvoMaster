package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DateGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                      val yearGeneImpact: IntegerGeneImpact,
                      val monthGeneImpact: IntegerGeneImpact,
                      val dayGeneImpact : IntegerGeneImpact
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
            yearGeneImpact: IntegerGeneImpact,
            monthGeneImpact: IntegerGeneImpact,
            dayGeneImpact : IntegerGeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            yearGeneImpact, monthGeneImpact, dayGeneImpact)

    constructor(id: String, gene : DateGene)
            : this(id,
            yearGeneImpact = ImpactUtils.createGeneImpact(gene.year, gene.year.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            monthGeneImpact = ImpactUtils.createGeneImpact(gene.month, gene.month.name)as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            dayGeneImpact = ImpactUtils.createGeneImpact(gene.day, gene.day.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateGeneImpact {
        return DateGeneImpact(
                shared.copy(),
                specific.copy(),
                yearGeneImpact = yearGeneImpact.copy(),
                monthGeneImpact = monthGeneImpact.copy(),
                dayGeneImpact = dayGeneImpact.copy())
    }

    override fun clone(): DateGeneImpact {
        return DateGeneImpact(
                shared.clone(),specific.clone(), yearGeneImpact.clone(), monthGeneImpact.clone(), dayGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DateGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets:Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is DateGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateGene")
        if (gc.previous !=null && gc.previous !is DateGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateGene")

        if (gc.previous == null || !gc.current.year.containsSameValueAs((gc.previous as DateGene).year))
            yearGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.month.containsSameValueAs((gc.previous as DateGene).month))
            monthGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null || !gc.current.day.containsSameValueAs((gc.previous as DateGene).day))
            dayGeneImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-yearGeneImpact" to yearGeneImpact,
                "${getId()}-monthGeneImpact" to monthGeneImpact,
                "${getId()}-dayGeneImpact" to dayGeneImpact
        )
    }
}
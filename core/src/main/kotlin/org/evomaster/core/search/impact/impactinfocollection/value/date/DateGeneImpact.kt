package org.evomaster.core.search.impact.impactinfocollection.value.date

import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DateGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                      val yearGeneImpact: IntegerGeneImpact,
                      val monthGeneImpact: IntegerGeneImpact,
                      val dayGeneImpact : IntegerGeneImpact
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id: String, gene : DateGene)
            : this(SharedImpactInfo(id), SpecificImpactInfo(),
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

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is DateGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateGene")
        if (gc.previous !=null && gc.previous !is DateGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateGene")

        val impacts = mutableListOf<IntegerGeneImpact>()
        if (gc.previous == null || !gc.current.year.containsSameValueAs((gc.previous as DateGene).year))
            impacts.add(yearGeneImpact)
        if (gc.previous == null || !gc.current.month.containsSameValueAs((gc.previous as DateGene).month))
            impacts.add(monthGeneImpact)
        if (gc.previous == null || !gc.current.day.containsSameValueAs((gc.previous as DateGene).day))
            impacts.add(dayGeneImpact)

        val num = impacts.size
        if (impacts.isEmpty()) return

        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        impacts.forEach {
            it.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = num)
        }
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-${yearGeneImpact.getId()}" to yearGeneImpact,
                "${getId()}-${monthGeneImpact.getId()}" to monthGeneImpact,
                "${getId()}-${dayGeneImpact.getId()}" to dayGeneImpact
        )
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(yearGeneImpact, monthGeneImpact, dayGeneImpact)
    }
}
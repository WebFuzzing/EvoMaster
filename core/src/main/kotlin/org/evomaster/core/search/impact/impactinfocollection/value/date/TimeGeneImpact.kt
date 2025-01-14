package org.evomaster.core.search.impact.impactinfocollection.value.date

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.OptionalGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-16
 */
class TimeGeneImpact(
    sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
    val hourGeneImpact: IntegerGeneImpact,
    val minuteGeneImpact: IntegerGeneImpact,
    val secondGeneImpact: IntegerGeneImpact,
    val millisecondGeneImpact: OptionalGeneImpact,
    val offsetGeneImpact: TimeOffsetGeneImpact
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id: String, gene : TimeGene)
            : this(SharedImpactInfo(id), SpecificImpactInfo(),
            hourGeneImpact = ImpactUtils.createGeneImpact(gene.hour, gene.hour.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            minuteGeneImpact = ImpactUtils.createGeneImpact(gene.minute, gene.minute.name)as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            secondGeneImpact = ImpactUtils.createGeneImpact(gene.second, gene.second.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            millisecondGeneImpact = ImpactUtils.createGeneImpact(gene.millisecond, gene.millisecond.name) as? OptionalGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            offsetGeneImpact = ImpactUtils.createGeneImpact(gene.offset, gene.offset.name) as? TimeOffsetGeneImpact ?:throw IllegalStateException("TimeOffsetGeneImpact should be created")
    )

    override fun copy(): TimeGeneImpact {
        return TimeGeneImpact(
                shared.copy(),
                specific.copy(),
                hourGeneImpact = hourGeneImpact.copy(),
                minuteGeneImpact = minuteGeneImpact.copy(),
                secondGeneImpact = secondGeneImpact.copy(),
                millisecondGeneImpact = millisecondGeneImpact.copy(),
                offsetGeneImpact = offsetGeneImpact.copy())
    }

    override fun clone(): TimeGeneImpact {
        return TimeGeneImpact(
                shared.clone(),specific.clone(), hourGeneImpact = hourGeneImpact.clone(), minuteGeneImpact = minuteGeneImpact.clone(), secondGeneImpact = secondGeneImpact.clone(), millisecondGeneImpact = millisecondGeneImpact.clone(), offsetGeneImpact = offsetGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is TimeGene


    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current !is TimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be TimeGene")

        if (gc.previous != null && gc.previous !is TimeGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be TimeGene")

        val innerImpacts = mutableListOf<GeneImpact>()

        if (gc.previous == null || !gc.current.hour.containsSameValueAs((gc.previous as TimeGene).hour))
            innerImpacts.add(hourGeneImpact)
        if (gc.previous == null || !gc.current.minute.containsSameValueAs((gc.previous as TimeGene).minute))
            innerImpacts.add(minuteGeneImpact)
        if (gc.previous == null || !gc.current.second.containsSameValueAs((gc.previous as TimeGene).second))
            innerImpacts.add(secondGeneImpact)

        if (gc.previous == null || !gc.current.millisecond.containsSameValueAs((gc.previous as TimeGene).millisecond))
            innerImpacts.add(millisecondGeneImpact)

        if (gc.previous == null || !gc.current.offset.containsSameValueAs((gc.previous as TimeGene).offset))
            innerImpacts.add(offsetGeneImpact)

        if (innerImpacts.isEmpty()) return
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        innerImpacts.forEach {
            it.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation,num = innerImpacts.size)
        }

    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-${hourGeneImpact.getId()}" to hourGeneImpact,
                "${getId()}-${minuteGeneImpact.getId()}" to minuteGeneImpact,
                "${getId()}-${secondGeneImpact.getId()}" to secondGeneImpact,
                "${getId()}-${millisecondGeneImpact.getId()}" to millisecondGeneImpact,
                "${getId()}-${offsetGeneImpact.getId()}" to offsetGeneImpact
        )
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(hourGeneImpact, minuteGeneImpact, secondGeneImpact, millisecondGeneImpact, offsetGeneImpact)
    }
}
package org.evomaster.core.search.impact.impactinfocollection.value.date

import org.evomaster.core.search.gene.DateTimeGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2019-09-16
 */

class DateTimeGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                         val dateGeneImpact: DateGeneImpact,
                         val timeGeneImpact: TimeGeneImpact
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf(),
            dateGeneImpact: DateGeneImpact,
            timeGeneImpact: TimeGeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            dateGeneImpact, timeGeneImpact)

    constructor(id: String, gene : DateTimeGene)
            : this(id,
            dateGeneImpact = ImpactUtils.createGeneImpact(gene.date, gene.date.name) as? DateGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            timeGeneImpact = ImpactUtils.createGeneImpact(gene.time, gene.time.name)as? TimeGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateTimeGeneImpact {
        return DateTimeGeneImpact(
                shared.copy(),
                specific.copy(),
                dateGeneImpact = dateGeneImpact.copy(),
                timeGeneImpact = timeGeneImpact.copy())
    }
    override fun clone(): DateTimeGeneImpact {
        return DateTimeGeneImpact(
                shared.clone(), specific.clone(), dateGeneImpact = dateGeneImpact.clone(), timeGeneImpact = timeGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DateTimeGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current !is DateTimeGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be DateTimeGene")

        if (gc.previous != null && gc.previous !is DateTimeGene)
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be DateTimeGene")

        val dateMutated = gc.previous == null || !gc.current.date.containsSameValueAs((gc.previous as DateTimeGene).date)
        val timeMutated = gc.previous == null || !gc.current.time.containsSameValueAs((gc.previous as DateTimeGene).time)

        val num = (if (dateMutated) 1 else 0 ) + (if (timeMutated) 1 else 0)
        if (num == 0) return

        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (dateMutated){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).date, current = gc.current.date, numOfMutatedGene = num)
            dateGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets=noImpactTargets,impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
        if (timeMutated){
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (gc.previous==null) null else (gc.previous as DateTimeGene).time, current = gc.current.time, numOfMutatedGene = num)
            timeGeneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets =noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-dateGeneImpact" to dateGeneImpact).plus(dateGeneImpact.flatViewInnerImpact()).plus(mapOf("${getId()}-timeGeneImpact" to timeGeneImpact)).plus(timeGeneImpact.flatViewInnerImpact())
    }

}
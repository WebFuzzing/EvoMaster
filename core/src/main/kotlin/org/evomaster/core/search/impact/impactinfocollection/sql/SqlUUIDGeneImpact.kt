package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.LongGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlUUIDGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                         val mostSigBitsImpact: LongGeneImpact,
                         val leastSigBitsImpact : LongGeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){
    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf(),
            mostSigBitsImpact: LongGeneImpact,
            leastSigBitsImpact : LongGeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            mostSigBitsImpact,
            leastSigBitsImpact
    )

    constructor(id : String, UUIDGene: UUIDGene) : this(
            id,
            mostSigBitsImpact = ImpactUtils.createGeneImpact(UUIDGene.mostSigBits, id) as? LongGeneImpact
                    ?: throw IllegalStateException("mostSigBitsImpact should be LongGeneImpact"),
            leastSigBitsImpact = ImpactUtils.createGeneImpact(UUIDGene.leastSigBits, id) as? LongGeneImpact
                    ?: throw IllegalStateException("leastSigBitsImpact should be LongGeneImpact")
    )

    override fun copy(): SqlUUIDGeneImpact {
        return SqlUUIDGeneImpact(
                shared.copy(),
                specific.copy(),
                mostSigBitsImpact = mostSigBitsImpact.copy(),
                leastSigBitsImpact = leastSigBitsImpact.copy())
    }

    override fun clone(): SqlUUIDGeneImpact {
        return SqlUUIDGeneImpact(
                shared.clone(),
                specific.clone(),
                mostSigBitsImpact = mostSigBitsImpact.clone(),
                leastSigBitsImpact = leastSigBitsImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext,noImpactTargets :Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.previous != null && gc.previous !is UUIDGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.current  !is UUIDGene){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }

        val mutatedMost = gc.previous == null || !gc.current.mostSigBits.containsSameValueAs((gc.previous as UUIDGene).mostSigBits)
        val mutatedLeast = gc.previous == null || !gc.current.leastSigBits.containsSameValueAs((gc.previous as UUIDGene).leastSigBits)
        val num = (if (mutatedLeast) 1 else 0) + (if (mutatedMost) 1 else 0)

        if (mutatedMost){
            mostSigBitsImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = num)
        }
        if (mutatedLeast){
            leastSigBitsImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num =  num)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is UUIDGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-${mostSigBitsImpact.getId()}" to mostSigBitsImpact,
                "${getId()}-${leastSigBitsImpact.getId()}" to leastSigBitsImpact
        )
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(mostSigBitsImpact, leastSigBitsImpact)
    }
}
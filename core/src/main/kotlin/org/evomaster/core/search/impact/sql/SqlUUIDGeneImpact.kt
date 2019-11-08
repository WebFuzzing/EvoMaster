package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlUUIDGene
import org.evomaster.core.search.impact.*
import org.evomaster.core.search.impact.value.numeric.LongGeneImpact

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
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            mostSigBitsImpact: LongGeneImpact,
            leastSigBitsImpact : LongGeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            mostSigBitsImpact,
            leastSigBitsImpact
    )

    constructor(id : String, sqlUUIDGene: SqlUUIDGene) : this(
            id,
            mostSigBitsImpact = ImpactUtils.createGeneImpact(sqlUUIDGene.mostSigBits, id) as? LongGeneImpact
                    ?: throw IllegalStateException("mostSigBitsImpact should be LongGeneImpact"),
            leastSigBitsImpact = ImpactUtils.createGeneImpact(sqlUUIDGene.leastSigBits, id) as? LongGeneImpact
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
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.previous != null && gc.previous !is SqlUUIDGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.current  !is SqlUUIDGene){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }

        if (gc.previous == null || !gc.current.mostSigBits.containsSameValueAs((gc.previous as SqlUUIDGene).mostSigBits)){
            mostSigBitsImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
        if (gc.previous == null || !gc.current.leastSigBits.containsSameValueAs((gc.previous as SqlUUIDGene).leastSigBits)){
            leastSigBitsImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is SqlUUIDGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-mostSigBitsImpact" to mostSigBitsImpact,
                "${getId()}-leastSigBitsImpact" to leastSigBitsImpact
        )
    }
}
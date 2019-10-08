package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlUUIDGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.LongGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlUUIDGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val mostSigBitsImpact: LongGeneImpact,
        val leastSigBitsImpact : LongGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,positionSensitive) {

    constructor(id : String, sqlUUIDGene: SqlUUIDGene) : this(
            id,
            mostSigBitsImpact = ImpactUtils.createGeneImpact(sqlUUIDGene.mostSigBits, id) as? LongGeneImpact?: throw IllegalStateException("mostSigBitsImpact of SqlJSONImpact should be ObjectGeneImpact"),
            leastSigBitsImpact = ImpactUtils.createGeneImpact(sqlUUIDGene.leastSigBits, id) as? LongGeneImpact?: throw IllegalStateException("leastSigBitsImpact of SqlJSONImpact should be ObjectGeneImpact")
    )

    override fun copy(): SqlUUIDGeneImpact {
        return SqlUUIDGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive, mostSigBitsImpact.copy(), leastSigBitsImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)
        if (gc.previous == null && hasImpact) return
        if (gc.previous != null && gc.previous !is SqlUUIDGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.current  !is SqlUUIDGene){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }

        if (gc.previous == null || !gc.current.mostSigBits.containsSameValueAs((gc.previous as SqlUUIDGene).mostSigBits)){
            mostSigBitsImpact.countImpactAndPerformance(hasImpact, noImprovement)
        }
        if (gc.previous == null || !gc.current.leastSigBits.containsSameValueAs((gc.previous as SqlUUIDGene).leastSigBits)){
            leastSigBitsImpact.countImpactAndPerformance(hasImpact, noImprovement)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is SqlUUIDGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-mostSigBitsImpact" to mostSigBitsImpact,
                "$id-leastSigBitsImpact" to leastSigBitsImpact
        )
    }
}
package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlNullableImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val presentImpact : BinaryGeneImpact = BinaryGeneImpact("isPresent"),
        val geneImpact: GeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,positionSensitive) {

    constructor(id : String, sqlnullGene: SqlNullable) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlnullGene.gene, id))

    override fun copy(): SqlNullableImpact {
        return SqlNullableImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive, presentImpact.copy(), geneImpact.copy() as GeneImpact)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.current  !is SqlNullable){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.current.isPresent)
            presentImpact._true.countImpactAndPerformance(hasImpact, noImprovement)
        else
            presentImpact._false.countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.previous == null && hasImpact) return

        if (gc.previous != null && gc.previous !is SqlNullable){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous == null) null else (gc.previous as SqlNullable).gene,
                current = gc.current.gene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
    }


    override fun validate(gene: Gene): Boolean = gene is SqlNullable
}
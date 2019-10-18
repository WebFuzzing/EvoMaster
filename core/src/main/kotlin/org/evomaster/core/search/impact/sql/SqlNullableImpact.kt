package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlNullableImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val presentImpact : BinaryGeneImpact = BinaryGeneImpact("isPresent"),
        val geneImpact: GeneImpact
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id : String, sqlnullGene: SqlNullable) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlnullGene.gene, id))

    override fun copy(): SqlNullableImpact {
        return SqlNullableImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                presentImpact = presentImpact.copy(),
                geneImpact = geneImpact.copy() as GeneImpact)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.current  !is SqlNullable){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.previous != null && gc.previous !is SqlNullable){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }

        if (gc.previous == null || (gc.previous as SqlNullable).isPresent != gc.current.isPresent){
            presentImpact.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

            if (gc.current.isPresent)
                presentImpact.trueValue.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            else
                presentImpact.falseValue.countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

            if (gc.previous != null) {
                return
            }
        }

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current.isPresent){
            val mutatedGeneWithContext = MutatedGeneWithContext(
                    previous = if (gc.previous == null) null else (gc.previous as SqlNullable).gene,
                    current = gc.current.gene
            )
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is SqlNullable

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-presentImpact" to presentImpact
        ).plus(presentImpact.flatViewInnerImpact()).plus("$id-geneImpact" to geneImpact).plus(geneImpact.flatViewInnerImpact())
    }
}
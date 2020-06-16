package org.evomaster.core.search.impact.impactInfoCollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.impact.impactInfoCollection.*
import org.evomaster.core.search.impact.impactInfoCollection.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlNullableImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                        val presentImpact : BinaryGeneImpact,
                        val geneImpact: GeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){
    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            presentImpact : BinaryGeneImpact = BinaryGeneImpact("isPresent"),
            geneImpact: GeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            presentImpact,
            geneImpact)

    constructor(id : String, sqlnullGene: SqlNullable) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlnullGene.gene, id))

    override fun copy(): SqlNullableImpact {
        return SqlNullableImpact(
                shared.copy(),
                specific.copy(),
                presentImpact = presentImpact.copy(),
                geneImpact = geneImpact.copy())
    }

    override fun clone(): SqlNullableImpact {
        return SqlNullableImpact(
                shared.clone(),specific.clone(), presentImpact.clone(), geneImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext,noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.current  !is SqlNullable){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.previous != null && gc.previous !is SqlNullable){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }

        if (gc.previous == null || (gc.previous as SqlNullable).isPresent != gc.current.isPresent){
            presentImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

            if (gc.current.isPresent)
                presentImpact.trueValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            else
                presentImpact.falseValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

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
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets= noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is SqlNullable

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-presentImpact" to presentImpact
        ).plus(presentImpact.flatViewInnerImpact()).plus("${getId()}-geneImpact" to geneImpact).plus(geneImpact.flatViewInnerImpact())
    }
}
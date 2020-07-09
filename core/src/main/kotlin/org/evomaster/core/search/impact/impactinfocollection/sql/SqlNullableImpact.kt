package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlNullableImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                        val presentImpact : BinaryGeneImpact = BinaryGeneImpact("isPresent"),
                        val geneImpact: GeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){


    constructor(id : String, sqlnullGene: SqlNullable) : this(SharedImpactInfo(id), SpecificImpactInfo(), geneImpact = ImpactUtils.createGeneImpact(sqlnullGene.gene, id))

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
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.current  !is SqlNullable){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlNullable")
        }
        if (gc.previous != null && gc.previous !is SqlNullable){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlNullable")
        }

        if (gc.previous == null || (gc.previous as SqlNullable).isPresent != gc.current.isPresent){
            presentImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

            if (gc.current.isPresent)
                presentImpact.trueValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
            else
                presentImpact.falseValue.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)

            if (gc.previous != null) {
                return
            }
        }

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current.isPresent){
            val mutatedGeneWithContext = MutatedGeneWithContext(
                    previous = if (gc.previous == null) null else (gc.previous as SqlNullable).gene,
                    current = gc.current.gene,
                    numOfMutatedGene = gc.numOfMutatedGene
            )
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets= noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is SqlNullable

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${geneImpact.getId()}" to geneImpact)
                .plus("${getId()}-${presentImpact.getId()}" to presentImpact)
                .plus(presentImpact.flatViewInnerImpact().plus(geneImpact.flatViewInnerImpact()).map { "${getId()}-${it.key}" to it.value })
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(presentImpact, geneImpact)
    }

    override fun syncImpact(previous: Gene?, current: Gene) {
        check(previous,current)
        geneImpact.syncImpact((previous as SqlNullable).gene, (current as SqlNullable).gene)
    }
}
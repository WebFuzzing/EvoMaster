package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext

/**
 * created by manzh on 2019-09-29
 */
class SqlPrimaryKeyGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
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

    constructor(id : String, sqlPrimaryKeyGene: SqlPrimaryKeyGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlPrimaryKeyGene.gene, id))

    override fun copy(): SqlPrimaryKeyGeneImpact {
        return SqlPrimaryKeyGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap().toMutableMap(),
                geneImpact = geneImpact.copy() as GeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is SqlPrimaryKeyGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current  !is SqlPrimaryKeyGene){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlPrimaryKeyGene")
        }
        if (gc.previous != null && gc.previous !is SqlPrimaryKeyGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlPrimaryKeyGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as SqlPrimaryKeyGene).gene,
                current = gc.current.gene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }
}
package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2019-09-29
 */
class SqlPrimaryKeyGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                               val geneImpact: GeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){


    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf(),
            geneImpact: GeneImpact
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            geneImpact)

    constructor(id : String, sqlPrimaryKeyGene: SqlPrimaryKeyGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlPrimaryKeyGene.gene, id))

    override fun copy(): SqlPrimaryKeyGeneImpact {
        return SqlPrimaryKeyGeneImpact(
                shared.copy(),
                specific.copy(),
                geneImpact = geneImpact.copy())
    }

    override fun clone(): SqlPrimaryKeyGeneImpact {
        return SqlPrimaryKeyGeneImpact(
                shared.clone(),
                specific.clone(),
                geneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is SqlPrimaryKeyGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current  !is SqlPrimaryKeyGene){
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlPrimaryKeyGene")
        }
        if (gc.previous != null && gc.previous !is SqlPrimaryKeyGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlPrimaryKeyGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as SqlPrimaryKeyGene).gene,
                current = gc.current.gene,
                numOfMutatedGene = gc.numOfMutatedGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets =noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }
}
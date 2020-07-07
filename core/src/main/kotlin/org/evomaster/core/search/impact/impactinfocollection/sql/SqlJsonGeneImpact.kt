package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlJSONGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlJsonGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo, val geneImpact: ObjectGeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf(),
            geneImpact: ObjectGeneImpact
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement), geneImpact)

    constructor(id : String, sqlJSONGene: SqlJSONGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlJSONGene.objectGene, id) as? ObjectGeneImpact?:throw IllegalStateException("geneImpact of SqlJSONImpact should be ObjectGeneImpact"))

    override fun copy(): SqlJsonGeneImpact {
        return SqlJsonGeneImpact(
                shared.copy(), specific.copy(), geneImpact.copy())
    }

    override fun clone(): SqlJsonGeneImpact {
        return SqlJsonGeneImpact(
                shared.clone(),specific.clone(), geneImpact.clone()
        )
    }
    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean){
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.current !is SqlJSONGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlJSONGene")
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.previous == null){
            val mutatedGeneWithContext = MutatedGeneWithContext(
                    previous = null,
                    current = gc.current.objectGene,
                    numOfMutatedGene = gc.numOfMutatedGene
            )
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            return
        }
        if ( gc.previous !is SqlJSONGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlJSONGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = gc.previous.objectGene,
                current = gc.current.objectGene,
                numOfMutatedGene = gc.numOfMutatedGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
    }
    override fun validate(gene: Gene): Boolean = gene is SqlJSONGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-geneImpact" to geneImpact).plus(geneImpact.flatViewInnerImpact())
    }
}
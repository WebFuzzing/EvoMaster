package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlXmlGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo, val geneImpact: ObjectGeneImpact) : GeneImpact(sharedImpactInfo, specificImpactInfo){

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

    constructor(id : String, sqlXMLGene: SqlXMLGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlXMLGene.objectGene, id) as? ObjectGeneImpact?:throw IllegalStateException("geneImpact of SqlJSONImpact should be ObjectGeneImpact"))

    override fun copy(): SqlXmlGeneImpact {
        return SqlXmlGeneImpact(
                shared.copy(),
                specific.copy(),
                geneImpact = geneImpact.copy())
    }

    override fun clone(): SqlXmlGeneImpact {
        return SqlXmlGeneImpact(
                shared.clone(),specific.clone(), geneImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current  !is SqlXMLGene )
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")

        if ( gc.previous != null && gc.previous !is SqlXMLGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) and gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as SqlXMLGene).objectGene,
                current = gc.current.objectGene,
                numOfMutatedGene = gc.numOfMutatedGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

    }

    override fun validate(gene: Gene): Boolean = gene is SqlXMLGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "${getId()}-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }
}
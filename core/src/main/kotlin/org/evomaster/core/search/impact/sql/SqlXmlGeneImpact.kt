package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.ObjectGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlXmlGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val geneImpact: ObjectGeneImpact
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id : String, sqlXMLGene: SqlXMLGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlXMLGene.objectGene, id) as? ObjectGeneImpact?:throw IllegalStateException("geneImpact of SqlJSONImpact should be ObjectGeneImpact"))

    override fun copy(): SqlXmlGeneImpact {
        return SqlXmlGeneImpact(
                id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                geneImpact = geneImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        if (gc.current  !is SqlXMLGene )
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")

        if ( gc.previous != null && gc.previous !is SqlXMLGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) and gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as SqlXMLGene).objectGene,
                current = gc.current.objectGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)

    }

    override fun validate(gene: Gene): Boolean = gene is SqlXMLGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }
}
package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.ObjectGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlXmlGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val geneImpact: ObjectGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive) {

    constructor(id : String, sqlXMLGene: SqlXMLGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlXMLGene.objectGene, id) as? ObjectGeneImpact?:throw IllegalStateException("geneImpact of SqlJSONImpact should be ObjectGeneImpact"))

    override fun copy(): SqlXmlGeneImpact {
        return SqlXmlGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter,niCounter, positionSensitive, geneImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {

        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.previous == null && hasImpact) return

        if (gc.current  !is SqlXMLGene )
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")

        if ( gc.previous != null && gc.previous !is SqlXMLGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) and gc.current (${gc.current::class.java.simpleName}) should be SqlXMLGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = if (gc.previous==null) null else (gc.previous as SqlXMLGene).objectGene,
                current = gc.current.objectGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
    }

    override fun validate(gene: Gene): Boolean = gene is SqlXMLGene
}
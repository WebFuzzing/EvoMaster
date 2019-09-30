package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlJSONGene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.ObjectGeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlJsonGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val geneImpact: ObjectGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    constructor(id : String, sqlJSONGene: SqlJSONGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlJSONGene.objectGene, id) as? ObjectGeneImpact?:throw IllegalStateException("geneImpact of SqlJSONImpact should be ObjectGeneImpact"))

    override fun copy(): SqlJsonGeneImpact {
        return SqlJsonGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, geneImpact.copy())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.current !is SqlJSONGene)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlJSONGene")
        if (gc.previous == null && hasImpact) return
        if (gc.previous == null){
            val mutatedGeneWithContext = MutatedGeneWithContext(
                    previous = null,
                    current = gc.current.objectGene
            )
            geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
            return
        }
        if ( gc.previous !is SqlJSONGene){
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlJSONGene")
        }

        val mutatedGeneWithContext = MutatedGeneWithContext(
                previous = gc.previous.objectGene,
                current = gc.current.objectGene
        )
        geneImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
    }
    override fun validate(gene: Gene): Boolean = gene is SqlJSONGene
}
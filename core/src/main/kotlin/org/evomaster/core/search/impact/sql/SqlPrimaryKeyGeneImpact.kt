package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils

/**
 * created by manzh on 2019-09-29
 */
class SqlPrimaryKeyGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val geneImpact: GeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive) {

    constructor(id : String, sqlPrimaryKeyGene: SqlPrimaryKeyGene) : this(id, geneImpact = ImpactUtils.createGeneImpact(sqlPrimaryKeyGene.gene, id))

    override fun copy(): SqlPrimaryKeyGeneImpact {
        return SqlPrimaryKeyGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive, geneImpact.copy() as GeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is SqlPrimaryKeyGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf(
                "$id-geneImpact" to geneImpact
        ).plus(geneImpact.flatViewInnerImpact())
    }
}
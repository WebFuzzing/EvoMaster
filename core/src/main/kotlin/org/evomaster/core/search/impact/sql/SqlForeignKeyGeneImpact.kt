package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.impact.GeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlForeignKeyGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false
): GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,positionSensitive) {

    override fun copy(): SqlForeignKeyGeneImpact {
        return SqlForeignKeyGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter,positionSensitive)
    }

    override fun validate(gene: Gene): Boolean = gene is SqlForeignKeyGene
}
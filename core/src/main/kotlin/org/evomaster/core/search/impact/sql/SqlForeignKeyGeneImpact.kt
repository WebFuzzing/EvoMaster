package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.impact.GeneImpact

/**
 * created by manzh on 2019-09-29
 */
class SqlForeignKeyGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf()
): GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    override fun copy(): SqlForeignKeyGeneImpact {
        return SqlForeignKeyGeneImpact(id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is SqlForeignKeyGene
}
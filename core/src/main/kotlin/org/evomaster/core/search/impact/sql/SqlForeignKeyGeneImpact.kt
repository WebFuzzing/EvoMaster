package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.SharedImpactInfo
import org.evomaster.core.search.impact.SpecificImpactInfo

/**
 * created by manzh on 2019-09-29
 */
class SqlForeignKeyGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf()
    ) : this(SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact), SpecificImpactInfo(noImpactFromImpact, noImprovement))

    override fun copy(): SqlForeignKeyGeneImpact {
        return SqlForeignKeyGeneImpact(
                shared.copy(), specific.copy()
        )
    }

    override fun clone(): SqlForeignKeyGeneImpact {
        return SqlForeignKeyGeneImpact(
            shared.clone(), specific.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is SqlForeignKeyGene
}
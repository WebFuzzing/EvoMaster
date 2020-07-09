package org.evomaster.core.search.impact.impactinfocollection.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo

/**
 * created by manzh on 2019-09-29
 */
class SqlForeignKeyGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

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
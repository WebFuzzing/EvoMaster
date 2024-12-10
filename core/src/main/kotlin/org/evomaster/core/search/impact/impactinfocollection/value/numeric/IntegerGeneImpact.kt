package org.evomaster.core.search.impact.impactinfocollection.value.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo

/**
 * created by manzh on 2019-09-09
 */
class IntegerGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun copy(): IntegerGeneImpact {
        return IntegerGeneImpact(
                shared.copy(),
                specific.copy()
        )
    }

    override fun clone(): IntegerGeneImpact {
        return IntegerGeneImpact(
                shared.clone(),
                specific.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is IntegerGene
}
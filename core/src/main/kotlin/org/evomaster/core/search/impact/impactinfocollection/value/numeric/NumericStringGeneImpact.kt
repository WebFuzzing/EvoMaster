package org.evomaster.core.search.impact.impactinfocollection.value.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.NumericStringGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo


class NumericStringGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun copy(): NumericStringGeneImpact {
        return NumericStringGeneImpact(
                shared.copy(), specific.copy())
    }

    override fun clone(): NumericStringGeneImpact {
        return NumericStringGeneImpact(
                shared.clone(), specific.clone())
    }

    override fun validate(gene: Gene): Boolean = gene is NumericStringGene
}
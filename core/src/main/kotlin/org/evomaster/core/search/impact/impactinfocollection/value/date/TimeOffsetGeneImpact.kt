package org.evomaster.core.search.impact.impactinfocollection.value.date

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.datetime.TimeOffsetGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

//TODO should be handle in a correct way
class TimeOffsetGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                           val typeImpact: CompositeFixedGeneImpact
) : GeneImpact(sharedImpactInfo, specificImpactInfo) {

    constructor(id: String, gene : TimeOffsetGene)
            : this(SharedImpactInfo(id), SpecificImpactInfo(),
        typeImpact = ImpactUtils.createGeneImpact(gene.type, gene.type.name) as? CompositeFixedGeneImpact ?:throw IllegalStateException("CompositeFixedGeneImpact should be created"),
    )

    override fun copy(): TimeOffsetGeneImpact {
        return TimeOffsetGeneImpact(
                shared.copy(),
                specific.copy(),
                typeImpact = typeImpact.copy()
        )
    }

    override fun clone(): TimeOffsetGeneImpact {
        return TimeOffsetGeneImpact(
                shared.clone(),specific.clone(),
            typeImpact = typeImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is TimeOffsetGene

}
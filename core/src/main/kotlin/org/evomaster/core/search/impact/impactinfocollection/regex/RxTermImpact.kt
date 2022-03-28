package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RxTerm
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo

/**
 * created by manzh on 2020-07-08
 */
open class RxTermImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo) :  GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun copy(): RxTermImpact {
        return RxTermImpact(
                shared.copy(),
                specific.copy()
        )
    }

    override fun clone(): RxTermImpact {
        return RxTermImpact(
                shared.clone(),
                specific.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is RxTerm
}
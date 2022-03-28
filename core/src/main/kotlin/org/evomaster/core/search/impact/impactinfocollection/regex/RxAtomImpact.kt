package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RxAtom
import org.evomaster.core.search.gene.regex.RxTerm
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo

/**
 * created by manzh on 2020-07-08
 */
open class RxAtomImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo) :  RxTermImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun copy(): RxAtomImpact {
        return RxAtomImpact(
                shared.copy(),
                specific.copy()
        )
    }

    override fun clone(): RxAtomImpact {
        return RxAtomImpact(
                shared.clone(),
                specific.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is RxAtom
}
package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.impact.impactinfocollection.*

/**
 * created by manzh on 2020-07-08
 */
class DisjunctionListRxGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val disjunctions: List<DisjunctionRxGeneImpact>
        ) :  RxAtomImpact(sharedImpactInfo, specificImpactInfo){


    constructor(id : String, gene : DisjunctionListRxGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), gene.disjunctions.map { ImpactUtils.createGeneImpact(it, it.name) as DisjunctionRxGeneImpact}.toList())

    override fun copy(): DisjunctionListRxGeneImpact {
        return DisjunctionListRxGeneImpact(
                shared.copy(),
                specific.copy(),
                disjunctions.map { it.copy() }
        )
    }

    override fun clone(): DisjunctionListRxGeneImpact {
        return DisjunctionListRxGeneImpact(
                shared.clone(),
                specific.clone(),
                disjunctions.map { it.clone() }
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DisjunctionListRxGene

    override fun innerImpacts(): List<Impact> {
        return disjunctions
    }
}
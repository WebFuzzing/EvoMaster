package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.DisjunctionRxGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2020-07-08
 */
class DisjunctionRxGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val termsImpact : List<GeneImpact>,
        val extraPrefix : BinaryGeneImpact = BinaryGeneImpact("extraPrefix"),
        val extraPostfix : BinaryGeneImpact = BinaryGeneImpact("extraPostfix")
) :  RxAtomImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String, gene : DisjunctionRxGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), gene.terms.map { ImpactUtils.createGeneImpact(it, it.name)}.toList())


    override fun copy(): DisjunctionRxGeneImpact {
        return DisjunctionRxGeneImpact(
                shared.copy(),
                specific.copy(),
                termsImpact.map { it.copy() },
                extraPrefix.copy(),
                extraPostfix.copy()
        )
    }

    override fun clone(): DisjunctionRxGeneImpact {
        return DisjunctionRxGeneImpact(
                shared.clone(),
                specific.clone(),
                termsImpact.map { it.clone() },
                extraPrefix.clone(),
                extraPostfix.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DisjunctionRxGene

    override fun innerImpacts(): List<Impact> {
        return termsImpact.plus(extraPostfix).plus(extraPostfix)
    }

}
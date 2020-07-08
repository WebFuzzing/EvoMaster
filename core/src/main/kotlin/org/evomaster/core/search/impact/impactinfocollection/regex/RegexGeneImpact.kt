package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.impact.impactinfocollection.SharedImpactInfo
import org.evomaster.core.search.impact.impactinfocollection.SpecificImpactInfo

/**
 * created by manzh on 2020-07-08
 */
class RegexGeneImpact(
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val listRxGeneImpact: DisjunctionListRxGeneImpact) :  GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String, gene: RegexGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), ImpactUtils.createGeneImpact(gene.disjunctions, gene.disjunctions.name) as DisjunctionListRxGeneImpact)

    override fun copy(): RegexGeneImpact {
        return RegexGeneImpact(
                shared.copy(),
                specific.copy(),
                listRxGeneImpact.copy()
        )
    }

    override fun clone(): RegexGeneImpact {
        return RegexGeneImpact(
                shared.clone(),
                specific.clone(),
                listRxGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is RegexGene

}
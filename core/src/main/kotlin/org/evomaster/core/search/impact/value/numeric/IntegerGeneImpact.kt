package org.evomaster.core.search.impact.value.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.SharedImpactInfo
import org.evomaster.core.search.impact.SpecificImpactInfo

/**
 * created by manzh on 2019-09-09
 */
class IntegerGeneImpact (sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo) : GeneImpact(sharedImpactInfo, specificImpactInfo){

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
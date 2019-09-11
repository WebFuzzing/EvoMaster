package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DisruptiveGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val geneImpact: GeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    constructor(id : String, gene: DisruptiveGene<*>) : this(id, geneImpact = ImpactUtils.createGeneImpact(gene.gene, id))

    override fun copy(): DisruptiveGeneImpact {
        return DisruptiveGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, geneImpact.copy() as GeneImpact)
    }

}
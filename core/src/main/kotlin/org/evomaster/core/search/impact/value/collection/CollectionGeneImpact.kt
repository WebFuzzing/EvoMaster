package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class CollectionGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val sizeImpact : IntegerGeneImpact = IntegerGeneImpact("size")
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    override fun copy(): CollectionGeneImpact {
        return CollectionGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, sizeImpact.copy())
    }

}
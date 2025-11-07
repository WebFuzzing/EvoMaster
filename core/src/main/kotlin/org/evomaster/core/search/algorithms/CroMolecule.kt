package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

data class Molecule<T : Individual>(
    var suite: WtsEvalIndividual<T>,
    var kineticEnergy: Double,
    var numCollisions: Int
)



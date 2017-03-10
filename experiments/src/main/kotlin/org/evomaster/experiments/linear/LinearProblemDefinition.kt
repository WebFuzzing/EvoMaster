package org.evomaster.experiments.linear

import com.google.inject.Inject
import org.evomaster.core.search.service.Randomness


class LinearProblemDefinition {

    @Inject
    lateinit var randomness : Randomness

    var nTargets = 1

    var infeasible = 0

    var range = 1000

    var disruptiveP = 0.01

    var problemType = ProblemType.GRADIENT

    var optima: MutableList<Int> = mutableListOf()

}
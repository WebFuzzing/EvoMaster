package org.evomaster.experiments.maxv

import com.google.inject.Inject
import org.evomaster.core.search.service.Randomness


class LinearProblemDefinition {

    @Inject
    lateinit var randomness : Randomness

    var nTargets = 1

    var range = 1000

    var disruptiveP = 0.01

    var optima: MutableList<Int> = mutableListOf()

}
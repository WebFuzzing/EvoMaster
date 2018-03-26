package org.evomaster.experiments.pair

import com.google.inject.Inject
import org.evomaster.core.search.service.Randomness


class PairProblemDefinition {


    @Inject
    lateinit var randomness : Randomness

    val optimaX: MutableList<Int> = mutableListOf()

    val optimaY: MutableList<Int> = mutableListOf()

    var range = 1000
}
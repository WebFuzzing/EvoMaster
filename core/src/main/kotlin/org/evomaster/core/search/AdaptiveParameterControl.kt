package org.evomaster.core.search

import com.google.inject.Inject
import org.evomaster.core.EMConfig

/**
 * Search algorithm parameters might change during the search,
 * eg based on time or fitness feedback
 */
class AdaptiveParameterControl {

    @Inject
    private lateinit var time : SearchTimeController

    @Inject
    private lateinit var configuration: EMConfig


    fun getArchiveTargetLimit() : Int {
        //TODO
        return configuration.archiveTargetLimit
    }

    fun getProbRandomSampling(): Double {

        //TODO
        return configuration.probOfRandomSampling
    }

    fun getNumberOfMutations(): Int {
        //TODO
        return configuration.startNumberOfMutations
    }
}
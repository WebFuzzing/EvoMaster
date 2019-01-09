package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig

/**
 * Specify how a REST individual was sampled.
 * This info is needed to have custom mutations of the
 * chromosome structures
 */
enum class SampleType(var description : String = "") {
    RANDOM,
    SMART,
    @EMConfig.Experimental SMART_RESOURCE,
    SMART_GET_COLLECTION
}
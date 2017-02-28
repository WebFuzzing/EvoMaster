package org.evomaster.core.problem.rest

/**
 * Specify how a REST individual was sampled.
 * This info is needed to have custom mutations of the
 * chromosome structures
 */
enum class SampleType {
    RANDOM,
    SMART,
    SMART_GET_COLLECTION
}
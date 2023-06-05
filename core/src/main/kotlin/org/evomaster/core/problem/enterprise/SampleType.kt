package org.evomaster.core.problem.enterprise

/**
 * Specify how an enterprise individual was sampled.
 * This info is needed to have custom mutations of the
 * chromosome structures
 */
enum class SampleType {
    RANDOM,
    SEEDED,
    SMART,
    SMART_GET_COLLECTION,
    SMART_RESOURCE
}

package org.evomaster.core.output.sorting

import org.evomaster.core.search.EvaluatedIndividual

/**
 * Sorting strategies will determine the order of the written test cases in the resulting suite.
 */
enum class SortingStrategy {

    /**
     * Will sort by priority:
     * - First: status codes. 500 responses go first, then 2xx and last 4xx
     * - Second: [EvaluatedIndividual] objects on based on the higher number of covered targets. The  purpose is to
     * give an example of sorting based on fitness information.
      */
    COVERED_TARGETS,

    /**
     * Will sort depending on the [ProblemType], using the following characteristics:
     * - REST: amount of path segments, status code (like in [COVERED_TARGETS]), and last [HttpVerb].
     * - GraphQL: method name, method type (query/mutation), and last amount of parameters.
     * - RPC: class name of the function being tested, function name, and last amount of parameters.
     *
     * The goal is to provide an incremental way of reading tests. For example in REST, a developer would usually test
     * the first (least path segments) and then go moving up the most complex paths.
     */
    TARGET_INCREMENTAL
    ;

}

package org.evomaster.core.output.clustering

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution

class SplitResult {
    /**
     * disjointed subsets of original split solution
     */
    lateinit var splitOutcome: List<Solution<out Individual>>

    /**
     * subset of tests finding faults, per category, if any
     */
    var executiveSummary: Solution<out Individual>? = null

    var clusteringTime: Long = 0L
}
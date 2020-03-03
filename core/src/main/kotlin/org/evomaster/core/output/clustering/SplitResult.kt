package org.evomaster.core.output.clustering

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution

class SplitResult {
    lateinit var splitOutcome: List<Solution<out Individual>>
    lateinit var executiveSummary: Solution<out Individual>
    var clusteringTime: Long = 0L
}
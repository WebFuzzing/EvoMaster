package org.evomaster.core.output.clustering

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution

class SplitResult {
    lateinit var splitOutcome: List<Solution<RestIndividual>>
    lateinit var executiveSummary: List<Solution<RestIndividual>>
}
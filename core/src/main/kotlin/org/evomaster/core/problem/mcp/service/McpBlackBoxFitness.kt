package org.evomaster.core.problem.mcp.service

import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.search.EvaluatedIndividual

class McpBlackBoxFitness : McpFitness() {

    override fun doCalculateCoverage(
        individual: McpIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<McpIndividual>? {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }
}

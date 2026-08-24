package org.evomaster.core.problem.mcp.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.problem.mcp.McpCallResult
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.mcp.McpResourceReadAction
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Blackbox fitness function for MCP servers.
 *
 * Executes each action in an [McpIndividual] sequentially via the [McpSampler]'s client
 * and scores coverage targets based on observable protocol signals (isError flag,
 * successful reads). Mirrors [GraphQLBlackBoxFitness] in structure.
 */
class McpBlackBoxFitness : McpFitness() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(McpBlackBoxFitness::class.java)
    }

    @Inject
    private lateinit var sampler: McpSampler

    private val mapper = ObjectMapper()

    override fun doCalculateCoverage(
        individual: McpIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<McpIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())
        val actionResults: MutableList<ActionResult> = mutableListOf()
        val client = sampler.getMcpClient()

        val actions = individual.seeMainExecutableActions()

        for (i in actions.indices) {
            val action = actions[i]
            val result = McpCallResult(action.getLocalId())
            actionResults.add(result)

            try {
                when (action) {
                    is McpToolCallAction -> {
                        val args = toolArguments(action.inputSchema)
                        val toolResult = client.callTool(action.toolName, args)

                        result.setIsError(toolResult.isError)
                        result.stopping = false

                        val targetId = idMapper.handleLocalTarget("tool:${action.toolName}")
                        val score = if (!toolResult.isError) 1.0 else 0.5
                        fv.updateTarget(targetId, score, i)
                    }

                    is McpResourceReadAction -> {
                        val resourceResult = client.readResource(action.resolvedUri())
                        result.setIsError(false)
                        result.stopping = false

                        val targetId = idMapper.handleLocalTarget("resource:${action.id}")
                        // A successful read (even empty contents) scores 1.0
                        fv.updateTarget(targetId, 1.0, i)
                    }

                    else -> {
                        result.stopping = false
                    }
                }
            } catch (e: Exception) {
                log.warn("Exception evaluating MCP action ${action.id}: ${e.message}")
                result.setIsError(true)
                result.stopping = true
                break
            }
        }

        return EvaluatedIndividual(
            fv,
            individual.copy() as McpIndividual,
            actionResults,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals,
            config = config
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build the concrete `arguments` object for a `tools/call` from the tool's input [ObjectGene].
     */
    private fun toolArguments(gene: ObjectGene): Map<String, Any?> {
        val json = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)
        return mapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
    }
}

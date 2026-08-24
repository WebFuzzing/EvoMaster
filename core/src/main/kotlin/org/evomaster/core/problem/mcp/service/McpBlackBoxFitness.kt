package org.evomaster.core.problem.mcp.service

import com.google.inject.Inject
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.mcp.McpCallResult
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.mcp.McpResourceReadAction
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.problem.mcp.client.McpResourceResult
import org.evomaster.core.problem.mcp.client.McpToolResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Blackbox fitness function for MCP servers.
 *
 * Executes each action in an [McpIndividual] sequentially via the [McpSampler]'s client and
 * scores coverage/fault targets based purely on observable protocol signals. Mirrors [GraphQLBlackBoxFitness] in structure.
 */
class McpBlackBoxFitness : McpFitness() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(McpBlackBoxFitness::class.java)

        private const val JSON_RPC_INTERNAL_ERROR = -32603
        private const val JSON_RPC_RESOURCE_NOT_FOUND = -32002
    }

    @Inject
    private lateinit var sampler: McpSampler

    override fun doCalculateCoverage(
        individual: McpIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<McpIndividual> {

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
                        val args = geneToMap(action.inputSchema)
                        val toolResult = client.callTool(action.toolName, args)

                        result.setIsError(toolResult.isError)
                        result.stopping = false

                        handleToolCallGoals(action, toolResult, fv, result, i)
                    }

                    is McpResourceReadAction -> {
                        val resourceResult = client.readResource(action.resolvedUri())
                        result.setIsError(resourceResult.protocolError != null)
                        result.stopping = false

                        handleResourceReadGoals(action, resourceResult, fv, result, i)
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
    // Tool-call goals
    // -------------------------------------------------------------------------

    private fun handleToolCallGoals(
        action: McpToolCallAction,
        toolResult: McpToolResult,
        fv: FitnessValue,
        result: McpCallResult,
        indexOfAction: Int
    ) {
        val name = action.toolName

        // Goal 1: Tool reached
        val reachedId = idMapper.handleLocalTarget("tool:$name")
        fv.updateTarget(reachedId, 1.0, indexOfAction)

        val protocolError = toolResult.protocolError
        val code = protocolError?.code?.toString() ?: "ok"

        // Goal 2: Tool success
        val successId = idMapper.handleLocalTarget("tool_success:$name")
        val errorId = idMapper.handleLocalTarget("tool_error:$name")
        when {
            protocolError == null && !toolResult.isError -> {
                fv.updateTarget(successId, 1.0, indexOfAction)
                fv.updateTarget(errorId, 0.5, indexOfAction)
            }
            protocolError == null && toolResult.isError -> {
                fv.updateTarget(successId, 0.5, indexOfAction)
                fv.updateTarget(errorId, 1.0, indexOfAction)
            }
            protocolError != null && protocolError.code == JSON_RPC_INTERNAL_ERROR -> {
                fv.updateTarget(successId, 0.5, indexOfAction)
                fv.updateTarget(errorId, 1.0, indexOfAction)
            }
            else -> {
                fv.updateTarget(successId, 0.1, indexOfAction)
                fv.updateTarget(errorId, 0.1, indexOfAction)
            }
        }

        // Goal 3: Tool outcome
        val outcomeId = idMapper.handleLocalTarget("tool_outcome:$name:$code")
        fv.updateTarget(outcomeId, 1.0, indexOfAction)

        // Goal 4: Tool output chema validation
        val outputSchema = sampler.getOutputSchema(name)
        if (outputSchema != null && protocolError == null && !toolResult.isError) {
            for ((violationKey, message) in McpSchemaValidator.findViolations(outputSchema, toolResult.structuredContent)) {
                val discriminant = "tool:$name -> $violationKey"
                val faultId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE, discriminant)
                )
                fv.updateTarget(faultId, 1.0, indexOfAction)
                result.addFault(
                    DetectedFault(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE, "tool:$name", violationKey, message)
                )
            }
        }

        // Goal 4: MCP internal error fault
        if (protocolError != null && protocolError.code == JSON_RPC_INTERNAL_ERROR) {
            val faultId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.MCP_INTERNAL_ERROR, name)
            )
            fv.updateTarget(faultId, 1.0, indexOfAction)
            result.addFault(
                DetectedFault(ExperimentalFaultCategory.MCP_INTERNAL_ERROR, "tool:$name", null, protocolError.message)
            )
        }
    }

    // -------------------------------------------------------------------------
    // Resource-read goals
    // -------------------------------------------------------------------------

    private fun handleResourceReadGoals(
        action: McpResourceReadAction,
        resourceResult: McpResourceResult,
        fv: FitnessValue,
        result: McpCallResult,
        indexOfAction: Int
    ) {
        val id = action.id

        // Goal 1: Resource reached
        val reachedId = idMapper.handleLocalTarget("resource:$id")
        fv.updateTarget(reachedId, 1.0, indexOfAction)

        val protocolError = resourceResult.protocolError
        val code = protocolError?.code?.toString() ?: "ok"

        // Goal 2: Resource found
        val foundId = idMapper.handleLocalTarget("resource_found:$id")
        val notFoundId = idMapper.handleLocalTarget("resource_not_found:$id")
        when {
            protocolError == null -> {
                fv.updateTarget(foundId, 1.0, indexOfAction)
                fv.updateTarget(notFoundId, 0.5, indexOfAction)
            }
            protocolError.code == JSON_RPC_RESOURCE_NOT_FOUND -> {
                fv.updateTarget(foundId, 0.5, indexOfAction)
                fv.updateTarget(notFoundId, 1.0, indexOfAction)
            }
            else -> {
                // fuzzer-input noise
                fv.updateTarget(foundId, 0.1, indexOfAction)
                fv.updateTarget(notFoundId, 0.1, indexOfAction)
            }
        }

        // Goal 3: Resource outcome
        val outcomeId = idMapper.handleLocalTarget("resource_outcome:$id:$code")
        fv.updateTarget(outcomeId, 1.0, indexOfAction)

        // Goal 4: Resource content
        if (protocolError == null) {
            val contentId = idMapper.handleLocalTarget("resource_content:$id")
            val score = if (resourceResult.contents.isNotEmpty()) 1.0 else 0.5
            fv.updateTarget(contentId, score, indexOfAction)
        }

        // Goal 5: Resource fault
        if (!action.isTemplate && protocolError != null && protocolError.code == JSON_RPC_RESOURCE_NOT_FOUND) {
            val faultId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.MCP_BROKEN_RESOURCE, id)
            )
            fv.updateTarget(faultId, 1.0, indexOfAction)
            result.addFault(
                DetectedFault(ExperimentalFaultCategory.MCP_BROKEN_RESOURCE, "resource:$id", null, protocolError.message)
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Convert an [ObjectGene] to a [Map] of argument values suitable for passing
     * to [McpClient.callTool]. Uses typed gene values when available, falling
     * back to printable string representation for complex types.
     */
    private fun geneToMap(gene: ObjectGene): Map<String, Any?> {
        /**
         * TODO: This will be replaced with an McpActionBuilder module
         * that will convert a tool's input schema into an [ObjectGene]
         */
        val map = mutableMapOf<String, Any?>()
        for (field in gene.fields) {
            map[field.name] = extractGeneValue(field)
        }
        return map
    }

    private fun extractGeneValue(gene: Gene): Any? {
        /**
         * TODO: The McpActionBuilder module will reuse [org.evomaster.core.problem.rest.builder.RestActionBuilderV3]
         * to handle nullable, optional and choice genes within an MCP action gene.
         */
        return when (gene) {
            is StringGene -> gene.value
            is BooleanGene -> gene.value
            is NumberGene<*> -> gene.value
            is ObjectGene -> geneToMap(gene)
            is ArrayGene<*> -> gene.getViewOfElements().map { extractGeneValue(it) }
            else -> gene.getValueAsPrintableString(listOf(), null, null, false)
        }
    }
}

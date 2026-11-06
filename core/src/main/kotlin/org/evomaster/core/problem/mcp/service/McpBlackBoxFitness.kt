package org.evomaster.core.problem.mcp.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.api.schema.JsonSchemaValidator
import org.evomaster.core.problem.mcp.McpCallResult
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.mcp.McpResourceReadAction
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.problem.mcp.client.McpResourceResult
import org.evomaster.core.problem.mcp.client.McpToolResult
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

    private val mapper = ObjectMapper()

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
                        val args = toolArguments(action.inputSchema)
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

        // Goal 4: Tool output schema validation
        handleOutputSchemaViolations(name, toolResult, sampler.getCompiledOutputSchema(name), fv, result, indexOfAction)

        // Goal 5: MCP internal error fault
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

    internal fun handleOutputSchemaViolations(
        name: String,
        toolResult: McpToolResult,
        outputSchema: JsonSchemaValidator.CompiledSchema?,
        fv: FitnessValue,
        result: McpCallResult,
        indexOfAction: Int
    ) {
        val protocolError = toolResult.protocolError
        if (config.schemaOracles
            && config.isEnabledFaultCategory(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE)
            && outputSchema != null
            && protocolError == null
            && !toolResult.isError
        ) {
            val violations = toolResult.structuredContent?.let(outputSchema::validate)
                ?: listOf(
                    JsonSchemaValidator.Violation(
                        key = "mcp:missing-structured-content",
                        keyword = "structuredContent",
                        message = "Tool declares an outputSchema but returned no structuredContent"
                    )
                )
            for (violation in violations) {
                val discriminant = "tool:$name -> ${violation.key}"
                val faultId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(DefinedFaultCategory.SCHEMA_INVALID_RESPONSE, discriminant)
                )
                fv.updateTarget(faultId, 1.0, indexOfAction)
                result.addFault(
                    DetectedFault(
                        DefinedFaultCategory.SCHEMA_INVALID_RESPONSE,
                        "tool:$name",
                        violation.key,
                        violation.message
                    )
                )
            }
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
     * Build the concrete `arguments` object for a `tools/call` from the tool's input [ObjectGene].
     */
    private fun toolArguments(gene: ObjectGene): Map<String, Any?> {
        val json = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = null)
        return mapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
    }
}

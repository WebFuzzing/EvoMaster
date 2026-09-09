package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import com.webfuzzing.arazzo.access.ArazzoAccess
import com.webfuzzing.arazzo.models.domain.Step
import com.webfuzzing.arazzo.models.domain.Workflow
import com.webfuzzing.arazzo.parser.ArazzoParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.service.Randomness
import java.util.ArrayDeque

/**
 * Arazzo Specifications: It is an OpenAPI Initiative standard designed to define complex workflows and sequences
 * of interdependent calls across one or multiple APIs.
 * Service responsible for parsing Arazzo documents and creating ArazzoWorkflow type individuals.
 */
class ArazzoWorkflowsService {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var sampler: AbstractRestSampler

    /**
     * List of Arazzo workflows. Used to create individuals.
     */
    var arazzoWorkflows = mutableListOf<Workflow>()
        private set

    /**
     * Map containing each Arazzo workflow associated with its corresponding ID.
     * Used to resolve nested workflow references in steps.
     */
    lateinit var arazzoWorkflowsById: Map<String, Workflow>
        private set

    /**
     * Load Arazzo workflows from disk.
     * This method must be invoked after the OpenAPI has been processed.
     * In the case of the RestSampler, this occurs during initialization.
     */
    fun load(openAPI: OpenAPI, location: String) {
        if (location.isBlank()) {
            throw ConfigProblemException("arazzoLocation must not be null when Arazzo strategy is enabled")
        }
        val workflows = readArazzoWorkflows(openAPI, location)
        if (workflows.isEmpty()) {
            throw ConfigProblemException("Arazzo document at '$location' must contain at least one workflow.")
        }
        arazzoWorkflows.clear()
        arazzoWorkflows.addAll(workflows)
        arazzoWorkflowsById = workflows.associateBy { it.workflowId }
    }

    private fun readArazzoWorkflows(openAPI: OpenAPI, location: String): List<Workflow> {
        return try {
            val arazzoText = ArazzoAccess.readFromDisk(location)
            ArazzoParser.parse(arazzoText, openAPI).workflows
        } catch (e: Exception) {
            throw ConfigProblemException("Failed to read or parse Arazzo document " +
                    "at '$location': ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Choose a random workflow
     */
    fun sampleAtRandom(): RestIndividual {
        val workflow = randomness.choose(arazzoWorkflows)
        return buildIndividualFromWorkflow(workflow)
    }

    /**
     * Create workflows individuals.
     * For the moment, it only recognizes a single OpenAPI.
     * Cases involving multiple APIs are currently being ignored.
     */
    fun buildIndividualFromWorkflow(workflow: Workflow): RestIndividual {
        val actions = buildArazzoRestCallActions(workflow.steps)
            .onEach {
                it.doInitialize(randomness)
                it.forceNewTaints()
            }
            .toMutableList()

        return sampler.createIndividual(SampleType.RANDOM, actions)
    }

    /**
     * A RestCallAction must be created for each Step.
     * Steps can be direct (operationId) or reference a sub-workflow
     */
    private fun buildArazzoRestCallActions(steps: List<Step>): List<RestCallAction> {
        val actions = mutableListOf<RestCallAction>()
        val pending = ArrayDeque<Step>()
        pending.addAll(steps)

        while (pending.isNotEmpty()) {
            val step = pending.removeFirst()
            when {
                !step.operationId.isNullOrBlank() ->
                    actions.add(findActionForOperation(step.operationId))

                !step.workflowId.isNullOrBlank() -> {
                    val nested = arazzoWorkflowsById[step.workflowId] ?: throw IllegalArgumentException("Arazzo: Unknown workflowId: ${step.workflowId}")
                    nested.steps.asReversed().forEach { pending.addFirst(it) }
                }

                else -> throw IllegalArgumentException("Arazzo: Step has no operationId, operationPath, or workflowId: ${step.stepId}")
            }
        }
        return actions
    }

    /**
     * Every operationId has its corresponding RestCallAction in the actionCluster
     */
    private fun findActionForOperation(operationId: String): RestCallAction {
        val template = sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()
            .find { it.operationId == operationId }
            ?: throw IllegalArgumentException("Arazzo: Unknown operationId: $operationId")
        return template.copy() as RestCallAction
    }

}
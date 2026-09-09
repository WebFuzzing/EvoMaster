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
import org.evomaster.core.search.action.Action
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
     * Load Arazzo workflows from disk
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
    fun sampleAtRandom(
        actionCluster: Map<String, Action>,
        createIndividual: (SampleType, MutableList<RestCallAction>) -> RestIndividual,
    ): RestIndividual {
        val workflow = randomness.choose(arazzoWorkflows)
        return buildIndividualFromWorkflow(workflow, actionCluster, createIndividual)
    }

    /**
     * Create workflows individuals.
     * For the moment, it only recognizes a single OpenAPI.
     * Cases involving multiple APIs are currently being ignored.
     */
    fun buildIndividualFromWorkflow(
        workflow: Workflow,
        actionCluster: Map<String, Action>,
        createIndividual: (SampleType, MutableList<RestCallAction>) -> RestIndividual,
    ): RestIndividual {
        val actions = buildArazzoRestCallActions(workflow.steps, actionCluster)
            .onEach {
                it.doInitialize(randomness)
                it.forceNewTaints()
            }
            .toMutableList()

        return createIndividual(SampleType.RANDOM, actions)
    }

    /**
     * A RestCallAction must be created for each Step.
     * Steps can be direct (operationId) or reference a sub-workflow
     */
    private fun buildArazzoRestCallActions(steps: List<Step>, actionCluster: Map<String, Action>): List<RestCallAction> {
        val actions = mutableListOf<RestCallAction>()
        val pending = ArrayDeque<Step>()
        pending.addAll(steps)

        while (pending.isNotEmpty()) {
            val step = pending.removeFirst()
            when {
                !step.operationId.isNullOrBlank() ->
                    actions.add(findActionForOperation(step.operationId, actionCluster))

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
    private fun findActionForOperation(operationId: String, actionCluster: Map<String, Action>): RestCallAction {
        val template = actionCluster.values
            .filterIsInstance<RestCallAction>()
            .find { it.operationId == operationId }
            ?: throw IllegalArgumentException("Arazzo: Unknown operationId: $operationId")
        return template.copy() as RestCallAction
    }

}
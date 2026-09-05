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

class ArazzoWorkflowsService {

    @Inject
    private lateinit var randomness: Randomness

    var arazzoWorkflows = mutableListOf<Workflow>()
        private set

    lateinit var arazzoWorkflowsById: Map<String, Workflow>
        private set

    fun load(openAPI: OpenAPI, location: String?) {
        if (location.isNullOrBlank()) {
            throw ConfigProblemException("arazzoLocation must not be null when Arazzo strategy is enabled")
        }
        val workflows = readArazzoWorkflows(openAPI, location)
        if (workflows.isEmpty()) {
            throw ConfigProblemException("There must be at least one Arazzo workflow.")
        }
        arazzoWorkflows.clear()
        arazzoWorkflows.addAll(workflows)
        arazzoWorkflowsById = workflows.associateBy { it.workflowId }
    }

    private fun readArazzoWorkflows(openAPI: OpenAPI, location: String): List<Workflow> {
        val arazzoText = ArazzoAccess.readFromDisk(location)
        return ArazzoParser.parse(arazzoText, openAPI).workflows
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
     * Create workflows individuals
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
package org.evomaster.core.problem.rest.service.sampler

import org.evomaster.arazzo.access.ArazzoAccess
import org.evomaster.arazzo.models.domain.Step
import org.evomaster.arazzo.models.domain.Workflow
import org.evomaster.arazzo.parser.ArazzoParser
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual

class ArazzoSampler : AbstractRestSampler() {

    private val workflows = mutableListOf<Workflow>()
    private lateinit var workflowsById: Map<String, Workflow>

    /**
     * Prepare workflows arazzo
     */
    override fun customizeAdHocInitialIndividuals() {
        val arazzo = readWorkflowsArazzo()
        workflows.clear()
        workflows.addAll(arazzo)
        workflowsById = arazzo.associateBy { it.workflowId }
    }

    /**
     * Parse Arazzo and looking for Workflows
     */
    private fun readWorkflowsArazzo(): List<Workflow> {
        val arazzoText = ArazzoAccess.readFromDisk(config.arazzoExampleLocation)
        return ArazzoParser.parse(arazzoText, schemaHolder.main.schemaParsed).workflows
    }

    override fun hasSpecialInitForSmartSampler(): Boolean = false

    /**
     * Choose a random workflow
     */
    override fun sampleAtRandom(): RestIndividual {
        val workflow = randomness.choose(workflows)
        return buildIndividualFromWorkflow(workflow)
    }

    /**
     * Create workflows individuals
     */
    private fun buildIndividualFromWorkflow(workflow: Workflow): RestIndividual {
        val actions = workflow.steps
            .flatMap { resolveStep(it) }
            .onEach {
                it.doInitialize(randomness)
                it.forceNewTaints()
            }
            .toMutableList()

        return createIndividual(SampleType.RANDOM, actions)
    }

    /**
     * The Steps can reference a sub-workflow or OpenApi
     */
    private fun resolveStep(step: Step): List<RestCallAction> {
        if (!step.operationId.isNullOrBlank()) {
            return listOfNotNull(findActionForOperation(step.operationId))
        }
        if (!step.workflowId.isNullOrBlank()) {
            val nested = workflowsById[step.workflowId] ?: return emptyList()
            return nested.steps.flatMap { resolveStep(it) }
        }
        return emptyList()
    }

    /**
     * Every Step Arazzo has its corresponding RestCallAction in the actionCluster
     */
    private fun findActionForOperation(operationId: String): RestCallAction? {
        val template = actionCluster.values
            .filterIsInstance<RestCallAction>()
            .find { it.operationId == operationId }
        return template?.copy() as? RestCallAction
    }
}

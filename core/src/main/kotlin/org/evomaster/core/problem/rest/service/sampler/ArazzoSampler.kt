package org.evomaster.core.problem.rest.service.sampler

import com.webfuzzing.arazzo.access.ArazzoAccess
import com.webfuzzing.arazzo.models.domain.Step
import com.webfuzzing.arazzo.models.domain.Workflow
import com.webfuzzing.arazzo.parser.ArazzoParser
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual

class ArazzoSampler : AbstractRestSampler() {

    var arazzoWorkflows = mutableListOf<Workflow>()
        private set

    lateinit var arazzoWorkflowsById: Map<String, Workflow>
        private set

    override fun postInits() {
        val location = config.arazzoLocation ?: throw ConfigProblemException("arazzoLocation must not be null when Arazzo strategy is enabled")
        val workflows = readArazzoWorkflows(location)
        arazzoWorkflows.clear()
        arazzoWorkflows.addAll(workflows)
        arazzoWorkflowsById = workflows.associateBy { it.workflowId }
    }

    private fun readArazzoWorkflows(location: String): List<Workflow> {
        val arazzoText = ArazzoAccess.readFromDisk(location)
        return ArazzoParser.parse(arazzoText, schemaHolder.main.schemaParsed).workflows
    }

    override fun customizeAdHocInitialIndividuals() {
    }

    override fun hasSpecialInitForSmartSampler(): Boolean = false

    /**
     * Choose a random workflow
     */
    override fun sampleAtRandom(): RestIndividual {
        val workflow = randomness.choose(arazzoWorkflows)
        return buildIndividualFromWorkflow(workflow)
    }

    /**
     * Create workflows individuals
     */
    fun buildIndividualFromWorkflow(workflow: Workflow): RestIndividual {
        val actions = workflow.steps
            .flatMap { buildArazzoRestCallActions(it) }
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
    private fun buildArazzoRestCallActions(step: Step): List<RestCallAction> {
        if (!step.operationId.isNullOrBlank()) {
            return listOf(findActionForOperation(step.operationId))
        }
        if (!step.workflowId.isNullOrBlank()) {
            val nested = arazzoWorkflowsById[step.workflowId] ?: throw IllegalArgumentException("Arazzo: Unknown workflowId: ${step.workflowId}")
            return nested.steps.flatMap { buildArazzoRestCallActions(it) }
        }
        throw IllegalArgumentException("Arazzo: Step has no operationId, operationPath, or workflowId: ${step.stepId}")
    }

    /**
     * Every Step Arazzo has its corresponding RestCallAction in the actionCluster
     */
    private fun findActionForOperation(operationId: String): RestCallAction {
        val template = actionCluster.values
            .filterIsInstance<RestCallAction>()
            .find { it.operationId == operationId }
            ?: throw IllegalArgumentException("Arazzo: Unknown operationId: $operationId")
        return template.copy() as RestCallAction
    }
}

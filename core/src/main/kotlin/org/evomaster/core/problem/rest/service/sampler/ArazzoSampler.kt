package org.evomaster.core.problem.rest.service.sampler

import com.webfuzzing.arazzo.models.domain.Step
import com.webfuzzing.arazzo.models.domain.Workflow
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual

class ArazzoSampler : AbstractRestSampler() {

    override fun customizeAdHocInitialIndividuals() {
        // workflows are loaded in AbstractRestSampler.initialize() when arazzoStrategy is enabled
    }

    override fun hasSpecialInitForSmartSampler(): Boolean = false

    /**
     * Choose a random workflow
     */
    override fun sampleAtRandom(): RestIndividual {
        val workflow = randomness.choose(workflowsArazzo)
        return buildIndividualFromWorkflow(workflow)
    }

    /**
     * Create workflows individuals
     */
    fun buildIndividualFromWorkflow(workflow: Workflow): RestIndividual {
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
            val nested = workflowsArazzoById[step.workflowId] ?: return emptyList()
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

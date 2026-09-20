package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import com.webfuzzing.arazzo.access.ArazzoAccess
import com.webfuzzing.arazzo.models.domain.ArazzoWorkflow
import com.webfuzzing.arazzo.models.domain.FailureAction
import com.webfuzzing.arazzo.models.domain.SuccessAction
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

    private val END = "end"

    enum class PathWay {
        SUCCESS,
        FAILURE,
        COTINUE
    }

    data class Frame(
        val workflow: ArazzoWorkflow,
        var currenStepIndex: Int,
        var pausedBranch: Boolean = false,
    ) {
        val stepIndexById: Map<String, Int> =
            workflow.steps.mapIndexedNotNull { index, step ->
                step.stepId?.let { id -> id to index }
            }.toMap()
    }

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var sampler: AbstractRestSampler

    /**
     * List of Arazzo workflows. Used to create individuals.
     */
    var arazzoArazzoWorkflows = mutableListOf<ArazzoWorkflow>()
        private set

    /**
     * Map containing each Arazzo workflow associated with its corresponding ID.
     * Used to resolve nested workflow references in steps.
     */
    lateinit var arazzoWorkflowsById: Map<String, ArazzoWorkflow>
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
        arazzoArazzoWorkflows.clear()
        arazzoArazzoWorkflows.addAll(workflows)
        arazzoWorkflowsById = workflows.associateBy { it.workflowId }
    }

    private fun readArazzoWorkflows(openAPI: OpenAPI, location: String): List<ArazzoWorkflow> {
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
        val workflow = randomness.choose(arazzoArazzoWorkflows)
        return buildIndividualFromWorkflow(workflow)
    }

    /**
     * Create workflows individuals.
     * Cases involving multiple APIs are currently being ignored.
     */
    fun buildIndividualFromWorkflow(arazzoWorkflow: ArazzoWorkflow): RestIndividual {

        if (arazzoWorkflow.steps.isEmpty()) {
            throw IllegalArgumentException("Arazzo: The Workflow ${arazzoWorkflow.workflowId} has no steps")
        }

        val frame = Frame(arazzoWorkflow, 0)

        val actions = crossWorkflow(frame)
            .onEach {
                it.doInitialize(randomness)
                it.forceNewTaints()
            }
            .toMutableList()

        return sampler.createIndividual(SampleType.RANDOM, actions)
    }

    /**
     * The workflow is traversed to locate jumps referencing other workflow and steps,
     * and to create the corresponding calls for the individual.
     * A RestCallAction must be created for each Step.
     */
    fun crossWorkflow(frame: Frame): List<RestCallAction> {
        val actions = mutableListOf<RestCallAction>()
        val stack = ArrayDeque<Frame>()
        stack.addLast(frame)

        outer@ while (stack.isNotEmpty()) {
            val currentFrame = stack.removeLast()

            while (currentFrame.currenStepIndex < currentFrame.workflow.steps.size) {
                val step = currentFrame.workflow.steps[currentFrame.currenStepIndex]

                if (!currentFrame.pausedBranch) {
                    when {
                        !step.operationId.isNullOrBlank() ->
                            actions.add(findActionForOperation(step.operationId))

                        !step.workflowId.isNullOrBlank() -> {
                            val nested = arazzoWorkflowsById[step.workflowId]
                                ?: throw IllegalArgumentException("Arazzo: Unknown workflowId: ${step.workflowId}")
                            currentFrame.pausedBranch = true
                            stack.addLast(currentFrame)
                            stack.addLast(Frame(nested, 0))
                            continue@outer
                        }

                        else -> throw IllegalArgumentException("Arazzo: Step has no operationId, operationPath, or workflowId: ${step.stepId}")
                    }
                } else {
                    currentFrame.pausedBranch = false
                }

                val pathway = choosePathway(step.onSuccess ?: emptyList(), step.onFailure ?: emptyList())

                when (pathway) {
                    PathWay.SUCCESS -> {
                        val successAction = step.onSuccess.random()
                        if (applyPathwayAction(stack, currentFrame, successAction.type, successAction.stepId, successAction.workflowId))
                            continue@outer
                        else
                            break

                    }
                    PathWay.FAILURE -> {
                        val failureAction = step.onFailure.random()
                        if (applyPathwayAction(stack, currentFrame, failureAction.type, failureAction.stepId, failureAction.workflowId))
                            continue@outer
                        else
                            break
                    }
                    else -> {
                        currentFrame.currenStepIndex++
                    }
                }
           }

       }

       return actions

    }

    /**
     * A path is randomly selected between `successActions` and `failureActions` if they exist;
     * otherwise, the process continues to the next step.
     */
    private fun choosePathway(successActions: List<SuccessAction>, failureActions: List<FailureAction>) : PathWay {
        return when {
            successActions.isEmpty() && failureActions.isEmpty() -> PathWay.COTINUE

            successActions.isEmpty() -> PathWay.FAILURE

            failureActions.isEmpty() -> PathWay.SUCCESS

            else -> if (randomness.nextBoolean())
                PathWay.SUCCESS
            else
                PathWay.FAILURE
        }
    }

    /**
     * If the workflow needs to jump to a reference, a decision is made regarding which workflow or step to jump to.
     * Steps can be direct (operationId) or reference a sub-workflow
     */
    private fun applyPathwayAction(stack : ArrayDeque<Frame>, frame: Frame, type : String, stepId : String?, workflowId: String?) : Boolean {
        if (END == type) return false

        when {
            stepId != null && workflowId != null ->
                throw IllegalArgumentException("Arazzo: stepId $stepId and $workflowId are mutually exclusive")

            workflowId != null -> {
                val newWorkflow = arazzoWorkflowsById[workflowId] ?: throw IllegalArgumentException("Arazzo: Unknown workflowId: $workflowId")
                stack.addLast(Frame(newWorkflow, 0))
                return true
            }

            stepId != null -> {
                val indexStep = frame.stepIndexById[stepId] ?: throw IllegalArgumentException("Arazzo: Unknown stepId: $stepId")
                frame.currenStepIndex = indexStep
                stack.addLast(frame)
                return true
            }

            else ->
                throw IllegalArgumentException("Arazzo: Either stepId or workflowId must be provided")
        }

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
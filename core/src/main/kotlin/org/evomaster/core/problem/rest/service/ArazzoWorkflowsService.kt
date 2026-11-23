package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import com.webfuzzing.arazzo.access.ArazzoAccess
import com.webfuzzing.arazzo.models.domain.ArazzoStep
import com.webfuzzing.arazzo.models.domain.ArazzoWorkflow
import com.webfuzzing.arazzo.models.domain.FailureAction
import com.webfuzzing.arazzo.models.domain.Parameter
import com.webfuzzing.arazzo.models.domain.SuccessAction
import com.webfuzzing.arazzo.parser.ArazzoParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.arazzo.ArazzoParameterSpec
import org.evomaster.core.problem.rest.arazzo.ArazzoStepSpecs
import org.evomaster.core.problem.rest.arazzo.FrameClose
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

    /**
     * [SuccessAction] and [FailureAction] possess the "type" attribute, which can have the value "END".
     * A constant is used to avoid having the string directly in the method
     */
    private val END = "end"

    /**
     * The steps can follow different paths to the linear flow; therefore,
     * this Enum is used to distinguish which path the workflow is traversing.
     */
    enum class PathWay {
        SUCCESS,
        FAILURE,
        CONTINUE
    }

    /**
     * Traversal state for one Arazzo workflow while [crossWorkflow] walks steps and nested workflows.
     * When [pausedBranch] is true, this frame is resuming the current step after a nested workflow completed.
     * The nested workflow must not be started again; execution continues with this step's success/failure handling.
     */
    data class Frame(
        val workflow: ArazzoWorkflow,
        var currenStepIndex: Int,
        var pausedBranch: Boolean = false,
        var workflowInputs: List<ArazzoParameterSpec> = emptyList(),
        val frameId: Int,
        val parentStep: ArazzoStep? = null,
    ) {
        var hasActions: Boolean = false

        val stepIndexById: Map<String, Int> =
            workflow.arazzoSteps.mapIndexedNotNull { index, step ->
                step.stepId?.let { id -> id to index }
            }.toMap()
    }

    /**
     * Source of unique [Frame.frameId]s.
     */
    private var frameCounter = 0

    private fun newFrame(
        workflow: ArazzoWorkflow,
        parentStep: ArazzoStep? = null,
        workflowInputs: List<ArazzoParameterSpec> = emptyList(),
    ): Frame = Frame(
        workflow,
        0,
        workflowInputs = workflowInputs,
        frameId = frameCounter++,
        parentStep = parentStep,
    )

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
     * Key: workflow ID ([ArazzoWorkflow.workflowId]) from the Arazzo file.
     * Value: the corresponding parsed [ArazzoWorkflow].
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

        if (arazzoWorkflow.arazzoSteps.isEmpty()) {
            throw IllegalArgumentException("Arazzo: The Workflow ${arazzoWorkflow.workflowId} has no steps")
        }

        val frame = newFrame(arazzoWorkflow)

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

            while (currentFrame.currenStepIndex < currentFrame.workflow.arazzoSteps.size) {
                val step = currentFrame.workflow.arazzoSteps[currentFrame.currenStepIndex]

                if (!currentFrame.pausedBranch) {
                    when {
                        !step.operationId.isNullOrBlank() -> {
                            var action = findActionForOperation(step.operationId)
                            val workflowInputs = currentFrame.workflowInputs
                            currentFrame.workflowInputs = emptyList()

                            action.arazzoStepSpecs = buildStepSpecs(step, currentFrame, workflowInputs)
                            currentFrame.hasActions = true

                            actions.add(action)
                        }

                        !step.workflowId.isNullOrBlank() -> {
                            val nested = arazzoWorkflowsById[step.workflowId]
                                ?: throw IllegalArgumentException("Arazzo: Unknown workflowId: ${step.workflowId}")
                            currentFrame.pausedBranch = true
                            stack.addLast(currentFrame)
                            stack.addLast(newFrame(nested, parentStep = step, workflowInputs = parameterSpecs(step.parameters)))
                            continue@outer
                        }

                        else -> throw IllegalArgumentException("Arazzo: Step has no operationId, operationPath, or workflowId: ${step.stepId}")
                    }
                } else {
                    currentFrame.pausedBranch = false
                }

                val onSuccess = mergeSuccessActions(currentFrame.workflow.successActions, step.onSuccess)
                val onFailure = mergeFailureActions(currentFrame.workflow.failureActions, step.onFailure)
                val pathway = choosePathway(onSuccess, onFailure)

                when (pathway) {
                    PathWay.SUCCESS -> {
                        val successAction = randomness.choose(onSuccess)
                        if (applyPathwayAction(stack, currentFrame, successAction.type, successAction.stepId, successAction.workflowId)) {
                            closeIfReplaced(actions, stack, currentFrame)
                            continue@outer
                        } else
                            break

                    }
                    PathWay.FAILURE -> {
                        val failureAction = randomness.choose(onFailure)
                        if (applyPathwayAction(stack, currentFrame, failureAction.type, failureAction.stepId, failureAction.workflowId)) {
                            closeIfReplaced(actions, stack, currentFrame)
                            continue@outer
                        } else
                            break
                    }
                    else -> {
                        currentFrame.currenStepIndex++
                    }
                }
           }

           closeFrame(actions, currentFrame, reportToParent = true)
       }

       return actions

    }

    /**
     * Closes [frame] when a success or failure jump replaces it with another workflow.
     * The new workflow keeps the parent step, so this close does not report outputs to it.
     */
    private fun closeIfReplaced(actions: List<RestCallAction>, stack: ArrayDeque<Frame>, frame: Frame) {
        if (stack.last() !== frame) {
            closeFrame(actions, frame, reportToParent = false)
        }
    }

    /**
     * Records on the last action that [frame] has finished, so the runtime can compute its outputs.
     * [reportToParent] also records the outputs of the step that invoked this workflow.
     * A frame with no actions of its own is skipped: the runtime never opened it.
     */
    private fun closeFrame(actions: List<RestCallAction>, frame: Frame, reportToParent: Boolean) {
        if (!frame.hasActions) {
            return
        }
        val last = actions.last()
        val specs = last.arazzoStepSpecs ?: return
        val parentStep = if (reportToParent) frame.parentStep else null

        val close = FrameClose(
            workflowOutputs = frame.workflow.outputs?.toMap() ?: emptyMap(),
            parentStepId = parentStep?.stepId,
            parentStepOutputs = parentStep?.outputs?.toMap() ?: emptyMap(),
        )
        last.arazzoStepSpecs = specs.copy(closes = specs.closes + close)
    }

    /**
     * Merges workflow-level [successActions] with step-level [onSuccess].
     * Step actions with the same [SuccessAction.name] override workflow actions
     */
    private fun mergeSuccessActions(workflowActions: List<SuccessAction>?, stepActions: List<SuccessAction>?): List<SuccessAction> {
        val byName = LinkedHashMap<String, SuccessAction>()
        for (action in workflowActions.orEmpty()) {
            val name = action.name ?: throw IllegalArgumentException("Arazzo: workflow successAction requires a name")
            byName[name] = action
        }
        for (action in stepActions.orEmpty()) {
            val name = action.name ?: throw IllegalArgumentException("Arazzo: step onSuccess action requires a name")
            byName[name] = action
        }
        return byName.values.toList()
    }

    /**
     * Merges workflow-level [failureActions] with step-level [onFailure].
     * Step actions with the same [FailureAction.name] override workflow actions
     */
    private fun mergeFailureActions(workflowActions: List<FailureAction>?, stepActions: List<FailureAction>?): List<FailureAction> {
        val byName = LinkedHashMap<String, FailureAction>()
        for (action in workflowActions.orEmpty()) {
            val name = action.name ?: throw IllegalArgumentException("Arazzo: workflow failureAction requires a name")
            byName[name] = action
        }
        for (action in stepActions.orEmpty()) {
            val name = action.name ?: throw IllegalArgumentException("Arazzo: step onFailure action requires a name")
            byName[name] = action
        }
        return byName.values.toList()
    }

    /**
     * A path is randomly selected between `successActions` and `failureActions` if they exist;
     * otherwise, the process continues to the next step.
     */
    private fun choosePathway(successActions: List<SuccessAction>, failureActions: List<FailureAction>) : PathWay {
        return when {
            successActions.isEmpty() && failureActions.isEmpty() -> PathWay.CONTINUE

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
                stack.addLast(newFrame(newWorkflow, parentStep = frame.parentStep))
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

    /**
     * Maps the step's parameters and outputs in the context of this workflow,
     * for use when executing or fuzzing the workflow step.
     */
    private fun buildStepSpecs(
        step: ArazzoStep,
        frame: Frame,
        workflowInputs: List<ArazzoParameterSpec> = emptyList(),
    ): ArazzoStepSpecs {
        return ArazzoStepSpecs(
            workflowId = frame.workflow.workflowId,
            frameId = frame.frameId,
            stepId = step.stepId,
            parameters = parameterSpecs(step.parameters),
            outputs = step.outputs?.toMap() ?: emptyMap(),
            workflowInputs = workflowInputs,
            requestBody = step.requestBody?.payload?.toString()
        )
    }

    private fun parameterSpecs(parameters: List<Parameter>?): List<ArazzoParameterSpec> {
        return parameters.orEmpty().map { p ->
            ArazzoParameterSpec(
                name = p.name,
                location = p.`in`,
                expression = p.value.asString(),
            )
        }
    }

}
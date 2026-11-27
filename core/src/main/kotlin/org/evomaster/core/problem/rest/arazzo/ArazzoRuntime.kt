package org.evomaster.core.problem.rest.arazzo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import java.util.ArrayDeque

/**
 * Runtime state of one Arazzo test evaluation.
 * A stack of [Frame]s keeps `$inputs`, `$outputs` and `$steps` scoped to the workflow in execution.
 */
class ArazzoRuntime private constructor() {
    companion object  {
        /**
         * Returns a runtime only if the individual comes from an Arazzo workflow, otherwise null.
         */
        operator fun invoke(mainActions: List<RestCallAction>): ArazzoRuntime? {
            return if (mainActions.isNotEmpty() && mainActions[0].arazzoStepSpecs != null) {
                ArazzoRuntime()
            } else {
                null
            }
        }
    }

    private val frames = ArrayDeque<Frame>()
    private val workflows = mutableMapOf<String, Frame>()
    private val mapper = ObjectMapper()

    /**
     * Prepare the request with the values corresponding to the step.
     */
    fun prepareRequest(action: RestCallAction) {
        val stepSpecs = action.arazzoStepSpecs ?: return
        syncFrame(stepSpecs, action)

        for (spec in stepSpecs.parameters) {
            val value = resolve(spec.expression, action, current()) ?: continue
            val param = findParam(action, spec) ?: continue
            param.primaryGene().setFromStringValue(value)
        }

        stepSpecs.requestBody?.let { applyBody(it, action) }
    }

    /**
     * Aligns the frame stack with this action's `frameId`: resume it or push a new workflow frame.
     */
    private fun syncFrame(stepSpecs: ArazzoStepSpecs, action: RestCallAction) {
        val frameId = stepSpecs.frameId

        if (frames.any { it.frameId == frameId }) {
            while (frames.last().frameId != frameId) {
                frames.removeLast()
            }
            return
        }
        val inputs = resolveInputs(stepSpecs.workflowInputs, action)
        push(Frame(frameId, stepSpecs.workflowId, inputs))
    }

    /**
     * Evaluates the caller's parameter expressions while the parent frame is still current.
     */
    private fun resolveInputs(
        workflowInputs: List<ArazzoParameterSpec>,
        action: RestCallAction,
    ): MutableMap<String, String> {
        val resolved = mutableMapOf<String, String>()
        for (spec in workflowInputs) {
            val value = resolve(spec.expression, action, current()) ?: continue
            resolved[spec.name] = value
        }
        return resolved
    }

    /**
     * Puts [frame] on the stack and records it for `$workflows.<id>` lookups.
     */
    private fun push(frame: Frame) {
        frames.addLast(frame)
        workflows[frame.workflowId] = frame
    }

    /**
     * Evaluates one runtime expression (`$inputs`, `$steps`, `$outputs`, etc.) to a string, or null.
     */
    private fun resolve(
        expression: String,
        action: RestCallAction?,
        frame: Frame,
        outputsFrame: Frame = frame,
    ): String? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
        if (!expr.startsWith("$")) {
            return expr
        }
        if (expr.contains('#')) {
            return null
        }

        val head = expr.substringBefore('.')
        val rest = expr.substringAfter('.', "")

        return when (head) {
            "\$inputs" -> readNamed(frame.inputs, rest)
            "\$outputs" -> readNamed(outputsFrame.outputs, rest)
            "\$steps" -> resolveStep(frame, rest)
            "\$workflows" -> resolveWorkflow(rest)
            "\$request" -> action?.let { resolveMessage(fieldsFromAction(it), rest) }
            else -> null
        }
    }

    /**
     * Resolves `$steps.<stepId>.outputs.<name>` on [frame].
     */
    private fun resolveStep(frame: Frame, rest: String): String? {
        val stepId = rest.substringBefore('.')
        val tail = rest.substringAfter('.', "")
        val step = frame.steps[stepId] ?: return null

        return when {
            tail.startsWith("outputs.") -> readNamed(step.outputs, tail.removePrefix("outputs."))
            else -> null
        }
    }

    /**
     * Resolves `$workflows.<workflowId>.inputs|outputs.*`.
     */
    private fun resolveWorkflow(rest: String): String? {
        val workflowId = rest.substringBefore('.')
        val tail = rest.substringAfter('.', "")
        val workflow = workflows[workflowId] ?: return null

        return when {
            tail.startsWith("inputs.") -> readNamed(workflow.inputs, tail.removePrefix("inputs."))
            tail.startsWith("outputs.") -> readNamed(workflow.outputs, tail.removePrefix("outputs."))
            else -> null
        }
    }

    /**
     * Reads a `$request.header|query|path.<name>` value. Body is not supported yet.
     */
    private fun resolveMessage(message: HttpFields?, rest: String): String? {
        if (message == null || rest.isEmpty()) {
            return null
        }
        return when {
            rest.startsWith("header.") -> message.headers[rest.removePrefix("header.")]
            rest.startsWith("query.") -> message.query[rest.removePrefix("query.")]
            rest.startsWith("path.") -> message.path[rest.removePrefix("path.")]
            else -> null
        }
    }

    /**
     * Returns the named value. Nested names and JSON Pointers are rejected.
     */
    private fun readNamed(source: Map<String, String>, name: String): String? {
        if (name.isEmpty() || name.contains('.') || name.contains('#')) {
            return null
        }
        return source[name]
    }

    /**
     * Reads the current action's genes into header, query and path maps for `$request.*`.
     */
    private fun fieldsFromAction(action: RestCallAction): HttpFields {
        val fields = HttpFields()
        for (param in action.parameters) {
            val value = try {
                param.primaryGene().getValueAsRawString()
            } catch (_: Exception) {
                continue
            }
            when (param) {
                is HeaderParam -> fields.headers[param.name] = value
                is QueryParam -> fields.query[param.name] = value
                is PathParam -> fields.path[param.name] = value
            }
        }
        return fields
    }

    /**
     * Finds the single action parameter with the same name and location as [spec].
     */
    private fun findParam(action: RestCallAction, spec: ArazzoParameterSpec): Param? {
        val location = spec.location ?: return null
        val matches = action.parameters.filter { param ->
            param.name == spec.name && matchesLocation(param, location)
        }
        return matches.singleOrNull()
    }

    /**
     * Matches an Arazzo `in` value (`path`, `query`, `header`) to the parameter type.
     */
    private fun matchesLocation(param: Param, location: String): Boolean {
        return when (location) {
            "path" -> param is PathParam
            "query" -> param is QueryParam
            "header" -> param is HeaderParam
            else -> false
        }
    }

    /**
     * Fills the action's body gene with the step's `requestBody` payload, once its expressions are evaluated.
     */
    private fun applyBody(payload: String, action: RestCallAction) {
        val body = action.parameters.filterIsInstance<BodyParam>().singleOrNull() ?: return
        val resolved = resolvePayload(mapper.readTree(payload), action) ?: return
        body.primaryGene().setFromStringValue(resolved.toString())
    }

    /**
     * Copies the JSON replacing each text starting with `$` by its value.
     * A field whose expression has no value is left out, so its gene keeps the generated value.
     */
    private fun resolvePayload(node: JsonNode, action: RestCallAction): JsonNode? {
        return when {
            node.isObject -> mapper.createObjectNode().also { copy ->
                for ((name, child) in node.fields()) {
                    resolvePayload(child, action)?.let { copy.set<JsonNode>(name, it) }
                }
            }
            node.isArray -> mapper.createArrayNode().also { copy ->
                for (child in node) {
                    resolvePayload(child, action)?.let { copy.add(it) }
                }
            }
            node.isTextual && node.asText().startsWith("$") ->
                resolve(node.asText(), action, current())?.let { TextNode(it) }
            else -> node
        }
    }

    /**
     * After the action is executed, evaluates the step's `outputs` against the response
     * and stores them for later steps (`$steps.<stepId>.outputs.<name>`).
     * Then closes the workflows that end right after this action.
     */
    fun recordResponse(action: RestCallAction, result: RestCallResult) {
        val stepSpecs = action.arazzoStepSpecs ?: return

        val outputs = mutableMapOf<String, String>()
        for ((name, expression) in stepSpecs.outputs) {
            val value = resolveResponseOutput(expression, result) ?: continue
            outputs[name] = value
        }

        current().steps[stepSpecs.stepId] = StepMemory(outputs)

        for (close in stepSpecs.closes) {
            closeFrame(close)
        }
    }

    /**
     * Pops the top workflow frame after filling its outputs and, if needed, the parent step's outputs.
     */
    private fun closeFrame(close: FrameClose) {
        val child = frames.peekLast() ?: return

        for ((name, expression) in close.workflowOutputs) {
            val value = resolve(expression, null, child) ?: continue
            child.outputs[name] = value
        }
        frames.removeLast()

        val parentStepId = close.parentStepId ?: return
        val parent = frames.peekLast() ?: return

        val stepOutputs = mutableMapOf<String, String>()
        for ((name, expression) in close.parentStepOutputs) {
            val value = resolve(expression, null, parent, outputsFrame = child) ?: continue
            stepOutputs[name] = value
        }
        parent.steps[parentStepId] = StepMemory(stepOutputs)
    }

    /**
     * Evaluates an output expression against the response. Supported:
     * - `$statusCode`
     * - `$response.header.<name>`
     * - `$response.body`
     * - `$response.body#/json/pointer`
     */
    private fun resolveResponseOutput(expression: String, result: RestCallResult): String? {
        val expr = expression.trim()

        if (expr == "\$statusCode") {
            return result.getStatusCode()?.toString()
        }

        if (expr.startsWith("\$response.header.")) {
            return result.getHeader(expr.removePrefix("\$response.header."))
        }

        if (expr == "\$response.body") {
            return result.getBody()
        }

        if (expr.startsWith("\$response.body#")) {
            val body = result.getBody() ?: return null
            val pointer = expr.substringAfter('#')
            val node = try {
                mapper.readTree(body).at(pointer)
            } catch (_: Exception) {
                return null
            }
            return when {
                node.isMissingNode || node.isNull -> null
                node.isValueNode -> node.asText()
                else -> node.toString()
            }
        }

        return null
    }

    /**
     * Workflow frame currently in execution.
     */
    private fun current(): Frame = frames.last()

    /**
     * One invocation of a workflow: its inputs, outputs and executed steps.
     */
    private class Frame(
        val frameId: Int,
        val workflowId: String,
        val inputs: MutableMap<String, String> = mutableMapOf(),
        val outputs: MutableMap<String, String> = mutableMapOf(),
        val steps: MutableMap<String, StepMemory> = mutableMapOf(),
    )

    /**
     * Values produced by one executed step: its named outputs.
     */
    private class StepMemory(
        val outputs: MutableMap<String, String> = mutableMapOf(),
    )

    /**
     * Header, query and path values of the request, used to resolve `$request.*`.
     */
    private class HttpFields(
        val headers: MutableMap<String, String> = mutableMapOf(),
        val query: MutableMap<String, String> = mutableMapOf(),
        val path: MutableMap<String, String> = mutableMapOf(),
    )
}

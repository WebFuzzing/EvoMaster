package org.evomaster.core.problem.rest.arazzo

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import java.util.ArrayDeque

/**
 * Runtime state of one Arazzo test evaluation.
 * A stack of [Frame]s keeps `$inputs`, `$outputs` and `$steps` scoped to the workflow in execution.
 */
class ArazzoRuntime(
    private val inputs: Map<String, String> = emptyMap(),
) {

    private val frames = ArrayDeque<Frame>()
    private val workflows = mutableMapOf<String, Frame>()

    /**
     * Prepare the request with the values corresponding to the step.
     */
    fun prepareRequest(action: RestCallAction) {
        val stepSpecs = action.arazzoStepSpecs ?: return
        syncFrame(stepSpecs.workflowId, stepSpecs.workflowInputs, action)

        for (spec in stepSpecs.parameters) {
            val value = resolve(spec.expression, action) ?: continue
            val param = findParam(action, spec) ?: continue
            param.primaryGene().setFromStringValue(value)
        }
    }

    /**
     * `$steps`, `$inputs` and `$outputs` refer to the workflow currently on the stack.
     * A nested workflow gets its own frame, so the same stepId does not collide.
     * Returning to a workflowId already on the stack closes the frames opened after it.
     */
    private fun syncFrame(
        workflowId: String,
        workflowInputs: List<ArazzoParameterSpec>,
        action: RestCallAction,
    ) {
        if (frames.isEmpty()) {
            push(Frame(workflowId, inputs.toMutableMap()))
            return
        }
        if (frames.last().workflowId == workflowId) {
            return
        }
        val parent = frames.indexOfLast { it.workflowId == workflowId }
        if (parent >= 0) {
            while (frames.size - 1 > parent) {
                frames.removeLast()
            }
            return
        }
        push(Frame(workflowId, resolveInputs(workflowInputs, action)))
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
            val value = resolve(spec.expression, action) ?: continue
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
     * Retrieves the value corresponding to the parameter expression
     */
    private fun resolve(expression: String, action: RestCallAction): String? {
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
            "\$inputs" -> readNamed(current().inputs, rest)
            "\$outputs" -> readNamed(current().outputs, rest)
            "\$steps" -> resolveStep(rest)
            "\$workflows" -> resolveWorkflow(rest)
            "\$request" -> resolveMessage(fieldsFromAction(action), rest)
            "\$sourceDescriptions", "\$url", "\$method","\$response", "\$statusCode" -> null
            else -> null
        }
    }

    /**
     * Resolves `$steps.<stepId>.outputs|request|response.*` on the current frame.
     */
    private fun resolveStep(rest: String): String? {
        val stepId = rest.substringBefore('.')
        val tail = rest.substringAfter('.', "")
        val step = current().steps[stepId] ?: return null

        return when {
            tail.startsWith("outputs.") -> readNamed(step.outputs, tail.removePrefix("outputs."))
            tail.startsWith("request.") -> resolveMessage(step.request, tail.removePrefix("request."))
            tail.startsWith("response.") -> resolveMessage(step.response, tail.removePrefix("response."))
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
     * Reads a header, query or path value. Body is not supported yet.
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
     * Workflow frame currently in execution.
     */
    private fun current(): Frame = frames.last()

    /**
     * One invocation of a workflow: its inputs, outputs and executed steps.
     */
    private class Frame(
        val workflowId: String,
        val inputs: MutableMap<String, String> = mutableMapOf(),
        val outputs: MutableMap<String, String> = mutableMapOf(),
        val steps: MutableMap<String, StepMemory> = mutableMapOf(),
    )

    /**
     * Values produced by one step in this run: named outputs, request and response fields.
     */
    private class StepMemory(
        val outputs: MutableMap<String, String> = mutableMapOf(),
        val request: HttpFields = HttpFields(),
        val response: HttpFields = HttpFields(),
    )

    /**
     * Header, query and path values of a request or a response.
     */
    private class HttpFields(
        val headers: MutableMap<String, String> = mutableMapOf(),
        val query: MutableMap<String, String> = mutableMapOf(),
        val path: MutableMap<String, String> = mutableMapOf(),
    )
}

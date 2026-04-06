package org.evomaster.core.problem.rest.arazzo.parser

import org.evomaster.core.problem.rest.arazzo.models.Components
import org.evomaster.core.problem.rest.arazzo.models.Criterion
import org.evomaster.core.problem.rest.arazzo.models.CriterionExpression
import org.evomaster.core.problem.rest.arazzo.models.CriterionType
import org.evomaster.core.problem.rest.arazzo.models.FailureAction
import org.evomaster.core.problem.rest.arazzo.models.Parameter
import org.evomaster.core.problem.rest.arazzo.models.SourceDescription
import org.evomaster.core.problem.rest.arazzo.models.Step
import org.evomaster.core.problem.rest.arazzo.models.SuccessAction
import org.evomaster.core.problem.rest.arazzo.models.Workflow
import wiremock.com.jayway.jsonpath.JsonPath
import javax.xml.xpath.XPathFactory

object ArazzoValidator {

    private val possibleParameters = listOf("path","query","header","cookie")
    private val possibleTypeSuccessActions = listOf("end","goto")
    private val possibleTypeFailureActions = listOf("end","goto","retry")
    private val possibleCriterionExpresionTypes = listOf("jsonpath","xpath")
    private val possibleCriterionVersionXpath = listOf("xpath-30","xpath-20","xpath-10")

    private const val POSSIBLE_CRITERION_VERSION_JSON = "draft-goessner-dispatch-jsonpath-00"

    fun validateSourceDescriptions(sourceDescription: SourceDescription) {
        val patternName = Regex("[A-Za-z0-9_\\-]+")

        if (!sourceDescription.name.matches(patternName)) {
            throw IllegalArgumentException("Arazzo Parsing Error: The name should conform to the regular expression [A-Za-z0-9_\\-]+.")
        }
    }

    fun validateWorkflows(workflows: List<Workflow>) {
        if (workflows.size != workflows.distinctBy { it.workflowId }.size) {
            throw IllegalArgumentException("Arazzo Parsing Error: The id MUST be unique amongst all workflows described in the Arazzo Description. ")
        }
        workflows.forEach { workflow -> validateWorkflow(workflow) }
        validateDependsOnWorkflows(workflows)
    }

    private fun validateWorkflow(workflow: Workflow) {
        val patternName = Regex("[A-Za-z0-9_\\-]+")

        // workflowId
        if (!workflow.workflowId.matches(patternName)) {
            throw IllegalArgumentException("Arazzo Parsing Error: The name should conform to the regular expression [A-Za-z0-9_\\-]+.")
        }
        //TODO: $sourceDescriptions.<name>.<workflowId>) Leer en la docu

        // Steps
        if (workflow.steps.isEmpty()) {
            throw IllegalArgumentException("Arazzo Parsing Error: The steps must have at least one step.")
        }

        workflow.steps.forEach { step -> validateStep(step) }

        // successActions

        // failureActions

        // outputs
        val keyOutputRegex = Regex("^[a-zA-Z0-9\\.\\-_]+$")
        workflow.outputs?.keys?.forEach { key ->
            if (!keyOutputRegex.matches(key)) {
                throw IllegalArgumentException(
                    "Arazzo Parsing Error: Output name in workflow: ${workflow.workflowId}: '$key' is invalid. " +
                            "Only alphanumeric characters, periods (.), hyphens (-) and underscores (_) are allowed."
                )
            }
        }

        // parameters

    }

    private fun validateStep(step: Step) {

    }

    private fun validateDependsOnWorkflows(workflows: List<Workflow>) {
        val workflowsMap = workflows.associateBy { it.workflowId }

        // Validate existence
        workflows.forEach { workflow ->
            workflow.dependsOn?.forEach { dependencyId ->
                if (!workflowsMap.containsKey(dependencyId)) {
                    throw IllegalArgumentException(
                        "Arazzo Parsing Error: The workflow '${workflow.workflowId}' depends on '$dependencyId', " +
                                "but that workflowId does not exist in the document."
                    )
                }
            }
        }

        // DFS for dependencies
        // The visit states map helps track our progress:
        // 0 or null = Unvisited
        // 1 = Visiting (currently in the recursion stack)
        // 2 = Fully Validated (safe, no cycles detected from here)
        val visitStates = mutableMapOf<String, Int>()

        fun detectCycleDFS(currentWorkflowId: String, currentPath: List<String>) {
            val state = visitStates[currentWorkflowId] ?: 0

            if (state == 1) {
                val cyclePath = currentPath.joinToString(" -> ") + " -> $currentWorkflowId"
                throw IllegalArgumentException("Arazzo Parsing Error: Cyclic dependency detected. Workflows cannot depend on themselves in a closed loop.")
            }

            if (state == 2) {
                return
            }

            visitStates[currentWorkflowId] = 1

            val dependencies = workflowsMap[currentWorkflowId]?.dependsOn ?: emptyList()
            dependencies.forEach { dependencyId ->
                detectCycleDFS(dependencyId, currentPath + currentWorkflowId)
            }

            visitStates[currentWorkflowId] = 2
        }

        // Execute the validator for all workflows
        workflowsMap.keys.forEach { workflowId ->
            if ((visitStates[workflowId] ?: 0) == 0) {
                detectCycleDFS(workflowId, emptyList())
            }
        }
    }

    fun validateComponents(components: Components?) {
        if  (components == null) return

        val componentKeyRegex = Regex("^[a-zA-Z0-9\\.\\-_]+$")
        fun validateMapKeys(map: Map<String, Any>?, componentType: String) {
            map?.keys?.forEach { key ->
                if (!componentKeyRegex.matches(key)) {
                    throw IllegalArgumentException(
                        "Arazzo Parsing Error: Component name:'$key' in '$componentType' is invalid. " +
                                "Only alphanumeric characters, periods (.), hyphens (-) and underscores (_) are allowed."
                    )
                }
            }
        }

        validateMapKeys(components.inputs, "inputs")
        validateMapKeys(components.parameters, "parameters")
        validateMapKeys(components.successActions, "successActions")
        validateMapKeys(components.failureActions, "failureActions")

        //TODO: Validar Json Schema. inputs
        components.parameters?.let { paremeters -> validateParameters(paremeters) }
        components.successActions?.forEach { successAction -> validateSuccessAction(successAction.value) }
        components.failureActions?.forEach { failureActions -> validateFailureAction(failureActions.value) }
    }

    private fun validateParameters(parameters: Map<String, Parameter>?) {
        if (parameters == null) return;

        //A unique parameter is defined by the combination of a name and in fields
        val duplicates = parameters.values.groupBy { Pair(it.name, it.location) }
            .filter { (_, list) -> list.size > 1 }

        if (duplicates.isNotEmpty()) {
            val errorMessages = duplicates.map { (key, list) ->
                "The Parameter [name='${key.first}', in='${key.second}'] is repeated ${list.size} times."
            }
            throw IllegalArgumentException("Arazzo Parsing Error: Parameters repeated. " + errorMessages.joinToString("\n"))
        }

        parameters.values.forEach { parameter ->
            if (parameter.location != null && parameter.location !in possibleParameters) {
                throw IllegalArgumentException("Arazzo Parsing Error: Parameters must be one of the list: \"path\",\"query\",\"header\",\"cookie\"")
            }
        }
    }

    private fun validateParameters(parameters: List<Parameter>) {
        //A unique parameter is defined by the combination of a name and in fields
        val duplicates = parameters.groupBy { Pair(it.name, it.location) }
            .filter { (_, list) -> list.size > 1 }

        if (duplicates.isNotEmpty()) {
            val errorMessages = duplicates.map { (key, list) ->
                "The Parameter [name='${key.first}', in='${key.second}'] is repeated ${list.size} times."
            }
            throw IllegalArgumentException("Arazzo Parsing Error: Parameters repeated. " + errorMessages.joinToString("\n"))
        }

        //Only Parameters in workflow context, option "in" is mandatory
        parameters.forEach { parameter ->
            if (parameter.location == null || parameter.location !in possibleParameters) {
                throw IllegalArgumentException("Arazzo Parsing Error: Parameters must be one of the list: \"path\",\"query\",\"header\",\"cookie\"")
            }
        }
    }

    private fun validateSuccessAction(successAction: SuccessAction) {
        if (successAction.type !in possibleTypeSuccessActions) {
            throw IllegalArgumentException("Arazzo Parsing Error: successAction.type must be one of the list: \"end\",\"goto\"")
        }

        if (successAction.type == "goto") {
            if (!((successAction.workflowId != null) xor (successAction.stepId != null))) {
                throw IllegalArgumentException("Arazzo Parsing Error: SuccesAction: workflowId and stepId are mutually exclusive.")
            }
            //TODO Añadir la referencia del workflow y stepId
        }

        successAction.criteria?.forEach { criterion -> validateCriterion(criterion) }

    }

    private fun validateFailureAction(failureAction: FailureAction) {
        if (failureAction.type !in possibleTypeFailureActions) {
            throw IllegalArgumentException("Arazzo Parsing Error: failureAction.type must be one of the list: \"end\",\"goto\",\"retry\"")
        }

        if (failureAction.type == "goto" || failureAction.type == "retry") {
            if (!((failureAction.workflowId != null) xor (failureAction.stepId != null))) {
                throw IllegalArgumentException("Arazzo Parsing Error: FailureAction: workflowId and stepId are mutually exclusive.")
            }
            //TODO Añadir la referencia del workflow y stepId
        }

        if (failureAction.type == "retry") {
            if (failureAction.retryAfter != null && failureAction.retryAfter.toDouble() <= 0) {
                throw IllegalArgumentException("Arazzo Parsing Error: FailureAction: retryAfter must be non-negative decimal number.")
            }

            if (failureAction.retryLimit != null && failureAction.retryLimit <= 0) {
                throw IllegalArgumentException("Arazzo Parsing Error: FailureAction: retryLimit must be non-negative integer number.")
            }
        }

        failureAction.criteria?.forEach { criterion -> validateCriterion(criterion) }
    }

    private fun validateCriterion(criterion: Criterion?) {
        if (criterion == null) return

        if (criterion.type != null && criterion.context == null) {
            throw IllegalArgumentException("Arazzo Parsing Error: Criterion Object. If \"type\" is specified, then the context MUST be provided.")
        }

        val type: String?
        when(val criterionType = criterion.type) {
            is CriterionType.Simple -> type = criterionType.value
            is CriterionType.Complex -> {
                validateCriterionExpresion(criterionType.expr)
                type = criterionType.expr.type
            }
            null -> type = "simple"
        }

        try {
            when (type.lowercase()) {
                "regex" -> Regex(criterion.condition)
                "jsonpath" -> JsonPath.compile(criterion.condition)
                "simple" -> SimpleConditionParser(criterion.condition).validateOrThrow()
                "xpath" -> {
                    val xpathFactory = XPathFactory.newInstance()
                    val xpath = xpathFactory.newXPath()
                    xpath.compile(criterion.condition)
                }
                else -> throw IllegalArgumentException("Invalid Criteron Type")
            }
        } catch(e: Exception) {
            throw IllegalArgumentException("Arazzo Parsing Error: Criterion condition error - ${criterion.condition}: ${e.message}")
        }
    }

    private fun validateCriterionExpresion(criterionExpresion: CriterionExpression) {
        if (criterionExpresion.type !in possibleCriterionExpresionTypes) {
            throw IllegalArgumentException("Arazzo Parsing Error: Criterion Expresion type invalid. The options allowed are jsonpath or xpath.")
        }

        when(criterionExpresion.type) {
            "jsonpath" -> {
                if (!criterionExpresion.version.equals(POSSIBLE_CRITERION_VERSION_JSON)) {
                    throw IllegalArgumentException("Arazzo Parsing Error: The allowed values for JSONPath are draft-goessner-dispatch-jsonpath-00")
                }
            }
            "xpath" -> {
                if (criterionExpresion.version !in possibleCriterionVersionXpath) {
                    throw IllegalArgumentException("Arazzo Parsing Error: The allowed values for XPath are xpath-30, xpath-20, or xpath-10.")
                }
            }
        }
    }

}
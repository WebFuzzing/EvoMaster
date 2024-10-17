package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.sql.SqlAction

abstract class ActionTestCaseNamingStrategy(
    solution: Solution<*>,
    private val languageConventionFormatter: LanguageConventionFormatter,
    private val config: EMConfig,
) : NumberedTestCaseNamingStrategy(solution)  {

    protected val on = "on"
    protected val throws = "throws"
    protected val returns = "returns"
    protected val error = "error"
    protected val success = "success"
    protected val data = "data"
    protected val empty = "empty"
    protected val using = "using"
    protected val sql = "sql"
    protected val mongo = "mongo"
    protected val wiremock = "wiremock"

    protected fun formatName(nameTokens: List<String>): String {
        return "_${languageConventionFormatter.formatName(nameTokens)}"
    }

    protected fun getPath(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return "root"
        }
        return TestWriterUtils.safeVariableName(nameQualifier)
    }

    private fun fault(faults: Set<FaultCategory>): String {
        if (faults.size > 1) {
            val faultCodes = StringBuilder("showsFaults")
            /*
              For better readability, multiple faults will be concatenated in a string separated by underscore
              to help understand it is a list of codes. Regardless of the outputFormat and language conventions.
             */
            faults.sortedBy { it.code }.forEach { fault -> faultCodes.append("_${fault.code}") }
            return faultCodes.toString()
        }
        return faults.first().testCaseLabel
    }

    protected fun addResult(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>) {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        if (detectedFaults.isNotEmpty()) {
            nameTokens.add(fault(detectedFaults))
        } else {
            addActionResult(individual.evaluatedMainActions().last(), nameTokens)
        }
        addEnvironmentActions(individual, nameTokens)
    }

    private fun addEnvironmentActions(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>) {
        val initializingActions = individual.individual.seeInitializingActions()
        val allActions = individual.individual.seeAllActions()

        val initActionNames = mutableListOf<String>()
        if (hasSqlAction(initializingActions)) initActionNames.add(sql)
        if (hasMongoAction(initializingActions)) initActionNames.add(mongo)
        if (usesWireMock(allActions)) initActionNames.add(wiremock)

        if (initActionNames.isNotEmpty()) {
            nameTokens.add(using)
            nameTokens.addAll(initActionNames)
        }
    }

    private fun hasSqlAction(actions: List<EnvironmentAction>): Boolean {
        return actions.any { it is SqlAction }
    }

    private fun hasMongoAction(actions: List<EnvironmentAction>): Boolean {
        return actions.any { it is MongoDbAction }
    }

    private fun usesWireMock(actions: List<Action>): Boolean {
        return config.isEnabledExternalServiceMocking() && actions.any { it is HttpExternalServiceAction }
    }

    protected abstract fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>)

}

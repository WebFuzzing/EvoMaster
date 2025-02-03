package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.mongo.MongoDbAction
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
    protected val maxTestCaseNameLength: Int
) : NumberedTestCaseNamingStrategy(solution)  {

    private val testCasesSize = solution.individuals.size

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
    protected val wiremock = "wireMock"

    protected fun formatName(nameTokens: List<String>): String {
        return if (nameTokens.isNotEmpty()) "_${languageConventionFormatter.formatName(nameTokens)}" else ""
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

    protected fun addResult(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, remainingNameChars: Int) {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        val newRemainingNameChars = if (detectedFaults.isNotEmpty()) {
            addNameTokenIfAllowed(nameTokens, fault(detectedFaults), remainingNameChars)
        } else {
            addActionResult(individual.evaluatedMainActions().last(), nameTokens, remainingNameChars)
        }
        addEnvironmentActions(individual, nameTokens, newRemainingNameChars)
    }

    protected fun namePrefixChars(): Int {
        return "test_".length + testCasesSize + 1
    }

    protected fun addNameTokensIfAllowed(nameTokens: MutableList<String>, targetStrings: List<String>, remainingNameChars: Int): Int {
        val charsToBeUsed = targetStrings.sumOf { it.length }
        if ((remainingNameChars - charsToBeUsed) >= 0) {
            nameTokens.addAll(targetStrings)
            return remainingNameChars - charsToBeUsed
        }
        return remainingNameChars
    }

    protected fun addNameTokenIfAllowed(nameTokens: MutableList<String>, targetString: String, remainingNameChars: Int): Int {
        if (canAddNameTokens(targetString, remainingNameChars)) {
            nameTokens.add(targetString)
            return remainingNameChars - targetString.length
        }
        return remainingNameChars
    }

    private fun canAddNameTokens(targetString: String, remainingNameChars: Int): Boolean {
        return (remainingNameChars - targetString.length) >= 0
    }

    private fun addEnvironmentActions(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, remainingNameChars: Int) {
        val initializingActions = individual.individual.seeInitializingActions()
        val allActions = individual.individual.seeAllActions()

        val initActionNames = mutableListOf<String>()
        if (hasSqlAction(initializingActions)) initActionNames.add(sql)
        if (hasMongoAction(initializingActions)) initActionNames.add(mongo)
        if (usesWireMock(allActions)) initActionNames.add(wiremock)

        if (initActionNames.isNotEmpty()) {
            initActionNames.add(0, using)
            addNameTokensIfAllowed(nameTokens, initActionNames, remainingNameChars)
        }
    }

    private fun hasSqlAction(actions: List<EnvironmentAction>): Boolean {
        return actions.any { it is SqlAction }
    }

    private fun hasMongoAction(actions: List<EnvironmentAction>): Boolean {
        return actions.any { it is MongoDbAction }
    }

    private fun usesWireMock(actions: List<Action>): Boolean {
        return actions.any { it is HttpExternalServiceAction }
    }

    protected abstract fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>, remainingNameChars: Int): Int

}

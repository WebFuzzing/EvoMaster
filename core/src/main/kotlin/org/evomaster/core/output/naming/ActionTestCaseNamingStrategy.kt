package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
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

    private var shouldAddSqlSuffix = true
    private var shouldAddMongoSuffix = true
    private var shouldAddWireMockSuffix = true

    /**
     * We do not add the UsingMongo/Sql/WireMock suffixes if:
     * - All tests in the suite share the same environment action.
     * - In a suite of more than 10 test cases, more than half of them share the same action.
     *
     * The number 10 as a boundary was chosen thinking of what a "short" test suite size.
     * Therefore, for short test suites the suffix might differentiate between
     * different test cases, or at least not result in a great repetition of suffixes.
     *
     * Lower boundary is 2 tests, since the goal of test naming is mainly to add information.
     */
    init {
        val individuals = solution.individuals
        if (testCasesSize > 2) {
            shouldAddSqlSuffix = shouldAddEnvironmentAction(::hasSqlAction, individuals)
            shouldAddMongoSuffix = shouldAddEnvironmentAction(::hasMongoAction, individuals)
            shouldAddWireMockSuffix = shouldAddWireMock(individuals)
        }
    }

    private fun shouldAddEnvironmentAction(environmentFunction: (List<EnvironmentAction>) -> Boolean, individuals: MutableList<out EvaluatedIndividual<out Individual>>): Boolean {
        val indsWithEnvironmentAction = individuals.count { environmentFunction(it.individual.seeInitializingActions()) }
        return indsWithEnvironmentAction != testCasesSize && (testCasesSize < 10 || indsWithEnvironmentAction < (testCasesSize/2))
    }

    private fun shouldAddWireMock(individuals: MutableList<out EvaluatedIndividual<out Individual>>): Boolean {
        val indsWithWireMock = individuals.count { usesWireMock(it.individual.seeAllActions()) }
        return indsWithWireMock != testCasesSize && (testCasesSize < 10 || indsWithWireMock < (testCasesSize/2))
    }

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
        val digitsUsedForTestNumbering = testCasesSize.toString().length
        return "test_".length + digitsUsedForTestNumbering + 1
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
        if (shouldAddSqlSuffix && hasSqlAction(initializingActions)) initActionNames.add(sql)
        if (shouldAddMongoSuffix && hasMongoAction(initializingActions)) initActionNames.add(mongo)
        if (shouldAddWireMockSuffix && usesWireMock(allActions)) initActionNames.add(wiremock)

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

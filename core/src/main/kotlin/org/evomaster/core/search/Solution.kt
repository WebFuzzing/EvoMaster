package org.evomaster.core.search

import org.evomaster.core.sql.SqlAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.search.action.ActionFilter


class Solution<T>(
    val individuals: MutableList<EvaluatedIndividual<T>>,
    val testSuiteNamePrefix: String,
    val testSuiteNameSuffix: String,
    val termination: Termination = Termination.NONE,
    val individualsDuringSeeding: List<EvaluatedIndividual<T>>,
    val targetsDuringSeeding: List<Int>
)
where T : Individual {

    val overall: FitnessValue = FitnessValue(0.0)

    @Deprecated("No longer used, but might be reintroduced with different form in future")
    var clusteringTime = 0
    var statistics = mutableListOf<Any>()

    init{
        individuals.forEach {
            overall.merge(it.fitness)
            overall.size += it.individual.size()
        }
        overall.setTargetsCoveredBySeeding(targetsDuringSeeding)
    }

    fun getFileRelativePath(format: OutputFormat): String{
        return getFileName().getAsPath(format)
    }

    fun getFileName() : TestSuiteFileName {

        val baseName = testSuiteNamePrefix + termination.suffix
        val name = if(testSuiteNameSuffix.isBlank()){
            baseName
        } else if(testSuiteNameSuffix.startsWith("_")){
            "$baseName$testSuiteNameSuffix"
        } else {
            "${baseName}_$testSuiteNameSuffix"
        }
        return TestSuiteFileName(name)
    }

    fun needWireMockServers() : Boolean{
        return hasAnyHostnameResolutionAction()
        // The following was wrong... could have SUT connecting to WM without any mocked response,
        // which would result in no action in the tests
//        return individuals.any { ind ->
//            ind.individual.seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE).isNotEmpty()
//        }
    }

    private fun hasAnyHostnameResolutionAction(): Boolean {
        return individuals.any { ind -> ind.individual.seeAllActions().any() { a -> a is HostnameResolutionAction } }
    }

    fun needsHostnameReplacement(): Boolean {
        return hasAnyHostnameResolutionAction()
    }

    fun hasAnySqlAction() : Boolean{
        return individuals.any { ind -> ind.individual.seeAllActions().any { a ->  a is SqlAction}}
    }

    fun hasAnyMongoAction() : Boolean{
        return individuals.any { ind -> ind.individual.seeAllActions().any { a ->  a is MongoDbAction}}
    }

    /**
     * extract individual generated during seeding as a solution
     */
    fun extractSolutionDuringSeeding() : Solution<T>{
        return Solution(individualsDuringSeeding.toMutableList(), testSuiteNamePrefix, testSuiteNameSuffix, Termination.SEEDING, listOf(), listOf())
    }

    fun distinctDetectedFaultTypes(): Set<Int> {
        return DetectedFaultUtils.getDetectedFaultCategories(this)
            .map { it.code }
            .toSet()
    }

    fun totalNumberOfDetectedFaults() : Int {
        return DetectedFaultUtils.getDetectedFaults(this).size
    }

    fun detectedFaultsSummary() : String {
        return DetectedFaultUtils.getDetectedFaults(this)
            .groupBy { it.category.code }
            .entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}:${it.value.size}" }
    }
}

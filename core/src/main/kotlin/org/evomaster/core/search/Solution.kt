package org.evomaster.core.search

import org.evomaster.core.sql.SqlAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction


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
    var clusteringTime = 0
    var statistics = mutableListOf<Any>()

    init{
        individuals.forEach {
            overall.merge(it.fitness)
            overall.size += it.individual.size()
        }
        overall.setTargetsCoveredBySeeding(targetsDuringSeeding)
    }

    fun getFileName() : String{

        val name = testSuiteNamePrefix + termination.suffix
        if(testSuiteNameSuffix.isBlank()){
            return name
        }
        if(testSuiteNameSuffix.startsWith("_")){
            return "$name$testSuiteNameSuffix"
        }
        return  "${name}_$testSuiteNameSuffix"
    }

    fun hasAnyActiveHttpExternalServiceAction() : Boolean{
        return individuals.any { ind -> ind.individual.seeAllActions().any { a ->  a is HttpExternalServiceAction && a.active } }
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

    /**
     * Add a function which sets the termination criteria
     */
    fun convertSolutionToExecutiveSummary() : Solution<T> {
        return Solution(individuals, testSuiteNamePrefix, testSuiteNameSuffix, Termination.SUMMARY,
            individualsDuringSeeding, targetsDuringSeeding)
    }
}

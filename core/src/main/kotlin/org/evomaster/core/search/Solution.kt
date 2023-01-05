package org.evomaster.core.search

import org.evomaster.core.database.DbAction
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction


class Solution<T>(
        val individuals: MutableList<EvaluatedIndividual<T>>,
        val testSuiteNamePrefix: String,
        val testSuiteNameSuffix: String,
        val termination: Termination = Termination.NONE
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

    fun hasAnyUsageOfDefaultExternalService() : Boolean{
        return individuals.any{ind -> ind.fitness.getViewEmployedDefaultWM().isNotEmpty()}
    }

    fun needsMockedDns() : Boolean{
        return hasAnyActiveHttpExternalServiceAction() || hasAnyUsageOfDefaultExternalService()
    }

    fun hasAnySqlAction() : Boolean{
        return individuals.any { ind -> ind.individual.seeAllActions().any { a ->  a is DbAction}}
    }
}
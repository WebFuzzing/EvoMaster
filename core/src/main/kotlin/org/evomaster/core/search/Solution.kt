package org.evomaster.core.search

import org.evomaster.core.output.Termination


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
}
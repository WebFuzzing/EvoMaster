package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution


/**
 * Created by arcuri82 on 11-Nov-19.
 */
object TestSuiteSplitter {


    /**
     * Given a [Solution], split it into several smaller solutions, based on the given [type] strategy.
     * No test must be lost, and combining/aggregating all those smaller solutions should give back
     * the original [Solution]
     */
    fun split(solution: Solution<*>, type: EMConfig.TestSuiteSplitType) : List<Solution<*>>{

        return when(type){
            EMConfig.TestSuiteSplitType.NONE -> listOf(solution)
            EMConfig.TestSuiteSplitType.CODE -> splitByCode(solution)
        }
    }

    fun <T:Individual> splitByCode(solution: Solution<T>): List<Solution<T>>{


        val s500 = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500

            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !s500.contains(it) &&
            it.evaluatedActions().all { ac ->
                val code = (ac.result as RestCallResult).getStatusCode()
                if(code!=null) code.rem(400) > 100
                else false
            }
        }.toMutableList()

        val remainder = solution.individuals.filter {
            !s500.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        return listOf(Solution(s500, "${solution.testSuiteName}_500s"),
                Solution(successses, "${solution.testSuiteName}_successes"),
                Solution(remainder, "${solution.testSuiteName}_remainder")
        )
    }

}
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

    /**
     * [splitByCode] splits the Solution into several subsets based on the HTTP codes found in the actions.
     * The split is as follows:
     * - all individuals that contain at least one action with a 500 code go into a separate file. A 500 code is likely
     * to be indicative of a fault, and therefore goes into a separate set.
     *
     * - all individuals that contain 2xx and 3xx action only are deemed to be successful, and a "successful" subset
     * is created for them. These are test cases that indicate no problem.
     *
     * - remaining test cases are set in a third subset. These are often test cases that don't contain outright bugs
     * (i.e. 500 actions) but may include 4xx. User errors and input problems may be interesting, hence the separate file.
     * Nevertheless, it is up to individual test engineers to look at these test cases in more depth and decide
     * if any further action or investigation is required.
     */
    fun <T:Individual> splitByCode(solution: Solution<T>): List<Solution<T>>{
        val s500 = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            // Note: we only check for 500 - Internal Server Error. Other 5xx codes are possible, but they're not really
            // related to bug finding. Test cases that have other errors from the 5xx series will end up in the
            // "remainder" subset - as they are neither errors, nor successful runs.
            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !s500.contains(it) &&
            it.evaluatedActions().all { ac ->
                val code = (ac.result as RestCallResult).getStatusCode()
                if(code!=null) code < 400
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
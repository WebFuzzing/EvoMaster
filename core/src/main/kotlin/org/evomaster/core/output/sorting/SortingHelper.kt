package org.evomaster.core.output.sorting

import org.evomaster.core.Lazy
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.naming.TestCaseNamingStrategy
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections.singletonList

class SortingHelper {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SortingHelper::class.java)
    }



    /** [maxStatusCode] sorts Evaluated individuals based on the highest status code (e.g., 500s are first).
     *
     * **/
    private val maxStatusCode: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        val max = ind.seeResults().filterIsInstance<HttpWsCallResult>().maxByOrNull { it.getStatusCode()?:0 }
        (max as HttpWsCallResult).getStatusCode() ?: 0
    }.reversed()

    /**
     * [statusCode] sorts Evaluated individuals based on the status code, as follows:
     *          - first:    5xx
     *          - second:   2xx
     *          - third:    4xx
     */


    private val statusCode: Comparator<EvaluatedIndividual<*>> = compareBy { ind ->
        val min = ind.seeResults().filterIsInstance<HttpWsCallResult>().minByOrNull {
            it.getStatusCode()?.rem(500) ?: 0
        }
        (min?.getStatusCode())?.rem(500) ?: 0
    }

    /** [maxActions] sorts Evaluated individuals based on the number of actions (most actions first).
     */
    private val maxActions: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        ind.individual.seeAllActions().size
    }.reversed()

    /** [minActions] sorts Evaluated individuals based on the number of actions (most actions first).
     */
    private val minActions: Comparator<EvaluatedIndividual<*>> = compareBy { ind ->
        ind.individual.seeAllActions().size
    }

    /**
     * dbInitSize sorts [EvaluatedIndividual] objects on the basis of the presence (and number) of db initialization actions.
     * Currently, this is only supported for [RestIndividual].
     * Note, writing the comparator as [EvaluatedIndividual<RestIndividual>>] seems to break the .sortWith() later on.
     */
    private val dbInitSize: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        if(ind.individual is RestIndividual) {
            ind.individual.seeInitializingActions().size
        }
        else 0
    }.reversed()

    /**
     * [coveredTargets] sorts [EvaluatedIndividual] objects on based on the higher number of covered targets.
     * The purpose is to give an example of sorting based on fitness information.
     */
    private val coveredTargets: Comparator<EvaluatedIndividual<*>> = compareBy {
        it.fitness.numberOfCoveredTargets()
    }

    /**
     *  [comparatorList] holds those comparators that are currently selected for sorting
     *  Note that the order of the comparators is the order their importance/priority.
     */

    private val comparatorList = listOf(statusCode, coveredTargets)

    private val restComparator: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>> { ind ->
        (ind.evaluatedMainActions().last().action as RestCallAction).path.levels()
    }
        .thenBy { ind ->
            val min = ind.seeResults().filterIsInstance<HttpWsCallResult>().minByOrNull {
                it.getStatusCode()?.rem(500) ?: 0
            }
            (min?.getStatusCode())?.rem(500) ?: 0
        }
        .thenBy { ind ->
            (ind.evaluatedMainActions().last().action as RestCallAction).verb
        }

    private val graphQLComparator: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>> { ind ->
        (ind.evaluatedMainActions().last().action as GraphQLAction).methodName
    }
        .thenBy { ind ->
            (ind.evaluatedMainActions().last().action as GraphQLAction).methodType
        }
        .thenBy { ind ->
            (ind.evaluatedMainActions().last().action as GraphQLAction).parameters.size
        }

    private val rpcComparator: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>> { ind ->
        (ind.evaluatedMainActions().last().action as RPCCallAction).getSimpleClassName()
    }
        .thenBy { ind ->
            (ind.evaluatedMainActions().last().action as RPCCallAction).getExecutedFunctionName()
        }
        .thenBy { ind ->
            (ind.evaluatedMainActions().last().action as RPCCallAction).parameters.size
        }


    fun sort(tests: List<TestCase>, testCaseSortingStrategy: SortingStrategy): List<TestCase> {
        val newSort = when (testCaseSortingStrategy) {
            SortingStrategy.COVERED_TARGETS -> sortByComparatorList(comparatorList, tests)
            SortingStrategy.TARGET_INCREMENTAL -> sortByTargetIncremental(tests)
            else -> throw IllegalStateException("Unrecognized sorting strategy $testCaseSortingStrategy")
        }

        return newSort
    }

    @Deprecated("Use other version")
    fun sort(solution: Solution<*>, namingStrategy: TestCaseNamingStrategy, testCaseSortingStrategy: SortingStrategy) : List<TestCase> {
        return sort(namingStrategy.getTestCases(), testCaseSortingStrategy)
    }


    private fun getSortedTestCases(tests: List<TestCase>, comparator: Comparator<EvaluatedIndividual<*>>): List<TestCase> {
        return getSortedTestCases(tests, singletonList(comparator))
    }

    private fun getSortedTestCases(tests: List<TestCase>, comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase> {

        val copy = tests.toMutableList()

        comparators.asReversed().forEach { comp ->
            val w = Comparator<TestCase>{a, b -> comp.compare(a.test,b.test)}
            copy.sortWith(w)
        }

        return copy
    }



    /**
     *Sorting is done according to the comparator list. If no list is provided, individuals are sorted by max status.
     */
    private fun sortByComparatorList (comparators: List<Comparator<EvaluatedIndividual<*>>> = listOf(statusCode),
                                     tests: List<TestCase>

    ): List<TestCase> {
        /**
         * Comparisons, as far as I understand them, are done as follows:
         * First, the list is sorted based on the first criterion.
         * Then, the (now sorted) list, is sorted based on the second criterion.
         * Where two values have equal priority with respect to the most recent sort, they maintain the order (and, thus,
         * are still sorted according to the first criterion).
         *
         * So, a criterion with more priority overrides most other criteria, unless elements have the same value.
         * If too many criteria are used, the ones that are lower on the priority list will not really have a chance to manifest.
         *
         * An example of how this approach is used:
         * = first priority (thus, last to be executed and most likely to be observed) is the [statusCode]. Thus, every
         * test case that contains a 500 code is at the top.
         * = second priority (thus, second to last to be executed), is the [coveredTargets]. Thus, among those test cases
         * that have the same code, the ones with the most covered targets will be at the top (among their sub-group).
         */

        return getSortedTestCases(tests, comparators)
    }

    private fun sortByTargetIncremental(tests: List<TestCase>): List<TestCase> {

        val comparator = when {
            tests.any { it.test.individual is RestIndividual } -> restComparator
            tests.any { it.test.individual is GraphQLIndividual } -> graphQLComparator
            tests.any { it.test.individual is RPCIndividual } -> rpcComparator
            tests.any { it.test.individual is WebIndividual } -> {
                log.warn("Web individuals do not have action based test case naming yet. Defaulting to Numbered strategy.")
                statusCode
            }
            else -> throw IllegalStateException("Unrecognized test individuals with no target incremental based sorting strategy set.")
        }

        return getSortedTestCases(tests, comparator)
    }


}
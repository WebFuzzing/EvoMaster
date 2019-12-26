package org.evomaster.core.output

import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import kotlin.reflect.KFunction1

/**
 * This class is responsible to decide the order in which
 * the test cases should be written in the final test suite.
 * Ideally, the most "interesting" tests should be written first.
 *
 * <br>
 *
 * Furthermore, this class is also responsible for deciding which
 * name each test will have
 */
class TestSuiteOrganizer {

    private val sortingHelper = SortingHelper()
    private val namingHelper = NamingHelper()

    private val defaultSorting = listOf(0, 1)

    fun sortTests(solution: Solution<*>, customNaming: Boolean = false): List<TestCase> {
        //sortingHelper.selectCriteriaByIndex(defaultSorting)
        //TODO here in the future we will have something a bit smarter
        return sortingHelper.sort(solution, namingHelper, customNaming)
    }

}

class NamingHelper {
    /**
     * The presence of a call with a 500 status code will be added to the test name.
     */
    private fun criterion1_500 (individual: EvaluatedIndividual<*>): String{
        if (individual.results.filterIsInstance<RestCallResult>().any{ it.getStatusCode() == 500 }){
            return "_with500"
        }
        return ""
    }

    private fun criterion2_hasPost (individual: EvaluatedIndividual<*>): String{
        if(individual.individual.seeActions().filterIsInstance<RestCallAction>().any{it.verb == HttpVerb.POST} ){
            return "_hasPost"
        }

        return ""
    }

    /**
     * The type of sample is added to the name. This is tied to the the [RestIndividual] and will change with a new problem.
     */
    private fun criterion3_sampling(individual: EvaluatedIndividual<*>): String{
        if(individual.individual is RestIndividual)
            return "_" + (individual.individual as RestIndividual).sampleType
        else return ""
    }

    /**
     * The presence of separate steps for DB initialization will be added to the test name. This is currently tied to
     * the [RestIndividual] and will change with a new problem
     */
    private fun criterion4_dbInit(individual: EvaluatedIndividual<*>): String{
        if ((individual.individual is RestIndividual) && (individual.individual as RestIndividual).dbInitialization.isNotEmpty()){
            return "_" + "hasDbInit"
        }
        else return ""
    }

    private var namingCriteria =  listOf(::criterion1_500)
    private val availableCriteria = listOf(::criterion1_500, ::criterion2_hasPost, ::criterion3_sampling, ::criterion4_dbInit)


    fun suggestName(individual: EvaluatedIndividual<*>): String{
        return namingCriteria.map { it(individual) }.joinToString("")
    }

    fun getAvailableCriteria(): List<KFunction1<EvaluatedIndividual<*>, String>> {
        return availableCriteria
    }

    fun selectCriteria(selected: List<KFunction1<EvaluatedIndividual<*>, String>>){
        if (availableCriteria.containsAll(selected)){
            namingCriteria = selected
        }
        else {
            throw UnsupportedOperationException("The naming criteria chosen appear to not be supported at the moment.")
        }
    }

    fun selectCriteriaByIndex(selected: List<Int>){
        if (availableCriteria.indices.toList().containsAll(selected)){
            for (i in selected)
            namingCriteria = availableCriteria.filterIndexed{ index, _ ->
                selected.contains(index)
            } as List<KFunction1<EvaluatedIndividual<*>, String>>
        }
        else {
            throw UnsupportedOperationException("The naming criteria chosen appear to not be supported at the moment.")
        }
    }


}


class SortingHelper {

    /** [maxStatusCode] sorts Evaluated individuals based on the highest status code (e.g., 500s are first).
     *
     * **/
    private val maxStatusCode: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        val max = ind.results.filterIsInstance<RestCallResult>().maxBy { it.getStatusCode()?:0 }
            (max as RestCallResult).getStatusCode() ?: 0
    }.reversed()

    /**
     * [statusCode] sorts Evaluated individuals based on the status code, as follows:
     *          - first:    5xx
     *          - second:   2xx
     *          - third:    4xx
     */


    private val statusCode: Comparator<EvaluatedIndividual<*>> = compareBy { ind ->
        val min = ind.results.filterIsInstance<RestCallResult>().minBy {
            it.getStatusCode()?.rem(500) ?: 0
        }
        ((min as RestCallResult).getStatusCode())?.rem(500) ?: 0
    }

    /** [maxActions] sorts Evaluated individuals based on the number of actions (most actions first).
     */
    private val maxActions: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        ind.individual.seeActions().size
    }.reversed()

    /** [minActions] sorts Evaluated individuals based on the number of actions (most actions first).
     */
    private val minActions: Comparator<EvaluatedIndividual<*>> = compareBy { ind ->
        ind.individual.seeActions().size
    }

    /**
     * dbInitSize sorts [EvaluatedIndividual] objects on the basis of the presence (and number) of db initialization actions.
     * Currently, this is only supported for [RestIndividual].
     * Note, writing the comparator as [EvaluatedIndividual<RestIndividual>>] seems to break the .sortWith() later on.
     */
    private val dbInitSize: Comparator<EvaluatedIndividual<*>> = compareBy<EvaluatedIndividual<*>>{ ind ->
        if(ind.individual is RestIndividual) {
            ind.individual.dbInitialization.size
        }
        else 0
    }.reversed()

    /**
     * [coveredTargets] sorts [EvaluatedIndividual] objects on based on the higher number of covered targets.
     * The purpose is to give an example of sorting based on fitness information.
     */
    private val coveredTargets: Comparator<EvaluatedIndividual<*>> = compareBy {
        it.fitness.coveredTargets()
    }

    /**
     *  [comparatorList] holds those comparators that are currently selected for sorting
     *  Note that the order of the comparators is the order their importance/priority.
     */

    var comparatorList = listOf(statusCode, coveredTargets)
    private val availableSortCriteria = listOf(statusCode, minActions, coveredTargets, maxStatusCode, maxActions, dbInitSize)



    fun getAvailableCriteria(): List<Comparator<EvaluatedIndividual<*>>> {
        return availableSortCriteria
    }

    fun selectCriteria(selected: List<Comparator<EvaluatedIndividual<*>>>){
        if (availableSortCriteria.containsAll(selected)){
            comparatorList = selected
        }
        else {
            throw UnsupportedOperationException("The sorting criteria chosen appear to not be supported at the moment.")
        }
    }

    fun selectCriteriaByIndex(selected: List<Int>){
        if (availableSortCriteria.indices.toList().containsAll(selected)){
            comparatorList = availableSortCriteria.filterIndexed{ index, _ ->
                selected.contains(index)
            }
        }
        else {
            throw UnsupportedOperationException("The sorting criteria chosen appear to not be supported at the moment.")
        }
    }

    /**
     *Sorting is done according to the comparator list. If no list is provided, individuals are sorted by max status.
     */
    private fun sortByComparatorList (solution: Solution<*>,
                              namingHelper: NamingHelper,
                              comparators: List<Comparator<EvaluatedIndividual<*>>> = listOf(statusCode)

    ): List<TestCase> {
        var counter = 0

        val inds = solution.individuals

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

        comparators.asReversed().forEach {
            //solution.individuals.sortWith(it)
            inds.sortWith(it)
        }

        //return solution.individuals.map{ ind -> TestCase(ind, "test_"  + (counter++) + namingHelper.suggestName(ind))}
        return inds.map{ ind -> TestCase(ind, "test_"  + (counter++) + namingHelper.suggestName(ind))}

    }

    /**
     * No sorting, and just basic name with incremental counter
     */
    private fun naiveSorting(solution: Solution<*>): List<TestCase> {
        var counter = 0
        return solution.individuals.map { ind -> TestCase(ind, "test" + (counter++)) }
    }

    fun sort(solution: Solution<*>, namingHelper: NamingHelper = NamingHelper(), customNaming: Boolean = false): List<TestCase> {
        val newSort = if (customNaming){
            sortByComparatorList(solution, namingHelper, comparatorList)
        }
        else naiveSorting(solution)

        Lazy.assert { solution.individuals.toSet() == newSort.map { it.test }.toSet()}
        return newSort
    }
}
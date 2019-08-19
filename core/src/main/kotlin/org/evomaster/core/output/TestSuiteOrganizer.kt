package org.evomaster.core.output

import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

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
    companion object {
        val sortingHelper = SortingHelper()
        val namingHelper = NamingHelper()


        fun sortTests(solution: Solution<*>, customNaming: Boolean = false): List<TestCase> {

            //TODO here in the future we will have something bit smarter
            return sortingHelper.sort(solution, namingHelper, customNaming)
        }
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

    val namingCriteria =  mutableListOf(::criterion1_500, ::criterion2_hasPost, ::criterion3_sampling, ::criterion4_dbInit)


    fun suggestName(individual: EvaluatedIndividual<*>): String{
        return namingCriteria.map { it(individual) }.joinToString("")
    }

}


class SortingHelper {
    /** maxStatusCodeComparatorInd sorts Evaluated individuals based on the highest status code (e.g., 500s are first).
     *
     * **/
    val maxStatusCodeComparatorInd = compareBy<EvaluatedIndividual<*>>{ind ->
        val max = ind.results.filterIsInstance<RestCallResult>().maxBy { it.getStatusCode()!! }
            (max as RestCallResult).getStatusCode() ?: 0
    }.reversed()

    /** maxNumberOfActionsComparatorInd sorts Evaluated individuals based on the number of actions (most actions first).
     * **/
    val maxNumberOfActionsComparatorInd = compareBy<EvaluatedIndividual<*>>{ ind ->
        ind.individual.seeActions().size
    }.reversed()

    /**
     * dbInitSize sorts [EvaluatedIndividual] objects on the basis of the presence (and number) of db initialization actions.
     * Currently, this is only supported for [RestIndividual].
     * Note, writing the comparator as [EvaluatedIndividual<RestIndividual>>] seems to break the .sortWith() later on.
     */
    val dbInitSize = compareBy<EvaluatedIndividual<*>>{ ind ->
        if(ind.individual is RestIndividual) {
            (ind.individual as RestIndividual).dbInitialization.size
        }
        else 0
    }.reversed()

    /**
     * coveredTargets sorts [EvaluatedIndividual] objects on the basis of the number of covered targets.
     * The purpose is to give an example of sorting based on fitness information.
     */
    val coveredTargets = compareBy<EvaluatedIndividual<*>> {
        it.fitness.coveredTargets()
    }

    /**
     *  [comparatorList] holds those comparators that are currently selected for sorting
     *  Note that (currently) the order of the comparators is inverse to their importance/priority
     */

    val comparatorList = mutableListOf(  maxStatusCodeComparatorInd, dbInitSize)


    /**
     *Sorting is done according to the comparator list. If no list is provided, individuals are sorted by max status.
     */
    fun sortByComparatorList (solution: Solution<*>,
                              namingHelper: NamingHelper,
                              comparators: MutableList<Comparator<EvaluatedIndividual<*>>> = mutableListOf(maxStatusCodeComparatorInd)

    ): List<TestCase> {
        var counter = 0

        comparators.forEach { solution.individuals.sortWith(it) }

        return solution.individuals.map{ ind -> TestCase(ind, "test_"  + (counter++) + namingHelper.suggestName(ind))}
    }

    /**
     * No sorting, and just basic name with incremental counter
     */
    fun naiveSorting(solution: Solution<*>): List<TestCase> {
        var counter = 0
        return solution.individuals.map { ind -> TestCase(ind, "test" + (counter++)) }
    }

    fun sort(solution: Solution<*>, namingHelper: NamingHelper = NamingHelper(), customNaming: Boolean = false): List<TestCase> {
        if (customNaming){
            return sortByComparatorList(solution, namingHelper, comparatorList)
        }
        return naiveSorting(solution)

    }
}
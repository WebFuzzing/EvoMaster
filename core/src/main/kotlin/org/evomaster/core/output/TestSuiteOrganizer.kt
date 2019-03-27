package org.evomaster.core.output


import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import java.lang.IllegalArgumentException
import java.rmi.Naming

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
        fun sortTests(solution: Solution<*>): List<TestCase> {

            //TODO here in the future we will have something bit smarter
            // TODO: The comparators are listed in reverse order of priority. Not sure if this is okay.
            //return sortByComparatorList(solution, mutableListOf(maxNumberOfActionsComparatorInd, maxStatusCodeComparatorInd))
            val sortingHelper = SortingHelper()
            val namingHelper = NamingHelper()

            return sortingHelper.sort(solution, namingHelper, false)
        }
    }
}

class NamingHelper {
    private fun criterion1_500 (individual: EvaluatedIndividual<*>): String{
        var found500 = false
        individual.results.forEach {
            if((it as RestCallResult).getStatusCode() == 500) found500 = true
        }
        val suggestedName = when (found500) {
            true ->  "_with500_"
            false ->  ""
            else -> throw IllegalArgumentException("This should not happen")
            // else ->  "wat?"
            // throw Exception instead?
        }
        return suggestedName
    }

    private fun criterion2_hasPost (individual: EvaluatedIndividual<*>): String{
        var foundPost = false
        individual.individual.seeActions().forEach {
            if ((it as RestCallAction).verb == HttpVerb.POST ) foundPost=true
        }
        val suggestedName = when (foundPost) {
            true ->  "_hasPost_"
            false ->  ""
            else -> throw IllegalArgumentException("This should not happen")
            //else ->  "wat?"
        }
        return suggestedName
    }

    private fun criterion3_sampling(individual: EvaluatedIndividual<*>): String{
        return "_" + (individual.individual as RestIndividual).sampleType + "_"
    }

    private fun criterion4_dbInit(individual: EvaluatedIndividual<*>): String{
        if ((individual.individual as RestIndividual).dbInitialization.isNotEmpty()){
            return "_" + "hasDbInit" + "_"
        }
        else return ""
    }

    val namingCriteria =  mutableListOf(::criterion1_500, ::criterion2_hasPost, ::criterion3_sampling, ::criterion4_dbInit)


    fun suggestName(individual: EvaluatedIndividual<*>): String{
        var suggestedName = "test"
        namingCriteria.forEach {
            suggestedName += it(individual)
        }
        return suggestedName
    }

}


class SortingHelper {
    /** maxStatusCodeComparatorInd sorts Evaluated individuals based on the highest status code (e.g., 500s are first).
     * **/
    val maxStatusCodeComparatorInd = compareBy<EvaluatedIndividual<*>>{ind ->
        val max = ind.results.maxBy { (it as RestCallResult).getStatusCode()!! }
        (max as RestCallResult).getStatusCode() ?: 0
        //there might be a Null Pointer Exception being thrown from here.
    }.reversed()

    /** maxNumberOfActionsComparatorInd sorts Evaluated individuals based on the number of actions (most actions first).
     * **/
    val maxNumberOfActionsComparatorInd = compareBy<EvaluatedIndividual<*>>{ ind ->
        ind.individual.seeActions().size
    }.reversed()

    val dbInitSize = compareBy<EvaluatedIndividual<*>>{ ind ->
        (ind.individual as RestIndividual).dbInitialization.size
    }.reversed()

    val coveredTargets = compareBy<EvaluatedIndividual<*>> {
        it.fitness.coveredTargets()
    }

    /**
     *  [comparatorList] holds those comparators that are currently selected for sorting
     *  Note that (currently) the order of the comparators is inverse to their importance/priority
     */

    var comparatorList = mutableListOf( dbInitSize, maxStatusCodeComparatorInd)

    /**
     *Sorting is done according to the comparator list. If no list is provided, individuals are sorted by max status.
     */
    fun sortByComparatorList (solution: Solution<*>,
                              namingHelper: NamingHelper,
                              comparators: MutableList<Comparator<EvaluatedIndividual<*>>> = mutableListOf(maxStatusCodeComparatorInd)

    ): List<TestCase> {
        var counter = 0
        comparators.forEach { solution.individuals.sortWith(it) }
        return solution.individuals.map{ ind -> TestCase(ind, namingHelper.suggestName(ind) + (counter++))}
    }

    /**
     * No sorting, and just basic name with incremental counter
     */
    fun naiveSorting(solution: Solution<*>): List<TestCase> {

        var counter = 0

        return solution.individuals.map { ind -> TestCase(ind, "test" + (counter++)) }
    }

    fun sort(solution: Solution<*>, namingHelper: NamingHelper = NamingHelper(), naive: Boolean = false): List<TestCase> {
        if (naive){
            return naiveSorting(solution)
        }
        return sortByComparatorList(solution, namingHelper, comparatorList)
    }

}

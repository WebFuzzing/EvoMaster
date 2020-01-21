package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual
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

        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        if( type == EMConfig.TestSuiteSplitType.CLUSTER && errs.size <= 1) return listOf(solution)
        // no clustering is attempted if there are not enough individuals containing errors

        return when(type){
            EMConfig.TestSuiteSplitType.NONE  -> listOf(solution)
            EMConfig.TestSuiteSplitType.CLUSTER -> splitByClusters(solution as Solution<RestIndividual>)
            EMConfig.TestSuiteSplitType.SUMMARY_ONLY -> executiveSummary(solution as Solution<RestIndividual>)
            EMConfig.TestSuiteSplitType.CODE -> splitByCode(solution)
                //splitByClusters(solution as Solution<RestIndividual>)
        }
    }


    /**
     * [sortByClusters] filters the error (i.e. containing 500s) [EvaluatedIndividual] and sorts them based on their
     * membership in the various clusters. The idea is to provide a single [Solution] that contains all the error
     * [EvaluatedIndividual], sorted by cluster.
     *
     * At the moment, the exact order of clusters is not particularly relevant, but may be useful for prioritization
     * in some future iteration.
     *
     *
     */

    fun sortByClusters(solution: Solution<RestIndividual>) : Solution<RestIndividual>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_errs"))
        val individuals = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.map {
                it.assignToCluster(index)
            }.toMutableList()
            individuals.addAll(inds)
        }

        val sortedSolution = Solution(individuals, "${solution.testSuiteName}_clustered")
        return sortedSolution
    }

    /**
     *
     */

    fun executiveSummary(solution: Solution<RestIndividual>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        // If no individuals have a 500 result, the summary is empty
        // If only one individual has a 500 result, clustering is skipped, and the relevant individual is returned
        when (errs.size){
            0 -> return mutableListOf()
            1 -> return mutableListOf(Solution(errs, "${solution.testSuiteName}_executiveSummary"))
        }

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_executiveSummary"))
        val sumSol = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.toMutableList()
            // Add a random individual from each cluster.
            // Other selection criteria than random might be added at some later date.
            // For example, one might want the smallest individual in a cluster (i.e. the smallest test case that
            // shows a particular type of behaviour).
            // sumSol.add(index, inds.random())
            sumSol.add(index, inds.minBy { it.individual.seeActions().size } ?: inds.random())
        }

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any { it.contains(ac.result as RestCallResult) }
            }
        }
        // add any Individuals that have a 500 action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }

        val sumSolution = Solution(sumSol, "${solution.testSuiteName}_executiveSummary")
        return mutableListOf(sumSolution)
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
    private fun <T:Individual> splitByCode(solution: Solution<T>): List<Solution<T>>{
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

        return listOf(Solution(s500, "${solution.testSuiteName}_errs"),
                Solution(successses, "${solution.testSuiteName}_successes"),
                Solution(remainder, "${solution.testSuiteName}_remainder")
        )
    }

    /**
     * [splitByClusters] splits a given Solution object into a List of several Solution objects, each
     * containing a cluster of (error - i.e. containing 500s) [EvaluatedIndividual<RestIndividual>]. Each such solution
     * can be printed as a separate test file.
     *
     * NOTE: This is currently not in use, as having lots of small files may be a problem for the potential users,
     * though it can be activated if a need/requirement for such information can be determined.
     *
     * Futhermore, if a particular type of fault is found to be of greater interest, this could be the starting point
     * for getting all the additional test cases related to that fault (i.e. belonging to the same cluster).
     */

    fun splitByClusters(solution: Solution<RestIndividual>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !errs.contains(it) &&
                    it.evaluatedActions().all { ac ->
                        val code = (ac.result as RestCallResult).getStatusCode()
                        if(code!=null) code < 400
                        else false
                    }
        }.toMutableList()

        val solSuccesses = Solution(successses, "${solution.testSuiteName}_successes")

        val remainder = solution.individuals.filter {
            !errs.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        val solRemainder = Solution(remainder, "${solution.testSuiteName}_remainder")

        // If no individuals have a 500 result, the summary is empty
        // If only one individual has a 500 result, clustering is skipped, and the relevant individual is returned
        when (errs.size){
            0 -> return mutableListOf(solSuccesses, solRemainder)
            1 -> return mutableListOf(Solution(errs, "${solution.testSuiteName}_errs"), solSuccesses, solRemainder)
        }

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_errs"))

        val sumSol = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.toMutableList()
            // Add a random individual from each cluster.
            // Other selection criteria than random might be added at some later date.
            // For example, one might want the smallest individual in a cluster (i.e. the smallest test case that
            // shows a particular type of behaviour).
            // sumSol.add(index, inds.random())
            sumSol.add(index, inds.minBy { it.individual.seeActions().size } ?: inds.random())
        }

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any { it.contains(ac.result as RestCallResult) }
            }
        }
        // add any Individuals that have a 500 action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }

        val solErrors = Solution(sumSol, "${solution.testSuiteName}_errs")
        return mutableListOf(solErrors,
                solSuccesses,
                solRemainder)

        /*val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_errs"))
        val clusteredSolutions = mutableListOf<Solution<RestIndividual>>()
        val individuals = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.map {
                it.assignToCluster(index)
            }.toMutableList()
            clusteredSolutions.add(index, Solution(inds, "C_$index"))
            individuals.addAll(inds)
        }
        return clusteredSolutions

         */
    }

}
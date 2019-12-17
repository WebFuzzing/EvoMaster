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

        if(errs.size <= 1) return listOf(solution)

        return when(type){
            EMConfig.TestSuiteSplitType.NONE -> listOf(solution)
            EMConfig.TestSuiteSplitType.CLUSTER -> listOf(sortByClusters(solution as Solution<RestIndividual>))
            EMConfig.TestSuiteSplitType.SUMMARY -> executiveSummary(solution as Solution<RestIndividual>)
                //splitByClusters(solution as Solution<RestIndividual>)
        }
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


        //Could this be a quick check to see if any 500s were skipped?
        /*
        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any { it.contains(ac.result as RestCallResult) }
            }

        }

        In debugger I visualize it like this:
        skipped.map {
            it.evaluatedActions().map { "" + Gson().fromJson((it.result as RestCallResult).getBody(), Map::class.java)?.get("message") + it.result.getStatusCode() }
        }
        */
        return clusteredSolutions
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

        val sortedSolution = Solution(individuals, "Clustered")
        return sortedSolution
    }

    fun executiveSummary(solution: Solution<RestIndividual>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        when (errs.size){
            0, 1 -> return mutableListOf()
        }

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_errs"))
        val sumSol = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.toMutableList()
            sumSol.add(index, inds.random())
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

        val sumSolution = Solution(sumSol, "EM_executiveSummary")
        return mutableListOf(sumSolution)
    }

}
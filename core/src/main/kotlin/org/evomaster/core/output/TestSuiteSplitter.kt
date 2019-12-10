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
        return when(type){
            EMConfig.TestSuiteSplitType.NONE -> listOf(solution)
            EMConfig.TestSuiteSplitType.CLUSTER ->  executiveSummary(solution as Solution<RestIndividual>)
                //splitByClusters(solution as Solution<RestIndividual>)
        }
    }

    fun splitByClusters(solution: Solution<RestIndividual>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

        val clusters = Clusterer.cluster(Solution(errs, "${solution.testSuiteName}_errs"))
        val clusteredSolutions = mutableListOf<Solution<RestIndividual>>()
        val summarySolutions = mutableListOf<Solution<RestIndividual>>()

        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.toMutableList()
            clusteredSolutions.add(index, Solution(inds, "C_$index"))
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

    fun executiveSummary(solution: Solution<RestIndividual>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                (ac.result as RestCallResult).getStatusCode() == 500
            }
        }.toMutableList()

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
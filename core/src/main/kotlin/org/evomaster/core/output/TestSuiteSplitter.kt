package org.evomaster.core.output

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.output.clustering.metrics.DistanceMetric
import org.evomaster.core.output.clustering.metrics.DistanceMetricErrorText
import org.evomaster.core.output.clustering.metrics.DistanceMetricLastLine
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.*
import org.evomaster.core.search.service.SearchTimeController


/**
 * Created by arcuri82 on 11-Nov-19.
 */
object TestSuiteSplitter {
    fun split(solution: Solution<*>,
              config: EMConfig) : SplitResult { //List<Solution<*>>{
        return split(solution as Solution<RestIndividual>, config, PartialOracles())
    }
    /**
     * Given a [Solution], split it into several smaller solutions, based on the given [type] strategy.
     * No test must be lost, and combining/aggregating all those smaller solutions should give back
     * the original [Solution]
     */
    fun split(solution: Solution<*>,
              config: EMConfig,
              oracles: PartialOracles) : SplitResult { //List<Solution<*>>{

        val type = config.testSuiteSplitType
        val sol = solution as Solution<RestIndividual>
        val metrics = mutableListOf(DistanceMetricErrorText(), DistanceMetricLastLine())
        val errs = sol.individuals.filter {ind ->
            if (ind.individual is RestIndividual) {
                ind.evaluatedActions().any {ac ->
                    assessFailed(ac, oracles)
                }
            }
            else false
        }.toMutableList()

        val splitResult = SplitResult()

        if( type == EMConfig.TestSuiteSplitType.CLUSTER && errs.size <= 1) splitResult.splitOutcome = splitByCode(sol)

        when(type){
            EMConfig.TestSuiteSplitType.NONE  -> splitResult.splitOutcome = listOf(sol)
            EMConfig.TestSuiteSplitType.CLUSTER -> {
                val clusters = conductClustering(sol, oracles, metrics, splitResult)
                splitByCluster(clusters, sol, oracles, splitResult)
            }
            EMConfig.TestSuiteSplitType.SUMMARY_ONLY -> {
                val clusters = conductClustering(sol, oracles, metrics, splitResult)
                //splitResult.splitOutcome = listOf(sol)
                splitResult.splitOutcome = splitByCode(sol, oracles)
            }
            EMConfig.TestSuiteSplitType.CODE -> splitResult.splitOutcome = splitByCode(sol, oracles)
        }

        return splitResult
    }

    private fun conductClustering(solution: Solution<RestIndividual>,
                                  oracles: PartialOracles = PartialOracles(),
                                  metrics: List<DistanceMetric<RestCallResult>>,
                                  splitResult: SplitResult) : MutableMap<String, MutableList<MutableList<RestCallResult>>> {

        val clusteringStart = System.currentTimeMillis()
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.toMutableList()

        // If no individuals have a failed result, the summary is empty
        // If only one individual has a failed result, clustering is skipped, and the relevant individual is returned

        when (errs.size) {
            0 -> splitResult.splitOutcome = mutableListOf()
            1 -> splitResult.splitOutcome = mutableListOf(Solution(errs, solution.testSuiteName, Termination.SUMMARY))
        }
        val clusters = mutableMapOf<String, MutableList<MutableList<RestCallResult>>>()
        val clusteringSol = Solution(errs, solution.testSuiteName, Termination.SUMMARY)

        for (metric in metrics) {
            clusters[metric.getName()] = Clusterer.cluster(
                    //Solution(errs, solution.testSuiteName, Termination.SUMMARY),
                    clusteringSol,
                    oracles = oracles,
                    metric = metric)
        }
        solution.clusteringTime = ((System.currentTimeMillis() - clusteringStart) / 1000).toInt()
        splitResult.clusteringTime = System.currentTimeMillis() - clusteringStart
        //If clustering is done, the executive summary is, essentially, for free.
        splitResult.executiveSummary = execSummary(clusters, solution, splitResult)
        return clusters
    }

    /**
     * The [execSummary] function takes in a solution, clusters individuals containing errors by error messsage,
     * then picks from each cluster one individual.
     *
     * The individual selected is the shortest (by action count) or random.
     */

    private fun execSummary(clusters : MutableMap<String, MutableList<MutableList<RestCallResult>>>,
                            solution: Solution<RestIndividual>,
                            splitResult: SplitResult
            ) : Solution<RestIndividual> {
        val execSol = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        clusters.values.forEach { it.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac -> clu.contains(ac.result as RestCallResult) }
            }.toMutableList()
            execSol.add(index, inds.minBy { it.individual.seeActions().size } ?: inds.random())
        } }
        return Solution(execSol, solution.testSuiteName, Termination.SUMMARY)
    }

    private fun splitByCluster(clusters: MutableMap<String, MutableList<MutableList<RestCallResult>>>,
                                 solution: Solution<RestIndividual>,
                                 oracles: PartialOracles,
                                 splitResult: SplitResult) : SplitResult {

        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.toMutableList()

        //Successes
        val successses = solution.individuals.filter {
            !errs.contains(it) &&
                    it.evaluatedActions().all { ac ->
                        val code = (ac.result as RestCallResult).getStatusCode()
                        (code != null && code < 400)
                    }
        }.toMutableList()

        val solSuccesses = Solution(successses, solution.testSuiteName, Termination.SUCCESSES)
        val remainder = solution.individuals.filter {
            !errs.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        val solRemainder = Solution(remainder, solution.testSuiteName, Termination.OTHER)

        // Failures by cluster
        val sumSol = mutableSetOf<EvaluatedIndividual<RestIndividual>>()
        sumSol.addAll(solution.individuals.filter { it.clusterAssignments.size > 0 })

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any {
                    it.value.any { va -> va.contains(ac.result as RestCallResult) } }
            }
        }
        // add any Individuals that have a failed action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }
        val solErrors = Solution(sumSol.toMutableList(), solution.testSuiteName, Termination.FAULTS)
        splitResult.splitOutcome = mutableListOf(solErrors,
                solSuccesses,
                solRemainder)
        return splitResult
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
    private fun <T:Individual> splitByCode(solution: Solution<T>,
                                           oracles: PartialOracles = PartialOracles()): List<Solution<T>>{
        val s500 = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)

            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !s500.contains(it) &&
            it.evaluatedActions().all { ac ->
                val code = (ac.result as RestCallResult).getStatusCode()
                (code != null && code < 400)
            }
        }.toMutableList()

        val remainder = solution.individuals.filter {
            !s500.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        return listOf(Solution(s500, solution.testSuiteName, Termination.FAULTS),
                Solution(successses, solution.testSuiteName, Termination.SUCCESSES),
                Solution(remainder, solution.testSuiteName, Termination.OTHER)
        )
    }

    fun assessFailed(action: EvaluatedAction, oracles: PartialOracles): Boolean{
        val codeSelect = if(action.result is RestCallResult){
            val code = (action.result as RestCallResult).getStatusCode()
            (code != null && code == 500)
            // Note: we only check for 500 - Internal Server Error. Other 5xx codes are possible, but they're not really
            // related to bug finding. Test cases that have other errors from the 5xx series will end up in the
            // "remainder" subset - as they are neither errors, nor successful runs.
        } else false

        val oracleSelect = oracles.selectForClustering(action)
        return codeSelect || oracleSelect
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
    /*

    private fun splitByClusters(solution: Solution<RestIndividual>,
                        oracles: PartialOracles = PartialOracles(),
                        metric: DistanceMetric<RestCallResult>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !errs.contains(it) &&
                    it.evaluatedActions().all { ac ->
                        val code = (ac.result as RestCallResult).getStatusCode()
                        (code != null && code < 400)
                    }
        }.toMutableList()

        val solSuccesses = Solution(successses, solution.testSuiteName, Termination.SUCCESSES)

        val remainder = solution.individuals.filter {
            !errs.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        val solRemainder = Solution(remainder, solution.testSuiteName, Termination.OTHER)

        // If no individuals have a 500 result, the summary is empty
        // If only one individual has a 500 result, clustering is skipped, and the relevant individual is returned
        when (errs.size){
            0 -> return mutableListOf(solSuccesses, solRemainder)
            1 -> return mutableListOf(Solution(errs, solution.testSuiteName, Termination.FAULTS), solSuccesses, solRemainder)
        }

        val sumSol = mutableSetOf<EvaluatedIndividual<RestIndividual>>()

        val clusters = Clusterer.cluster(Solution(errs, solution.testSuiteName, Termination.FAULTS), oracles = oracles, metric = metric)
        //BMR: let's try a second clustering, then

        val metric2 = DistanceMetricLastLine()
        val clusters2 = Clusterer.cluster(Solution(errs, solution.testSuiteName, Termination.FAULTS), oracles = oracles, metric = metric2)

        sumSol.addAll(solution.individuals.filter { it.clusterAssignments.size > 0 })

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any { it.contains(ac.result as RestCallResult) } ||
                        clusters2.any { it.contains(ac.result as RestCallResult) }
            }
        }
        // add any Individuals that have a failed action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }

        val solErrors = Solution(sumSol.toMutableList(), solution.testSuiteName, Termination.FAULTS)
        return mutableListOf(solErrors,
                solSuccesses,
                solRemainder)
    }


    private fun executiveSummary(solution: Solution<RestIndividual>,
                                 oracles: PartialOracles = PartialOracles(),
                                 metric: DistanceMetric<RestCallResult>): List<Solution<RestIndividual>>{
        val errs = solution.individuals.filter {
            it.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.toMutableList()

        // If no individuals have a failed result, the summary is empty
        // If only one individual has a failed result, clustering is skipped, and the relevant individual is returned
        when (errs.size){
            0 -> return mutableListOf()
            1 -> return mutableListOf(Solution(errs, solution.testSuiteName, Termination.SUMMARY))
        }
        val sumSol = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        val clusters = Clusterer.cluster(Solution(errs, solution.testSuiteName, Termination.SUMMARY), oracles = oracles, metric = metric)


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
            sumSol.add(index, inds.minBy { it.individual.seeActions().size } ?: inds.random())
        }

        val metric2 = DistanceMetricLastLine()
        val clusters2 = Clusterer.cluster(Solution(errs, solution.testSuiteName, Termination.FAULTS), oracles = oracles, metric = metric2)

        clusters2.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.map {
                it.assignToCluster("${metric2.getName()}_$index")
            }.toMutableSet()
            sumSol.add(index, inds.minBy { it.individual.seeActions().size } ?: inds.random())
        }

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedActions().any { ac ->
                assessFailed(ac, oracles)
            }
        }.filterNot { ind ->
            ind.evaluatedActions().any { ac ->
                clusters.any { it.contains(ac.result as RestCallResult) }
            }
        }
        // add any Individuals that have a failed action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }

        val sumSolution = Solution(sumSol, solution.testSuiteName, Termination.SUMMARY)
        return mutableListOf(sumSolution)
    }




     */
}
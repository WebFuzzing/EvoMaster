package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.output.clustering.metrics.DistanceMetric
import org.evomaster.core.output.clustering.metrics.DistanceMetricErrorText
import org.evomaster.core.output.clustering.metrics.DistanceMetricLastLine
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.search.*
import com.google.gson.*
import org.evomaster.core.problem.api.ApiWsIndividual

/**
 * Created by arcuri82 on 11-Nov-19.
 */
object TestSuiteSplitter {

    const val MULTIPLE_RPC_INTERFACES  = "MultipleRPCInterfaces"

    /**
     * simple split based on whether it exists exception based on RPC results
     */
    fun splitRPCByException(solution: Solution<RPCIndividual>): SplitResult{

        val other = solution.individuals.filter { i-> i.seeResults().any { r-> r is RPCCallResult && !r.isExceptionThrown() } }

        val clusterOther = other.groupBy {
            if (it.individual.getTestedInterfaces().size == 1){
                formatClassNameInTestName(it.individual.getTestedInterfaces().first(), true)
            }else{
                MULTIPLE_RPC_INTERFACES
            }
        }

        val exceptionGroup = solution.individuals.filterNot { other.contains(it) }.groupBy {
            i ->
            i.seeResults().filterIsInstance<RPCCallResult>()
                .minOfOrNull { it.getExceptionImportanceLevel()}?:-1
        }
        return SplitResult().apply {
            this.splitOutcome = clusterOther.map {o->
                Solution(
                    individuals = o.value.toMutableList(),
                    testSuiteNamePrefix = "${solution.testSuiteNamePrefix}_${o.key}",
                    testSuiteNameSuffix = solution.testSuiteNameSuffix,
                    termination = Termination.OTHER,listOf(), listOf()
                )
            }
//                .plus(Solution(individuals = other.toMutableList(),
//                    testSuiteNamePrefix = solution.testSuiteNamePrefix,
//                    testSuiteNameSuffix = solution.testSuiteNameSuffix,
//                    termination = Termination.OTHER, listOf()))
                .plus(
                exceptionGroup.map { e->
                    var level = "Undefined"
                    if (e.key >= 0)
                        level = "_P${e.key}"

                    Solution(
                        individuals = e.value.toMutableList(),
                        testSuiteNamePrefix = "${solution.testSuiteNamePrefix}${level}",
                        testSuiteNameSuffix = solution.testSuiteNameSuffix,
                        termination = Termination.EXCEPTION,listOf(), listOf()
                    )
                }
            )
        }
    }

    fun split(solution: Solution<*>,
              config: EMConfig) : SplitResult { //List<Solution<*>>{
        return split(solution as Solution<*>, config, PartialOracles())
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

        // BMR: Splitting support for new problems
        val sol = if(config.problemType == EMConfig.ProblemType.GRAPHQL){
            solution as Solution<GraphQLIndividual>
        } else {
            solution as Solution<RestIndividual>
        }
        val metrics = mutableListOf(DistanceMetricErrorText(config.errorTextEpsilon), DistanceMetricLastLine(config.lastLineEpsilon))
        val errs = sol.individuals.filter {ind ->
            if (ind.individual is RestIndividual || ind.individual is GraphQLIndividual) {
                ind.evaluatedMainActions().any { ac ->
                    assessFailed(ac, oracles, config)
                }
            }
            else false
        }.toMutableList()

        val splitResult = SplitResult()

        when(type){
            EMConfig.TestSuiteSplitType.NONE  -> splitResult.splitOutcome = listOf(sol)
            EMConfig.TestSuiteSplitType.CODE -> splitResult.splitOutcome = splitByCode(sol, config)
            EMConfig.TestSuiteSplitType.CLUSTER -> {
                if(errs.size <= 1){
                    splitResult.splitOutcome = splitByCode(sol, config)

                    // TODO: BMR - what is the executive summary behaviour for 1 or fewer errors?
                    // Onur - Executive summary gets all success cases in case there are no faults.
                    // So if the executive summary gets all success cases, it should not be shown.
                    splitResult.executiveSummary = sol.convertSolutionToExecutiveSummary()
                } else {
                    val clusters = conductClustering(sol as Solution<ApiWsIndividual>, oracles, config, metrics, splitResult)
                    splitByCluster(clusters, sol, oracles, splitResult, config)
                }

            }

        }

        return splitResult
    }

    /**
     * @return split test suite based on specified maximum number [limit]
     */
    fun splitSolutionByLimitSize(solution: Solution<ApiWsIndividual>, limit: Int) : List<Solution<ApiWsIndividual>>{
        if (limit < 0) return listOf(solution)
        val group = solution.individuals.groupBy {
            solution.individuals.indexOf(it) / limit
        }

        return group.map {g->
            Solution(
                individuals = g.value.toMutableList(),
                testSuiteNamePrefix = "${solution.testSuiteNamePrefix}_${g.key}",
                testSuiteNameSuffix = solution.testSuiteNameSuffix,
                termination = solution.termination,listOf(), listOf()
            )
        }
    }

    private fun conductClustering(solution: Solution<ApiWsIndividual>,
                                  oracles: PartialOracles = PartialOracles(),
                                  config: EMConfig,
                                  metrics: List<DistanceMetric<HttpWsCallResult>>,
                                  splitResult: SplitResult) : MutableMap<String, MutableList<MutableList<HttpWsCallResult>>> {

        val clusteringStart = System.currentTimeMillis()
        val errs = solution.individuals.filter {
            it.evaluatedMainActions().any { ac ->
                assessFailed(ac, oracles, config)
            }
        }.toMutableList()

        val clusterableActions = errs.flatMap {
            it.evaluatedMainActions().filter { ac ->
                TestSuiteSplitter.assessFailed(ac, oracles, config)
            }
        }.map { ac -> ac.result }
                .filterIsInstance<HttpWsCallResult>()

        // If no individuals have a failed result, the summary is empty
        // If only one individual has a failed result, clustering is skipped, and the relevant individual is returned

        when (clusterableActions.size) {
            0 -> splitResult.splitOutcome = mutableListOf()
            1 -> splitResult.splitOutcome = mutableListOf(Solution(
                errs,
                solution.testSuiteNamePrefix,
                solution.testSuiteNameSuffix,
                Termination.SUMMARY,
                listOf(),
                listOf()
            ))
        }
        val clusters = mutableMapOf<String, MutableList<MutableList<HttpWsCallResult>>>()
        val clusteringSol = Solution(
            errs,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.SUMMARY,
            listOf(),
            listOf()
        )

        /**
        In order for clustering to make sense, we need a set of clusterable actions with at least 2 elements.
         */
        if(clusterableActions.size >= 2){
            for (metric in metrics) {
                clusters[metric.getName()] = Clusterer.cluster(
                        //Solution(errs, solution.testSuiteName, Termination.SUMMARY),
                        clusteringSol,
                        config,
                        epsilon = metric.getRecommendedEpsilon(),
                        oracles = oracles,
                        metric = metric)
            }
        }

        solution.clusteringTime = ((System.currentTimeMillis() - clusteringStart) / 1000).toInt()
        splitResult.clusteringTime = System.currentTimeMillis() - clusteringStart
        //If clustering is done, the executive summary is, essentially, for free.
        splitResult.executiveSummary = execSummary(clusters, solution, oracles, splitResult)
        return clusters
    }

    /**
     * The [execSummary] function takes in a solution, clusters individuals containing errors by error messsage,
     * then picks from each cluster one individual.
     *
     * The method uses [MutableSet] to ensure the uniqueness of [EvaluatedIndividual] objects
     * selected for inclusion in the summary.
     *
     * The individual selected is the shortest (by action count) or random.
     */

    private fun execSummary(clusters : MutableMap<String, MutableList<MutableList<HttpWsCallResult>>>,
                            solution: Solution<ApiWsIndividual>,
                            oracles: PartialOracles,
                            splitResult: SplitResult
            ) : Solution<ApiWsIndividual> {

        // MutableSet is used here to ensure the uniqueness of TestCases selected for the executive summary.
        val execSol = mutableSetOf<EvaluatedIndividual<ApiWsIndividual>>()
        clusters.values.forEach { it.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedMainActions().any { ac -> clu.contains(ac.result as HttpWsCallResult) }
            }.toMutableList()
            inds.sortBy { it.individual.seeAllActions().size }
            inds.firstOrNull { execSol.add(it) }
        } }

        val oracleInds = oracles.failByOracle(solution.individuals)
        oracleInds.forEach { key, ind ->
            ind.firstOrNull { execSol.add(it) }
        }

        val execSolList = execSol.toMutableList()
        return Solution(
            execSolList,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.SUMMARY,
            listOf(),
            listOf()
        )
    }

    private fun splitByCluster(clusters: MutableMap<String, MutableList<MutableList<HttpWsCallResult>>>,
                               solution: Solution<ApiWsIndividual>,
                               oracles: PartialOracles,
                               splitResult: SplitResult,
                               config: EMConfig) : SplitResult {

        val errs = solution.individuals.filter {
            it.evaluatedMainActions().any { ac ->
                assessFailed(ac, oracles, config)
            }
        }.toMutableList()

        //Successes
        val successses = solution.individuals.filter {
            !errs.contains(it) &&
                    it.evaluatedMainActions().all { ac ->
                        val code = (ac.result as HttpWsCallResult).getStatusCode()
                        (code != null && code < 400)
                    }
        }.toMutableList()

        val solSuccesses = Solution(
            successses,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.SUCCESSES,
            listOf(),
            listOf()
        )
        val remainder = solution.individuals.filter {
            !errs.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        val solRemainder = Solution(
            remainder,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.OTHER,
            listOf(),
            listOf()
        )

        // Failures by cluster
        val sumSol = mutableSetOf<EvaluatedIndividual<ApiWsIndividual>>()
        sumSol.addAll(solution.individuals.filter { it.clusterAssignments.size > 0 })

        val skipped = solution.individuals.filter { ind ->
            ind.evaluatedMainActions().any { ac ->
                assessFailed(ac, oracles, config)
            }
        }.filterNot { ind ->
            ind.evaluatedMainActions().any { ac ->
                clusters.any {
                    it.value.any { va -> va.contains(ac.result as HttpWsCallResult) } }
            }
        }
        // add any Individuals that have a failed action and belong to no cluster to the executive summary too.
        skipped.forEach {
            sumSol.add(it)
        }
        val solErrors = Solution(
            sumSol.toMutableList(),
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.FAULTS,
            listOf(),
            listOf()
        )
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
    private fun <T:Individual> splitByCode(solution: Solution<T>, config: EMConfig): List<Solution<T>>{
        val s500 = solution.individuals.filter {
            it.evaluatedMainActions().any { ac ->
                assessFailed(ac, null, config)

            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !s500.contains(it) &&
            it.evaluatedMainActions().all { ac ->
                val code = (ac.result as HttpWsCallResult).getStatusCode()
                (code != null && code < 400)
            }
        }.toMutableList()

        val remainder = solution.individuals.filter {
            !s500.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        return listOf(Solution(
            s500,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.FAULTS,
            listOf(),
            listOf()
        ),
                Solution(
                    successses,
                    solution.testSuiteNamePrefix,
                    solution.testSuiteNameSuffix,
                    Termination.SUCCESSES,
                    listOf(),
                    listOf()
                ),
                Solution(
                    remainder,
                    solution.testSuiteNamePrefix,
                    solution.testSuiteNameSuffix,
                    Termination.OTHER,
                    listOf(),
                    listOf()
                )
        )
    }

    /***
     * A [GraphQlCallResult] is considered to be "failed" (and thus a potential fault)
     * if it contains the field "errors" in its body.
     */
    fun assessFailed(result: GraphQlCallResult): Boolean{
        val resultBody = try {
            Gson().fromJson(result.getBody(), HashMap::class.java)
        } catch (e : JsonSyntaxException){
            return true //TODO should this be treated specially???
        }
        val errMsg = resultBody?.get("errors")
        return (resultBody!=null && errMsg != null)
    }

    /***
     * A [HttpWsCallResult] is considered failed if it has a 500 code (i.e.
     * if it contains a server error) OR if it contains no code at all.
     */
    fun assessFailed(result: HttpWsCallResult): Boolean {
        val code = result.getStatusCode()
        return (code != null && code == 500)
        // Note: we only check for 500 - Internal Server Error. Other 5xx codes are possible, but they're not really
        // related to bug finding. Test cases that have other errors from the 5xx series will end up in the
        // "remainder" subset - as they are neither errors, nor successful runs.
    }

    /***
     * When the test suite is split into Successful and Failed tests, this function determines what a failed test
     * is defined as.
     * A test is a failure:
     *  - if it has a call with a status code 500
     *  - if it contains a GraphQL call with a response containing an "errors" field
     *  - IF [PartialOracles] are selected, if the test contains a call that fails an expectation
     *  (i.e. is selected for clustering by one of the partial oracles).
     */
    fun assessFailed(action: EvaluatedAction, oracles: PartialOracles?, config: EMConfig): Boolean{
        val codeSelect = when (action.result) {
            is GraphQlCallResult -> {
                assessFailed(action.result as GraphQlCallResult)
            }
            is HttpWsCallResult -> {
                return assessFailed(action.result as HttpWsCallResult)
            }
            else -> false
        }

        val oracleSelect = when{
            !config.expectationsActive -> false
            oracles!= null -> oracles.selectForClustering(action)
            else -> false
        }
            //config.expectationsActive == false -> false
            //else ->            oracles?.selectForClustering(action) ?: false

        return codeSelect || oracleSelect
    }

    /**
     * [splitByClusters] splits the Solution into several subsets based on the HTTP codes found in the actions.
     * The split is as follows:
     * - all individuals that count as failed go into a separate file. A failed call is likely
     * to be indicative of a fault, and therefore goes into a separate set.
     * This differs from [splitByCode] by also counting as failed those calls that fail the partial oracles as well
     * as those that have a 500 code.
     *
     * - all individuals that contain 2xx and 3xx action only are deemed to be successful, and a "successful" subset
     * is created for them. These are test cases that indicate no problem.
     *
     * - remaining test cases are set in a third subset. These are often test cases that don't contain outright bugs
     * (i.e. 500 actions) but may include 4xx. User errors and input problems may be interesting, hence the separate file.
     * Nevertheless, it is up to individual test engineers to look at these test cases in more depth and decide
     * if any further action or investigation is required.
     */
    private fun <T:Individual> splitByCluster(solution: Solution<T>,
                                              oracles: PartialOracles = PartialOracles(),
                                              config: EMConfig): List<Solution<T>>{
        val s500 = solution.individuals.filter {
            it.evaluatedMainActions().any { ac ->
                assessFailed(ac, oracles, config)

            }
        }.toMutableList()

        val successses = solution.individuals.filter {
            !s500.contains(it) &&
                    it.evaluatedMainActions().all { ac ->
                        val code = (ac.result as HttpWsCallResult).getStatusCode()
                        (code != null && code < 400)
                    }
        }.toMutableList()

        val remainder = solution.individuals.filter {
            !s500.contains(it) &&
                    !successses.contains(it)
        }.toMutableList()

        return listOf(Solution(
            s500,
            solution.testSuiteNamePrefix,
            solution.testSuiteNameSuffix,
            Termination.FAULTS,
            listOf(),
            listOf()
        ),
                Solution(
                    successses,
                    solution.testSuiteNamePrefix,
                    solution.testSuiteNameSuffix,
                    Termination.SUCCESSES,
                    listOf(),
                    listOf()
                ),
                Solution(
                    remainder,
                    solution.testSuiteNamePrefix,
                    solution.testSuiteNameSuffix,
                    Termination.OTHER,
                    listOf(),
                    listOf()
                )
        )
    }

    private fun formatTestedInterfacesInTestName(rpcIndividual: RPCIndividual) : String{
        return rpcIndividual.getTestedInterfaces().joinToString("_") { formatClassNameInTestName(it, true) }
    }

    private fun formatClassNameInTestName(clazz: String, simpleName : Boolean): String{
        val names = clazz.replace("$","_").split(".")
        if (simpleName)
            return names.last()
        return names.joinToString("_")
    }


    /**
     * [splitIntoClusters] splits a given Solution object into a List of several Solution objects, each
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

    private fun splitIntoClusters(solution: Solution<RestIndividual>,
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

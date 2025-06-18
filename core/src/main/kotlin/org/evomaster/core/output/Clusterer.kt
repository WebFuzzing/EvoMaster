package org.evomaster.core.output

import org.evomaster.core.output.clustering.metrics.DistanceMetricErrorText
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.Solution


/**
 * The [Clusterer] groups the individuals in the [Solution] by some specified distance metric.
 *
 * Currently [DistanceMetricErrorText] is implemented. This clusters [RestIndividual] objects based that contain some
 * kind of failed [RestCallResult] on the similarity of their error messages.
 *
 * Notes:
 * - [DistanceMetricErrorText] is an example of distance, but it is by no means the only example. Further distance metrics
 * will be implemented in future, as needed.
 *
 * - the [Clusterer] only looks at failed [HttpWsCallResult] objects and defines those as those that have a 500 code.
 * TODO: Refactor the method to show if the action reveals a fault in other ways.
 */
object Clusterer {
//    fun cluster(solution: Solution<ApiWsIndividual>,
//                config: EMConfig,
//                epsilon: Double = 0.6,
//                oracles: PartialOracles = PartialOracles(),
//                metric: DistanceMetric<HttpWsCallResult>
//    ): MutableList<MutableList<HttpWsCallResult>>{
//
//        /*
//        In order to be clustered, an individual must have at least one failed result.
//         */
//        val sol1 = solution.individuals.filter{
//            it.evaluatedMainActions().any{ ac ->
//                TestSuiteSplitter.assessFailed(ac, oracles, config)
//            }
//        }
//
//        /*
//        In order to "participate" in the clustering process, an action must be a RestCallResult
//        and it must be a failed result.
//         */
//
//        val clusterableActions = sol1.flatMap {
//            it.evaluatedMainActions().filter { ac ->
//                TestSuiteSplitter.assessFailed(ac, oracles, config)
//            }
//        }.map { ac -> ac.result }
//                .filterIsInstance<HttpWsCallResult>()
//
//        //TODO: Check the clusterableActions here.
//        // if clusterableActions are comprised of results that are not HttpWsCallResult.
//        // Currently it throws an exception. But what would be the expected behaviour? Presumably, as clustering is
//        // not guaranteed, return without clusters? or introduce the checks earlier?
//
//        //BMR: Could it be that clusterableACtions are null here (I mean... it could).
//        val clu = DBSCANClusterer<HttpWsCallResult>(
//                values = clusterableActions,
//                epsilon = epsilon,
//                minimumMembers = 2,
//                metric = metric
//        )
//
//        val clusters = clu.performCLustering()
//        clusters.forEachIndexed { index, clu ->
//            val inds = solution.individuals.filter { ind ->
//                ind.evaluatedMainActions().any { ac ->
//                    clu.contains(ac.result as HttpWsCallResult)
//                }
//            }.map {
//                it.assignToCluster("${metric.getName()}_$index")
//            }.toMutableSet()
//        }
//        return clusters
//    }

}
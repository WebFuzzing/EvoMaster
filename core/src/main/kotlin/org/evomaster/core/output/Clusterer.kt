package org.evomaster.core.output

import org.evomaster.core.output.clustering.DBSCANClusterer
import org.evomaster.core.output.clustering.metrics.DistanceMetric
import org.evomaster.core.output.clustering.metrics.DistanceMetricErrorText
import org.evomaster.core.output.clustering.metrics.DistanceMetricLastLine
import org.evomaster.core.output.PartialOracles
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
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
 * - the [Clusterer] only looks at failed [RestCallResult] objects and defines those as those that have a 500 code.
 * TODO: Refactor the method to show if the action reveals a fault in other ways.
 */
object Clusterer {
    fun cluster(solution: Solution<RestIndividual>,
                epsilon: Double = 0.6,
                oracles: PartialOracles = PartialOracles(),
                metric: DistanceMetric<RestCallResult>): MutableList<MutableList<RestCallResult>>{

        /*
        In order to be clustered, an individual must have at least one failed result.
         */
        val sol1 = solution.individuals.filter{
            it.evaluatedActions().any{ ac ->
                TestSuiteSplitter.assessFailed(ac, oracles)
            }
        }

        /*
        In order to "participate" in the clustering process, an action must be a RestCallResult
        and it must be a failed result.
         */

        val clusterableActions = sol1.flatMap {
            it.evaluatedActions().filter { ac ->
                TestSuiteSplitter.assessFailed(ac, oracles)
            }
        }.map { ac -> ac.result }
                .filterIsInstance<RestCallResult>()

        val clu = DBSCANClusterer<RestCallResult>(
                values = clusterableActions,
                epsilon = epsilon,
                minimumMembers = 2,
                metric = metric
        )

        val clusters = clu.performCLustering()
        clusters.forEachIndexed { index, clu ->
            val inds = solution.individuals.filter { ind ->
                ind.evaluatedActions().any { ac ->
                    clu.contains(ac.result as RestCallResult)
                }
            }.map {
                it.assignToCluster("${metric.getName()}_$index")
            }.toMutableSet()
        }
        return clusters
    }

}
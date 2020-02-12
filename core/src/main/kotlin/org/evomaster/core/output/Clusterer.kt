package org.evomaster.core.output

import org.evomaster.core.output.clustering.DBSCANClusterer
import org.evomaster.core.output.clustering.metrics.DistanceMetricAction
import org.evomaster.core.output.clustering.metrics.DistanceMetricLastLine
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution


/**
 * The [Clusterer] groups the individuals in the [Solution] by some specified distance metric.
 *
 * Currently [DistanceMetricAction] is implemented. This clusters [RestIndividual] objects based that contain some
 * kind of failed [RestCallResult] on the similarity of their error messages.
 *
 * Notes:
 * - [DistanceMetricAction] is an example of distance, but it is by no means the only example. Further distance metrics
 * will be implemented in future, as needed.
 *
 * - the [Clusterer] only looks at failed [RestCallResult] objects and defines those as those that have a 500 code.
 * TODO: Refactor the method to show if the action reveals a fault in other ways.
 */
object Clusterer {
    fun cluster(solution: Solution<RestIndividual>,
                epsilon: Double = 0.6,
                oracles: PartialOracles = PartialOracles()): MutableList<MutableList<RestCallResult>>{

        /*
        In order to be clustered, an individual must have at least one failed result.
         */
        val sol1 = solution.individuals.filter{
            it.evaluatedActions().any{ ac ->
                //val code = (ac.result as RestCallResult).getStatusCode()
                //(code != null && code == 500)
                TestSuiteSplitter.assessFailed(ac, oracles)
            }
        }

        /*
        In order to "participate" in the clustering process, an action must be a RestCallResult
        and it must be a failed result (i.e. a 500 call).
         */

        val clusterableActions_old = sol1.flatMap { it.evaluatedActions().map { ac-> ac.result } }
                .filterIsInstance<RestCallResult>()
                .filter { ac -> ac.getStatusCode() == 500 }

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
                metric = DistanceMetricAction()
        )

        val clu2 = DBSCANClusterer<RestCallResult>(
                values = clusterableActions,
                epsilon = 0.1,
                minimumMembers = 2,
                metric = DistanceMetricLastLine()
        )

        val clusters = clu.performCLustering()
        return clusters
    }

}
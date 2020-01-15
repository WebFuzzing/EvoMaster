package org.evomaster.core.output

import com.google.gson.Gson
import jsat.DataSet
import jsat.SimpleDataSet
import jsat.clustering.OPTICS
import jsat.linear.distancemetrics.DistanceMetric
import org.nield.kotlinstatistics.dbScanCluster
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.apache.commons.math3.ml.clustering.*
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.evomaster.core.output.clustering.DBSCANClusterer
import org.evomaster.core.output.clustering.metrics.DistanceMetricAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.GeneUtils

object Clusterer {

    //TODO: BMR - this is just here to run and evaluate the clusterer. Will be refactored to
    // account for more clustering options soon.

    fun cluster(solution: Solution<RestIndividual>,
                epsilon: Double = 0.6): MutableList<MutableList<RestCallResult>>{

        /*
        In order to be clustered, an individual must have at least one failed result.
         */
        val sol1 = solution.individuals.filter{
            it.evaluatedActions().any{ ac ->
                val code = (ac.result as RestCallResult).getStatusCode()
                if (code!=null) (code == 500)
                else false
            }
        }

        /*
        In order to "participate" in the clustering process, an action must be a RestCallResult
        and it must be a failed result (i.e. a 500 call).
         */
        val cluterableActions = sol1.flatMap {
            it.evaluatedActions()
                    .filter{ ac -> ac.result is RestCallResult }
                    .map { ac -> (ac.result as RestCallResult) }
                    .filter { ac -> ac.getStatusCode() == 500 }
        }

        val clu = DBSCANClusterer<RestCallResult>(
                values = cluterableActions,
                epsilon = epsilon,
                minimumMembers = 2,
                metric = DistanceMetricAction()
        )

        val clusters = clu.performCLustering()
        return clusters
    }
}
package org.evomaster.experiments.archiveMutation.stringProblem

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition

/**
 * created by manzh on 2019-09-16
 */

class StringFitness: FitnessFunction<StringIndividual>() {

    @Inject
    lateinit var sp : ArchiveProblemDefinition<StringIndividual>

    override fun doCalculateCoverage(individual: StringIndividual): EvaluatedIndividual<StringIndividual>? {
        val fv = FitnessValue(individual.size().toDouble())

        sp.distance(individual).forEach { (t, u) ->
            fv.updateTarget(t, u)
        }

        return EvaluatedIndividual(fv, individual.copy() as StringIndividual, listOf(), enableTracking = config.enableTrackEvaluatedIndividual, trackOperator = if(config.enableTrackEvaluatedIndividual) individual.trackOperator else null, enableImpact = (config.probOfArchiveMutation > 0.0))
    }
}
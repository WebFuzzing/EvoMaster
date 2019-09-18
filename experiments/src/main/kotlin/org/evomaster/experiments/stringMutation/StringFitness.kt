package org.evomaster.experiments.stringMutation

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction

/**
 * created by manzh on 2019-09-16
 */

class StringFitness: FitnessFunction<StringIndividual>() {

    @Inject
    lateinit var spd : StringProblemDefinition

    override fun doCalculateCoverage(individual: StringIndividual): EvaluatedIndividual<StringIndividual>? {
        val fv = FitnessValue(individual.size().toDouble())

        spd.distance(individual.genes).forEach { (t, u) ->
            fv.updateTarget(t, u)
        }

        return EvaluatedIndividual(fv, individual.copy() as StringIndividual, listOf(), enableTracking = config.enableTrackEvaluatedIndividual, trackOperator = if(config.enableTrackEvaluatedIndividual) individual.trackOperator else null, enableImpact = (config.probOfArchiveMutation > 0.0))
    }
}
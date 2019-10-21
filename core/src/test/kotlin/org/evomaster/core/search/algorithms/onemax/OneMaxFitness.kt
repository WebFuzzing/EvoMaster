package org.evomaster.core.search.algorithms.onemax

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.tracer.TraceableElementCopyFilter


class OneMaxFitness : FitnessFunction<OneMaxIndividual>() {

    @Inject
    private lateinit var sampler: OneMaxSampler

    override fun doCalculateCoverage(individual: OneMaxIndividual)
            : EvaluatedIndividual<OneMaxIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())

        (0 until individual.n)
                .forEach { fv.updateTarget(it, individual.getValue(it)) }

        return EvaluatedIndividual(fv, individual.copy(if(!config.enableTrackIndividual) TraceableElementCopyFilter.NONE else TraceableElementCopyFilter.WITH_TRACK) as OneMaxIndividual, listOf(), enableTracking = config.enableTrackEvaluatedIndividual, trackOperator = if(config.enableTrackEvaluatedIndividual) sampler else null, enableImpact = (config.probOfArchiveMutation > 0.0))
    }
}
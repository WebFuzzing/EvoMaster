package org.evomaster.experiments.maxv

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction


class LinearFitness: FitnessFunction<LinearIndividual>() {

    @Inject
    lateinit var lpd : LinearProblemDefinition


    override fun doCalculateCoverage(individual: LinearIndividual): EvaluatedIndividual<LinearIndividual> {

        val fv = FitnessValue()

        val id = individual.id.gene.value
        val k = individual.k.value

        val distance =  Math.abs(lpd.optima[id] - k)
        val h = 1.0 / (distance + 1.0)

        fv.updateTarget(id, h)

        return EvaluatedIndividual(fv, individual.copy() as LinearIndividual, listOf())
    }


}
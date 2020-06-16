package org.evomaster.experiments.linear

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction


class LinearFitness : FitnessFunction<LinearIndividual>() {

    @Inject
    lateinit var lpd: LinearProblemDefinition


    override fun doCalculateCoverage(individual: LinearIndividual): EvaluatedIndividual<LinearIndividual> {

        val fv = FitnessValue(individual.size().toDouble())

        val id = individual.id.gene.value

        if(id >=0 ) {

            val k = individual.k.value
            val optimum = lpd.optima[id]
            val range = individual.k.max

            val distance = when {
                k == optimum -> 0
                lpd.problemType == ProblemType.GRADIENT -> Math.abs(optimum - k)
                optimum > k -> optimum - k
                lpd.problemType == ProblemType.PLATEAU -> (0.1 * range).toInt()
                else -> 1 + range - k  //DECEPTIVE
            }

            val h = 1.0 / (distance + 1.0)

            fv.updateTarget(id, h)
        } else {
            fv.updateTarget(id, 0.5)
        }


        return EvaluatedIndividual(fv, individual.copy() as LinearIndividual, listOf())
    }


}
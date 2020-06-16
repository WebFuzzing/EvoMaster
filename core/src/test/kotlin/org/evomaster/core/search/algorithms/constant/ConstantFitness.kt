package org.evomaster.core.search.algorithms.constant

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantFitness : FitnessFunction<ConstantIndividual>() {


    override fun doCalculateCoverage(individual: ConstantIndividual): EvaluatedIndividual<ConstantIndividual>? {

        val target = 123
        val res = individual.gene.value

        val h = if (res == target) {
            1.0
        } else {
            val distance = Math.abs(target - res)
            1.0 - (distance / (1.0 + distance))
        }

        val fv = FitnessValue(individual.size().toDouble())
        fv.updateTarget(0, h)

        return EvaluatedIndividual(fv, individual.copy() as ConstantIndividual, listOf())
    }
}
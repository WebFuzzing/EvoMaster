package org.evomaster.core.search.impact.stringmatchproblem

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction

/**
 * created by manzh on 2020-06-16
 */
class StringMatchFitness : FitnessFunction<StringMatchIndividual>() {

    private val seedTargets = listOf("foo", "", "Y8l4x0WC9qtH5mTN")

    override fun doCalculateCoverage(individual: StringMatchIndividual, targets: Set<Int>): EvaluatedIndividual<StringMatchIndividual>? {

        val current = if (archive.isEmpty()) 0 else if (archive.notCoveredTargets().isNotEmpty()) archive.notCoveredTargets().first()  else
            null
        val value = individual.gene.value
        val fv = FitnessValue(individual.size().toDouble())

        if (current != null) {
            // simulate target.equals(value)
            val h = calculateHeuristic(seedTargets[current], value)

            fv.updateTarget(current, h)

            // reach next target until current is covered
            if (h == 1.0 && (current + 1) < seedTargets.size){
                fv.updateTarget(current+1, calculateHeuristic(seedTargets[current+1], value))
            }
        }

        return EvaluatedIndividual(fv, individual.copy() as StringMatchIndividual, listOf())
    }

    private fun calculateHeuristic(target: String, value : String) : Double{
        val distance = DistanceHelper.getLeftAlignmentDistance(target, value)
        return  (1.0 / (1.0 + distance))
    }
}



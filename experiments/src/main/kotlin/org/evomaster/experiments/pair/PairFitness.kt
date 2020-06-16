package org.evomaster.experiments.pair

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import java.util.stream.Stream


class PairFitness : FitnessFunction<PairIndividual>() {

    @Inject
    lateinit var ppd: PairProblemDefinition


    override fun doCalculateCoverage(individual: PairIndividual): EvaluatedIndividual<PairIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())

        val x = individual.x.value
        val y = individual.y.value

        var id = 0

        Stream.concat(
                ppd.optimaX.stream().map { Math.abs(it - x) },
                ppd.optimaY.stream().map { Math.abs(it - y) })
                .map { 1.0 / (it + 1.0) }
                .forEach { h -> fv.updateTarget(id++, h) }

        return EvaluatedIndividual(fv, individual.copy() as PairIndividual, listOf())
    }

}
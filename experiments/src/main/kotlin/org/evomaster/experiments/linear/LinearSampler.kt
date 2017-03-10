package org.evomaster.experiments.linear

import com.google.inject.Inject
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Sampler


class LinearSampler : Sampler<LinearIndividual>() {

    @Inject
    lateinit var lpd : LinearProblemDefinition


    override fun sampleAtRandom(): LinearIndividual {

        val ind = LinearIndividual(
                DisruptiveGene("id", IntegerGene("id_value",0, -lpd.infeasible, lpd.nTargets-1), lpd.disruptiveP),
                IntegerGene("k", 0, 0, lpd.range)
        )
        ind.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return ind
    }
}
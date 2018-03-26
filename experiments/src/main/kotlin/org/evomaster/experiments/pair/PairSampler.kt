package org.evomaster.experiments.pair

import com.google.inject.Inject
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Sampler


class PairSampler : Sampler<PairIndividual>(){

    @Inject
    lateinit var lpd : PairProblemDefinition


    override fun sampleAtRandom(): PairIndividual {

        val ind = PairIndividual(
                IntegerGene("x", 0, 0, lpd.range),
                IntegerGene("y", 0, 0, lpd.range)
        )
        ind.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return ind
    }
}
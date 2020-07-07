package org.evomaster.core.search.impact.stringmatchproblem

import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Sampler

/**
 * created by manzh on 2020-06-16
 */
class StringMatchSampler : Sampler<StringMatchIndividual>() {

    override fun sampleAtRandom(): StringMatchIndividual {
        val gene = StringGene(name = "value")
        gene.randomize(randomness, forceNewValue = false)
        return StringMatchIndividual(gene)
    }
}
package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.value.StringGeneImpact

/**
 * created by manzh on 2019-10-09
 */
class StringGeneImpactTest : GeneImpactTest() {
    override fun getGene(): Gene {
        return StringGene("s","hello")
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is StringGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as StringGene
        geneToMutate.apply {
            if (value.length + 1 > maxLength)
                value = value.dropLast(1)
            else
                value += "a"
        }

        return MutatedGeneWithContext(current = geneToMutate, previous = original)
    }

    //TODO for specialization
}
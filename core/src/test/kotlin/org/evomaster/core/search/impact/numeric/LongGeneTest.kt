package org.evomaster.core.search.impact.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.LongGeneImpact

/**
 * created by manzh on 2019-10-08
 */
class LongGeneTest : GeneImpactTest() {

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as LongGene

        if (geneToMutate.value + 1L > Long.MAX_VALUE)
            geneToMutate.value -= 1L
        else
            geneToMutate.value += 1L

        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    override fun getGene(): Gene = LongGene("i",  value= 1L)

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is LongGeneImpact)
    }

}
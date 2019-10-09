package org.evomaster.core.search.impact.numeric

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-10-08
 */
class IntegerGeneTest : GeneImpactTest() {

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as IntegerGene

        if (geneToMutate.value + 1 > Int.MAX_VALUE)
            geneToMutate.value -= 1
        else
            geneToMutate.value += 1

        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    override fun getGene(): Gene = IntegerGene("i", value = 2)

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is IntegerGeneImpact)
    }

}
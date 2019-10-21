package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.impact.value.DisruptiveGeneImpact

import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-09
 */
class DisruptiveGeneImpactTest : GeneImpactTest() {
    override fun getGene(): Gene {
        val gene = IntegerGene("gene", 0)
        return DisruptiveGene("o", gene = gene, probability =  0.9)
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is DisruptiveGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as DisruptiveGene<IntegerGene>
        val gene = geneToMutate.gene
        gene.value += if (gene.value + 1 > gene.max) -1 else 1
        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    @Test
    fun testInsideGene(){

        val gene = getGene()
        val impact = initImpact(gene)

        val pair = template(gene, impact, listOf(ImpactOptions.ONLY_IMPACT))

        impact as DisruptiveGeneImpact

        assertImpact(impact.geneImpact, (pair.second as DisruptiveGeneImpact).geneImpact, ImpactOptions.ONLY_IMPACT)

    }
}
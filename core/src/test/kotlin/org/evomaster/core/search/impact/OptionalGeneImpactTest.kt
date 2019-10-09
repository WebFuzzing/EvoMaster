package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.impact.value.OptionalGeneImpact
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-09
 */
class OptionalGeneImpactTest : GeneImpactTest() {
    override fun getGene(): Gene {
        val gene = IntegerGene("gene", 0)
        return OptionalGene("o", isActive = true, gene = gene)
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is OptionalGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as OptionalGene
        geneToMutate.apply {
            when{
                mutationTag == 0 -> (geneToMutate.gene as IntegerGene).value += 1
                mutationTag == 1 -> geneToMutate.isActive = !geneToMutate.isActive
                else -> throw IllegalArgumentException("bug")
            }
        }

        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    @Test
    fun testActiveAndGene(){
        // true -> false
        val gene = getGene()
        val impact = initImpact(gene)

        val pair = template(gene, impact, listOf(ImpactOptions.ONLY_IMPACT), 1)

        impact as OptionalGeneImpact
        assertImpact(impact.activeImpact, (pair.second as OptionalGeneImpact).activeImpact, ImpactOptions.ONLY_IMPACT)
        assertImpact(impact.activeImpact._false, (pair.second as OptionalGeneImpact).activeImpact._false, ImpactOptions.ONLY_IMPACT)
        assertImpact(impact.activeImpact._true, (pair.second as OptionalGeneImpact).activeImpact._true, ImpactOptions.NONE)

        assertImpact(impact.geneImpact, (pair.second as OptionalGeneImpact).geneImpact, ImpactOptions.NONE)

    }
}
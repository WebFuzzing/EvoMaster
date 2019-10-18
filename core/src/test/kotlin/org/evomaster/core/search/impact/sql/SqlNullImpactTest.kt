package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.ImpactOptions
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-09
 */
class SqlNullImpactTest : GeneImpactTest() {
    override fun getGene(): Gene {
        val gene = IntegerGene("gene", 0)
        return SqlNullable("o", isPresent = false, gene = gene)
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is SqlNullableImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as SqlNullable
        geneToMutate.apply {
            when{
                mutationTag == 0 -> (geneToMutate.gene as IntegerGene).value += 1
                mutationTag == 1 -> geneToMutate.isPresent = !geneToMutate.isPresent
                else -> throw IllegalArgumentException("bug")
            }
        }

        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    @Test
    fun testActiveAndGene(){

        val gene = getGene()
        val impact = initImpact(gene)

        // false -> true
        val pair = template(gene, impact, listOf(ImpactOptions.ONLY_IMPACT), 1)

        impact as SqlNullableImpact
        assertImpact(impact.presentImpact, (pair.second as SqlNullableImpact).presentImpact, ImpactOptions.ONLY_IMPACT)
        assertImpact(impact.presentImpact.falseValue, (pair.second as SqlNullableImpact).presentImpact.falseValue, ImpactOptions.NONE)
        assertImpact(impact.presentImpact.trueValue, (pair.second as SqlNullableImpact).presentImpact.trueValue, ImpactOptions.ONLY_IMPACT)

        assertImpact(impact.geneImpact, (pair.second as SqlNullableImpact).geneImpact, ImpactOptions.NONE)

        // mutate inside gene of optional gene.
        val pairG = template(pair.first, pair.second, listOf(ImpactOptions.IMPACT_IMPROVEMENT), 0)

        assertImpact((pair.second as SqlNullableImpact).presentImpact, (pairG.second as SqlNullableImpact).presentImpact, ImpactOptions.NONE)
        assertImpact((pair.second as SqlNullableImpact).presentImpact.falseValue, (pairG.second as SqlNullableImpact).presentImpact.falseValue, ImpactOptions.NONE)
        assertImpact((pair.second as SqlNullableImpact).presentImpact.trueValue, (pairG.second as SqlNullableImpact).presentImpact.trueValue, ImpactOptions.NONE)

        assertImpact((pair.second as SqlNullableImpact).geneImpact, (pairG.second as SqlNullableImpact).geneImpact, ImpactOptions.IMPACT_IMPROVEMENT)
    }
}
package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.ImpactOptions
import org.evomaster.core.search.impact.MutatedGeneWithContext

import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-09
 */
class SqlPrimaryGeneImpactTest : GeneImpactTest() {
    override fun getGene(): Gene {
        val gene = IntegerGene("gene", 0)
        return SqlPrimaryKeyGene("primaryKey", tableName = "fake",gene = gene, uniqueId = 1L)
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is SqlPrimaryKeyGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as SqlPrimaryKeyGene
        val gene = geneToMutate.gene as IntegerGene
        gene.value += if (gene.value + 1 > gene.max) -1 else 1
        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    @Test
    fun testInsideGene(){

        val gene = getGene()
        val impact = initImpact(gene)

        val pair = template(gene, impact, listOf(ImpactOptions.ONLY_IMPACT))

        impact as SqlPrimaryKeyGeneImpact

        assertImpact(impact.geneImpact, (pair.second as SqlPrimaryKeyGeneImpact).geneImpact, ImpactOptions.ONLY_IMPACT)

    }
}
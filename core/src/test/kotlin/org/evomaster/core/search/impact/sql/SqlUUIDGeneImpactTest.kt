package org.evomaster.core.search.impact.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlUUIDGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.ImpactOptions
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * created by manzh on 2019-10-09
 */
class SqlUUIDGeneImpactTest : GeneImpactTest() {

    override fun getGene(): Gene {

        return SqlUUIDGene("uuid")
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is SqlUUIDGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as SqlUUIDGene
        val p = Random.nextBoolean()
        geneToMutate.apply {
            when{
                mutationTag == 1 || (mutationTag == 0 && p)->  mostSigBits.value += if (mostSigBits.value+1 > Long.MAX_VALUE) -1 else 1
                mutationTag == 2 || (mutationTag == 0 && !p) -> leastSigBits.value += if (leastSigBits.value+1 > Long.MAX_VALUE) -1 else 1
                else -> throw IllegalArgumentException("bug")
            }
        }

        return MutatedGeneWithContext(previous = original, current = geneToMutate)
    }

    @Test
    fun testDate(){
        var gene = getGene()
        var impact = initImpact(gene)
        val pairM = template( gene, impact, listOf(ImpactOptions.IMPACT_IMPROVEMENT), 1)

        impact as SqlUUIDGeneImpact

        var updatedImpact = pairM.second as SqlUUIDGeneImpact

        assertImpact(impact, updatedImpact, ImpactOptions.IMPACT_IMPROVEMENT)
        assertImpact(impact.mostSigBitsImpact, updatedImpact.mostSigBitsImpact, ImpactOptions.IMPACT_IMPROVEMENT)
        assertImpact(impact.leastSigBitsImpact, updatedImpact.leastSigBitsImpact, ImpactOptions.NONE)

        impact = updatedImpact
        gene = pairM.first
        val pairL = template(gene, impact, listOf(ImpactOptions.ONLY_IMPACT), 2)

        updatedImpact = pairL.second as SqlUUIDGeneImpact

        assertImpact(impact, updatedImpact, ImpactOptions.ONLY_IMPACT)
        assertImpact(impact.mostSigBitsImpact, updatedImpact.mostSigBitsImpact, ImpactOptions.NONE)
        assertImpact(impact.leastSigBitsImpact, updatedImpact.leastSigBitsImpact, ImpactOptions.ONLY_IMPACT)
    }


}
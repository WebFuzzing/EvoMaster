package org.evomaster.core.search.impact.collection

import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneImpactTest
import org.evomaster.core.search.impact.ImpactOptions
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.collection.ArrayGeneImpact
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * created by manzh on 2019-10-10
 */
class ArrayGeneImpactTest : GeneImpactTest(){

    val map = listOf(1,2,3).map { IntegerGene(it.toString(), value = it) }.toMutableList()
    var counter = 3

    fun generateKey() :Int{
        counter += 1
        return counter
    }
    override fun getGene(): Gene {
        return ArrayGene("map", template = map.first(), elements = map)
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is ArrayGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as ArrayGene<IntegerGene>

        val p = Random.nextBoolean()

        when{
            mutationTag == 1 || (mutationTag == 0 && p)->{
                val index = Random.nextInt(0, geneToMutate.elements.size)
                geneToMutate.elements[index].apply {
                    value += if (value + 1> max) -1 else 1
                }
            }
            mutationTag == 2 || (mutationTag == 0 && !p)->{
                if (geneToMutate.elements.size + 1 > geneToMutate.maxSize)
                    geneToMutate.elements.removeAt(0)
                else{
                    val key = generateKey()
                    geneToMutate.elements.add(IntegerGene(key.toString(), key))
                }
            }
        }
        return MutatedGeneWithContext(current = geneToMutate, previous = original)
    }

    @Test
    fun testSize(){
        val gene = getGene()
        val impact = initImpact(gene)
        val pair = template(gene, impact, listOf(ImpactOptions.NO_IMPACT), 1)

        assertImpact(impact, pair.second, ImpactOptions.NO_IMPACT)
        assertImpact((impact as ArrayGeneImpact).sizeImpact, (pair.second as ArrayGeneImpact).sizeImpact, ImpactOptions.NONE)

        val pairS = template(pair.first, pair.second, listOf(ImpactOptions.IMPACT_IMPROVEMENT), 2)
        assertImpact(pair.second, pairS.second, ImpactOptions.IMPACT_IMPROVEMENT)
        assertImpact((pair.second as ArrayGeneImpact).sizeImpact, (pairS.second as ArrayGeneImpact).sizeImpact, ImpactOptions.IMPACT_IMPROVEMENT)
    }
}
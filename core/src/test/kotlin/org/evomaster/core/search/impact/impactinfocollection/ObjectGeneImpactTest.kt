package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-10
 */
class ObjectGeneImpactTest : GeneImpactTest() {

    val f1 = "f1"
    val f2 = "f2"

    override fun getGene(): Gene {
        val f1 = StringGene(f1, "string1")
        val f2 = IntegerGene(f2, 2)

        return ObjectGene("obj", listOf(f1, f2))
    }

    override fun checkImpactType(impact: GeneImpact) {
        assert(impact is ObjectGeneImpact)
    }

    override fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int): MutatedGeneWithContext {
        geneToMutate as ObjectGene

        assert(geneToMutate.fields.first() is StringGene)
        val f1 = geneToMutate.fields.first() as StringGene
        assert(geneToMutate.fields.last() is IntegerGene)
        val f2 = geneToMutate.fields.last() as IntegerGene

        val p = Math.random() < 0.5

        when{
            mutationTag == 1 || (mutationTag == 0 && p) -> {
                if (f1.value.length + 1 > f1.maxLength){
                    f1.value = f1.value.dropLast(1)
                }else
                    f1.value = f1.value + "a"
            }
            mutationTag == 2 || (mutationTag == 0 && !p)-> {
                if (f2.value + 1 > f2.getMaximum())
                    f2.value -= 1
                else
                    f2.value += 1
            }
            else -> TODO()
        }

        return MutatedGeneWithContext(current = geneToMutate, previous = original,)
    }

    @Test
    fun testfields(){
        val gene = getGene()
        var impact = initImpact(gene) as ObjectGeneImpact

        val pairf1 = template(gene, impact, listOf(ImpactOptions.IMPACT_IMPROVEMENT), 1)

        var updateImpact = pairf1.second as ObjectGeneImpact

        assertImpact(impact, updateImpact, ImpactOptions.IMPACT_IMPROVEMENT)
        assertImpact(impact.fixedFields.getValue(f1), updateImpact.fixedFields.getValue(f1), ImpactOptions.IMPACT_IMPROVEMENT)
        assertImpact(impact.fixedFields.getValue(f2), updateImpact.fixedFields.getValue(f2), ImpactOptions.NONE)

        impact = updateImpact

        val pairf2 = template(gene, impact, listOf(ImpactOptions.NO_IMPACT), 2)
        updateImpact = pairf2.second as ObjectGeneImpact

        assertImpact(impact, updateImpact, ImpactOptions.NO_IMPACT)
        assertImpact(impact.fixedFields.getValue(f1), updateImpact.fixedFields.getValue(f1), ImpactOptions.NONE)
        assertImpact(impact.fixedFields.getValue(f2), updateImpact.fixedFields.getValue(f2), ImpactOptions.NO_IMPACT)

    }

    @Test
    fun testHeterogeneousAndMissingFieldImpactsAreSafelySkipped() {
        val prev = ObjectGene("obj", listOf(StringGene(f1, "s1"), IntegerGene(f2, 2)))
        val impact = initImpact(prev) as ObjectGeneImpact
        val curr = ObjectGene("obj", listOf(IntegerGene(f1, 99), IntegerGene(f2, 3), StringGene("unknown", "x")))

        impact.countImpactWithMutatedGeneWithContext(
            gc = MutatedGeneWithContext(current = curr, previous = prev),
            noImpactTargets = setOf(),
            impactTargets = setOf(1),
            improvedTargets = setOf(1),
            onlyManipulation = false
        )

        assertEquals(0, impact.fixedFields.getValue(f1).getTimesToManipulate())
        assertEquals(0.0, impact.fixedFields.getValue(f1).getTimesOfImpacts()[1] ?: 0.0)
        assertEquals(1, impact.fixedFields.getValue(f2).getTimesToManipulate())

        val validOnlyF2 = ObjectGene("obj", listOf(StringGene(f1, "s1"), IntegerGene(f2, 4)))
        impact.countImpactWithMutatedGeneWithContext(
            gc = MutatedGeneWithContext(current = validOnlyF2, previous = prev),
            noImpactTargets = setOf(),
            impactTargets = setOf(1),
            improvedTargets = setOf(1),
            onlyManipulation = false
        )
        assertEquals(1.0, impact.fixedFields.getValue(f2).getTimesOfImpacts()[1])
    }
}

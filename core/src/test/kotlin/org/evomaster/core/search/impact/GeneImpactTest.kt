package org.evomaster.core.search.impact

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.evomaster.core.search.gene.Gene
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-08
 */
abstract class GeneImpactTest {

    private val fakeImpactTargets = setOf(1, 2)
    private val fakeImprovedTarget = setOf(1)

    @Test
    fun testWithoutImpact(){
        template(getGene(), initImpact(), listOf(ImpactOptions.NO_IMPACT))
    }

    @Test
    fun testWithImpact(){
        template(getGene(), initImpact(), listOf(ImpactOptions.ONLY_IMPACT))
    }

    @Test
    fun testWithImpactImprovement(){
        template(getGene(), initImpact(), listOf(ImpactOptions.IMPACT_IMPROVEMENT))
    }

    @Test
    fun test(){
        template(getGene(), initImpact(), listOf(ImpactOptions.IMPACT_IMPROVEMENT, ImpactOptions.IMPACT_IMPROVEMENT, ImpactOptions.ONLY_IMPACT, ImpactOptions.NO_IMPACT))
    }

    abstract fun getGene() : Gene

    abstract fun checkImpactType(impact: GeneImpact)

    /**
     * @param mutationTag configures different mutations
     */
    abstract fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int = 0) : MutatedGeneWithContext

    /**
     * @param mutationTag indicates how the gene is mutated
     */
    fun template(original: Gene, originalImpact: Impact, sequence: List<ImpactOptions>, mutationTag: Int = 0) : Pair<Gene, Impact>{
        var gene = original
        var impact = originalImpact

        sequence.forEach { option->
            val mutated = gene.copy()
            val gc = simulateMutation(gene, mutated, mutationTag)

            val updateImpact = impact.copy() as GeneImpact
            countImpact(updateImpact, gc, option)

            assertImpact(impact, updateImpact, option)

            gene = mutated
            impact = updateImpact
        }
        return Pair(gene, impact)
    }

    fun initImpact(g : Gene? = null): GeneImpact{
        val gene = g?:getGene()
        val id = ImpactUtils.generateGeneId(gene)
        val impact = ImpactUtils.createGeneImpact(gene, id)
        checkImpactType(impact)
        return  impact
    }

    fun countImpact(impact: GeneImpact, gc : MutatedGeneWithContext, option : ImpactOptions){
        when(option){
            ImpactOptions.NO_IMPACT -> impact.countImpactWithMutatedGeneWithContext(gc, setOf(), setOf(), false)
            ImpactOptions.ONLY_IMPACT -> impact.countImpactWithMutatedGeneWithContext(gc, impactTargets = fakeImpactTargets, improvedTargets = setOf(), onlyManipulation = false)
            ImpactOptions.IMPACT_IMPROVEMENT-> impact.countImpactWithMutatedGeneWithContext(gc, impactTargets = fakeImpactTargets, improvedTargets = fakeImprovedTarget, onlyManipulation = false)
        }
    }

    fun assertImpact(previous: Impact, impact: Impact, option: ImpactOptions){
        impact.apply {
            when(option){
                ImpactOptions.NO_IMPACT->{
                    assertEquals(previous.timesToManipulate + 1, timesToManipulate)

                    assertEquals(previous.timesOfNoImpacts + 1, timesOfNoImpacts)
                    fakeImpactTargets.forEach { t->
                        val expectedImpact = previous.timesOfImpact[t]?:0
                        assertEquals(expectedImpact, timesOfImpact[t]?:0)

                        val expectedNoImpact = (previous.noImpactFromImpact[t]?:-1) + 1
                        assertEquals(expectedNoImpact , noImpactFromImpact[t]?:0)

                        val expected = (previous.noImprovement[t]?:-1) + 1
                        assertEquals(expected, noImprovement[t]?:0)
                    }
                }

                ImpactOptions.ONLY_IMPACT->{
                    assertEquals(previous.timesToManipulate + 1, timesToManipulate)

                    assertEquals(previous.timesOfNoImpacts, timesOfNoImpacts)

                    fakeImpactTargets.forEach { t->
                        assertNotNull(timesOfImpact[t])
                        val expectedImpact = previous.timesOfImpact[t]?:0
                        assertEquals(expectedImpact + 1, timesOfImpact[t])

                        assertNotNull(noImpactFromImpact[t])
                        assertEquals(0, noImpactFromImpact[t])

                        assertNotNull(noImprovement[t])
                        val expected = previous.noImprovement[t]?:0
                        assertEquals(expected + 1, noImprovement[t])
                    }


                }

                ImpactOptions.IMPACT_IMPROVEMENT->{
                    assertEquals(previous.timesToManipulate + 1, timesToManipulate)

                    assertEquals(previous.timesOfNoImpacts, timesOfNoImpacts)

                    fakeImpactTargets.forEach { t->
                        assertNotNull(timesOfImpact[t])
                        val expectedImpact = previous.timesOfImpact[t]?:0
                        assertEquals(expectedImpact + 1, timesOfImpact[t])
                        assertNotNull(noImpactFromImpact[t])
                        assertEquals(0, noImpactFromImpact[t])

                        if (!fakeImprovedTarget.contains(t)){
                            assertNotNull(noImprovement[t])
                            val expected = previous.noImprovement[t]?:0
                            assertEquals(expected + 1, noImprovement[t])
                        }

                    }

                    fakeImprovedTarget.forEach { t->
                        assertNotNull(noImprovement[t])
                        assertEquals(0, noImprovement[t])
                    }

                }

                ImpactOptions.NONE->{
                    assertEquals(previous.timesToManipulate, timesToManipulate)

                    assertEquals(previous.timesOfNoImpacts, timesOfNoImpacts)

                    assert(previous.timesOfImpact == timesOfImpact)
                    assert(previous.noImpactFromImpact == noImpactFromImpact)
                    assert(previous.noImprovement == noImprovement)
                }
            }
        }
    }
}

enum class ImpactOptions{
    NO_IMPACT,
    ONLY_IMPACT,
    IMPACT_IMPROVEMENT,
    NONE
}
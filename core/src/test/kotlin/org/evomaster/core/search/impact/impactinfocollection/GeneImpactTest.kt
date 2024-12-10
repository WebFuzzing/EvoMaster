package org.evomaster.core.search.impact.impactinfocollection

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-10-08
 */
abstract class GeneImpactTest {

    private val allTargets = setOf(1,2,3)
    private val fakeImpactTargets = setOf(1,2)
    private val fakeImprovedTarget = setOf(1)

    fun getNoImpactTarget(impacts: Set<Int>) = allTargets.filter { impacts.contains(it) }.toSet()

    @Test
    fun testWithoutImpact(){
        arrayOf(false, true).forEach {
            template(getInitializedGene(), initImpact(), listOf(ImpactOptions.NO_IMPACT), doesDeepCopy = it)
        }
    }

    @Test
    fun testWithImpact(){
        arrayOf(false, true).forEach {
            template(getInitializedGene(), initImpact(), listOf(ImpactOptions.ONLY_IMPACT), doesDeepCopy = it)
        }
    }

    @Test
    fun testWithImpactImprovement(){
        arrayOf(false, true).forEach {
            template(getInitializedGene(), initImpact(), listOf(ImpactOptions.IMPACT_IMPROVEMENT), doesDeepCopy = it)
        }
    }

    @Test
    fun test(){
        arrayOf(false, true).forEach {
            template(getInitializedGene(), initImpact(), listOf(ImpactOptions.IMPACT_IMPROVEMENT, ImpactOptions.IMPACT_IMPROVEMENT, ImpactOptions.ONLY_IMPACT, ImpactOptions.NO_IMPACT), doesDeepCopy = it)
        }
    }

    private fun getInitializedGene() = getGene().apply { doInitialize(Randomness().apply { updateSeed(42) }) }
    abstract fun getGene() : Gene

    abstract fun checkImpactType(impact: GeneImpact)

    /**
     * @param mutationTag configures different mutations
     */
    abstract fun simulateMutation(original: Gene, geneToMutate: Gene, mutationTag: Int = 0) : MutatedGeneWithContext

    /**
     * @param mutationTag indicates how the gene is mutated
     */
    fun template(original: Gene, originalImpact: Impact, sequence: List<ImpactOptions>, mutationTag: Int = 0, doesDeepCopy : Boolean = false) : Pair<Gene, Impact>{
        var gene = original
        var impact = originalImpact

        sequence.forEach { option->
            val mutated =  gene.copy()
            val gc = simulateMutation(gene, mutated, mutationTag)

            val updateImpact = (if (doesDeepCopy) impact.copy() else impact.clone()) as GeneImpact
            countImpact(updateImpact, gc, option)

            assertImpact(impact, updateImpact, option, doesDeepCopy)

            gene = mutated
            impact = updateImpact
        }
        return Pair(gene, impact)
    }

    fun initImpact(g : Gene? = null): GeneImpact{
        val gene = g?:getInitializedGene()
        val id = ImpactUtils.generateGeneId(gene)
        val impact = ImpactUtils.createGeneImpact(gene, id)
        checkImpactType(impact)
        return  impact
    }

    private fun countImpact(impact: GeneImpact, gc : MutatedGeneWithContext, option : ImpactOptions){
        when(option){
            ImpactOptions.NO_IMPACT -> impact.countImpactWithMutatedGeneWithContext(gc, noImpactTargets = getNoImpactTarget(setOf()), impactTargets = setOf(), improvedTargets =  setOf(), onlyManipulation = false)
            ImpactOptions.ONLY_IMPACT -> impact.countImpactWithMutatedGeneWithContext(gc, noImpactTargets = getNoImpactTarget(fakeImpactTargets), impactTargets = fakeImpactTargets, improvedTargets = setOf(), onlyManipulation = false)
            ImpactOptions.IMPACT_IMPROVEMENT-> impact.countImpactWithMutatedGeneWithContext(gc, noImpactTargets = getNoImpactTarget(fakeImpactTargets), impactTargets = fakeImpactTargets, improvedTargets = fakeImprovedTarget, onlyManipulation = false)
            else ->{}
        }
    }

    fun assertImpact(previous: Impact, impact: Impact, option: ImpactOptions, doesDeepCopy: Boolean = false){
        impact.apply {
            when(option){
                ImpactOptions.NO_IMPACT->{
                    //shared
                    if (doesDeepCopy){
                        assertEquals(previous.getTimesToManipulate() + 1, getTimesToManipulate())
                        assertEquals(previous.getTimesOfNoImpact() + 1, getTimesOfNoImpact())
                    }else{
                        assertEquals(previous.getTimesToManipulate(), getTimesToManipulate())
                        assertEquals(previous.getTimesOfNoImpact(), getTimesOfNoImpact())
                    }

                    fakeImpactTargets.forEach { t->
                        val expectedImpact = previous.getTimesOfImpacts()[t]?:0
                        assertEquals(expectedImpact, getTimesOfImpacts()[t]?:0)


                        val expectedNoImpact = (previous.getNoImpactsFromImpactCounter()[t]?:-1.0) + 1
                        assertEquals(expectedNoImpact , getNoImpactsFromImpactCounter()[t]?:0.0)

                        val expected = (previous.getNoImprovementCounter()[t]?:-1.0) + 1
                        assertEquals(expected, getNoImprovementCounter()[t]?:0.0)
                    }
                }

                ImpactOptions.ONLY_IMPACT->{
                    if (doesDeepCopy)
                        assertEquals(previous.getTimesToManipulate() + 1, getTimesToManipulate())
                    else
                        assertEquals(previous.getTimesToManipulate(), getTimesToManipulate())

                    assertEquals(previous.getTimesOfNoImpact(), getTimesOfNoImpact())

                    fakeImpactTargets.forEach { t->
                        assertNotNull(getTimesOfImpacts()[t])
                        val expectedImpact = previous.getTimesOfImpacts()[t]?:0.0
                        if (doesDeepCopy)
                             assertEquals(expectedImpact + 1, getTimesOfImpacts()[t])
                        else
                            assertEquals(expectedImpact, getTimesOfImpacts()[t])


                        assertNotNull(getNoImpactsFromImpactCounter()[t])
                        assertEquals(0.0, getNoImpactsFromImpactCounter()[t])

                        assertNotNull(getNoImprovementCounter()[t])
                        val expected = previous.getNoImprovementCounter()[t]?:0.0
                        assertEquals(expected + 1, getNoImprovementCounter()[t])
                    }


                }

                ImpactOptions.IMPACT_IMPROVEMENT->{
                    if (doesDeepCopy)
                        assertEquals(previous.getTimesToManipulate() + 1, getTimesToManipulate())
                    else
                        assertEquals(previous.getTimesToManipulate(), getTimesToManipulate())

                    assertEquals(previous.getTimesOfNoImpact(), getTimesOfNoImpact())

                    fakeImpactTargets.forEach { t->
                        assertNotNull(getTimesOfImpacts()[t])
                        val expectedImpact = previous.getTimesOfImpacts()[t]?:0.0
                        if (doesDeepCopy)
                            assertEquals(expectedImpact + 1, getTimesOfImpacts()[t])
                        else
                            assertEquals(expectedImpact, getTimesOfImpacts()[t])

                        assertNotNull(getNoImpactsFromImpactCounter()[t])
                        assertEquals(0.0, getNoImpactsFromImpactCounter()[t])

                        if (!fakeImprovedTarget.contains(t)){
                            assertNotNull(getNoImprovementCounter()[t])
                            val expected = previous.getNoImprovementCounter()[t]?:0.0
                            assertEquals(expected + 1, getNoImprovementCounter()[t])
                        }

                    }

                    fakeImprovedTarget.forEach { t->
                        assertNotNull(getNoImprovementCounter()[t])
                        assertEquals(0.0, getNoImprovementCounter()[t])
                    }

                }

                ImpactOptions.NONE->{
                    assertEquals(previous.getTimesToManipulate(), getTimesToManipulate())

                    assertEquals(previous.getTimesOfNoImpact(), getTimesOfNoImpact())

                    assert(previous.getTimesOfImpacts() == getTimesOfImpacts())
                    assert(previous.getNoImpactsFromImpactCounter() == getNoImpactsFromImpactCounter())
                    assert(previous.getNoImprovementCounter() == getNoImprovementCounter())
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
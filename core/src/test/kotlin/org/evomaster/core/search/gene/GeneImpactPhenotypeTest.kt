package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GeneImpactPhenotypeTest : AbstractGeneTest() {

    @ParameterizedTest
    @ValueSource(longs = [1,2,3,4,5,6,7,8,9,10])
    fun testImpactPhenotype(seed: Long) {

        val rand = Randomness()
        rand.updateSeed(seed)

        val sample = getSample(seed)

        sample.filter { it.isPrintable() }
            .forEach { root ->

            val before = try{
                root.getValueAsRawString()
            } catch (e: Exception){
                //few genes are constraints, that seems not necessarily satisfied by sampler
                return@forEach
            }
            root.flatView()
                .filter { !it.staticCheckIfImpactPhenotype() }
                .filter { it.isMutable() }
                .forEach { it.randomize(rand, true) }
            val after = root.getValueAsRawString()
            //if no impact genes are modified, still should be no change in phenotype
            assertEquals(before, after)
        }
    }
}
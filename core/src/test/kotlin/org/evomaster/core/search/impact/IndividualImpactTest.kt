package org.evomaster.core.search.impact

import org.evomaster.core.TestUtils
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * check init and update impact of individual
 */
class IndividualImpactTest {


    @Test
    fun testRestIndividualImpactInit(){

        val ind = TestUtils.generateFakeSimpleRestIndividual()

        val impactInfo = ImpactsOfIndividual(ind, false, null)

        impactInfo.fixedMainActionImpacts.apply {
            assertEquals(2, size)
            assertEquals(2, this[0].geneImpacts.size)
            assertEquals(1, this[1].geneImpacts.size)
        }
    }
}

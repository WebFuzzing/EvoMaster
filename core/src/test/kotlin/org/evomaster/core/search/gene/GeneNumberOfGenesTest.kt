package org.evomaster.core.search.gene


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class GeneNumberOfGenesTest : AbstractGeneTest() {

    @Test
    fun testNumberOfGenes() {
        /*
            This number should not change, unless you explicitly add/remove any gene.
            if so, update this number accordingly
         */
        assertEquals(84, geneClasses.size)
    }

}

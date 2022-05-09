package org.evomaster.core.search.gene.sql.geometric

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlPathGeneTest {

    @Test
    fun testClosePath() {
        val p1 = SqlPointGene("p1")
        p1.x.value = 0.0f
        p1.y.value = 0.1f

        val p2 = SqlPointGene("p2")
        p2.x.value = 0.1f
        p2.y.value = 0.0f


        val gene = SqlPathGene("gene")
        gene.isClosedPath.value = true
        gene.points.addElement(p1)
        gene.points.addElement(p2)

        assertEquals("\"((0.0,0.1),(0.1,0.0))\"", gene.getValueAsPrintableString().replace(" ",""))
    }

    @Test
    fun testOpenPath() {
        val p1 = SqlPointGene("p1")
        p1.x.value = 0.0f
        p1.y.value = 0.1f

        val p2 = SqlPointGene("p2")
        p2.x.value = 0.1f
        p2.y.value = 0.0f


        val gene = SqlPathGene("gene")
        gene.isClosedPath.value = false
        gene.points.addElement(p1)
        gene.points.addElement(p2)

        assertEquals("\"[(0.0,0.1),(0.1,0.0)]\"", gene.getValueAsPrintableString().replace(" ",""))
    }
}
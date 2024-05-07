package org.evomaster.core.search.structuralelement

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StructuralElementTest {


    @Test
    fun testFirstParent(){

        val a = StringGene("a")
        val b = OptionalGene("b", a)
        val c = OptionalGene("c", b)
        val d = OptionalGene("d", c)
        val e = NullableGene("e",d)
        val f = OptionalGene("f",e)
        val g = CustomMutationRateGene("g", f,0.5)

        assertNull(a.getFirstParent(IntegerGene::class.java))
        assertEquals("g", a.getFirstParent(CustomMutationRateGene::class.java)!!.name)
        assertEquals("b", a.getFirstParent(OptionalGene::class.java)!!.name)
        assertEquals("c", b.getFirstParent(OptionalGene::class.java)!!.name)
        assertEquals("f", d.getFirstParent(OptionalGene::class.java)!!.name)
    }

}
package org.evomaster.core.search.structuralelement

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.wrapper.CustomMutationRateGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
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

    @Test
    fun testKillAllChildrenDoesNotCallWinstonWolfeOnParent() {
        var parentWinstonWolfeCalled = false
        var childWinstonWolfeCalled = false

        val child = object : org.evomaster.core.search.StructuralElement() {
            override fun callWinstonWolfe() {
                super.callWinstonWolfe()
                childWinstonWolfeCalled = true
            }
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }

        val parent = object : org.evomaster.core.search.StructuralElement(children = mutableListOf(child)) {
            override fun callWinstonWolfe() {
                super.callWinstonWolfe()
                parentWinstonWolfeCalled = true
            }
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }

        parent.killAllChildren()

        org.junit.jupiter.api.Assertions.assertTrue(childWinstonWolfeCalled)
        org.junit.jupiter.api.Assertions.assertFalse(parentWinstonWolfeCalled)
        org.junit.jupiter.api.Assertions.assertTrue(parent.getViewOfChildren().isEmpty())
        assertNull(child.parent)
    }

    @Test
    fun testKillAllChildrenSemanticallyEquivalentToKillChildren() {
        val child1 = object : org.evomaster.core.search.StructuralElement() {
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }
        val child2 = object : org.evomaster.core.search.StructuralElement() {
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }
        val parent1 = object : org.evomaster.core.search.StructuralElement(children = mutableListOf(child1)) {
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }
        val parent2 = object : org.evomaster.core.search.StructuralElement(children = mutableListOf(child2)) {
            override fun copyContent(): org.evomaster.core.search.StructuralElement = throw UnsupportedOperationException()
        }

        parent1.killAllChildren()
        parent2.killChildren(parent2.getViewOfChildren().toList())

        assertEquals(parent1.getViewOfChildren().size, parent2.getViewOfChildren().size)
        assertNull(child1.parent)
        assertNull(child2.parent)
    }

}

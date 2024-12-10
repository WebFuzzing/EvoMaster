package org.evomaster.core.search.structuralelement.simple

import org.evomaster.core.search.structuralelement.simple.model.Leaf
import org.evomaster.core.search.structuralelement.simple.model.Middle
import org.evomaster.core.search.structuralelement.simple.model.Root
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled

class BasicElementCopyTest {

    @Test
    fun testFind(){
        val leafA1 = Leaf("A1")
        val leafA2 = Leaf("A2")
        val leafA3 = Leaf("A3")
        val leafA4 = Leaf("A4")
        val leafA5 = Leaf("A5")
        val middleA = Middle(1, mutableListOf(leafA1, leafA2, leafA3, leafA4, leafA5))

        val leafB1 = Leaf("B1")
        val leafB2 = Leaf("B2")
        val middleB = Middle(2, mutableListOf(leafB1, leafB2))

        val root = Root(3.0, mutableListOf(middleA, middleB))

        val found = root.targetWithIndex(listOf(0, 3))
        assertTrue(found is Leaf)
        assertEquals("A4", (found as Leaf).data)

        val backpath = mutableListOf<Int>()
        found.traverseBackIndex(backpath)
        assertEquals(2, backpath.size)
        assertTrue(backpath.containsAll(listOf(0,3)))
    }

    @Test
    fun testCopy(){
        val leafA1 = Leaf("A1")
        val leafA2 = Leaf("A2")
        val middleA = Middle(1, mutableListOf(leafA1, leafA2))

        val leafB1 = Leaf("B1")
        val leafB2 = Leaf("B2")
        val middleB = Middle(2, mutableListOf(leafB1, leafB2))

        //binding
        leafA1.binding.add(leafB1)
        leafB1.binding.add(leafA1)

        val root = Root(3.0, mutableListOf(middleA, middleB))
        val copy = root.copy()

        assertTrue(copy is Root)
        assertEquals(2, copy.getViewOfChildren().size)
        assertEquals(3.0, (copy as Root).data)
        assertEquals(1, copy.middles[0].data)
        assertEquals("A1", copy.middles[0].leaves[0].data)
        assertEquals(1, copy.middles[0].leaves[0].binding.size)
        assertEquals("B1", copy.middles[0].leaves[0].binding.first().data)
        assertEquals("A2", copy.middles[0].leaves[1].data)
        assertEquals(2, copy.middles[1].data)
        assertEquals("B1", copy.middles[1].leaves[0].data)
        assertEquals("A1", copy.middles[1].leaves[0].binding.first().data)
        assertEquals("B2", copy.middles[1].leaves[1].data)
    }
}
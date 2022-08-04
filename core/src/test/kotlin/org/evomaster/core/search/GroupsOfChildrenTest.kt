package org.evomaster.core.search

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GroupsOfChildrenTest {

    @Test
    fun testOneGroupOnly() {
        val children = listOf<StructuralElement>()
        val groups = listOf(ChildGroup<StructuralElement>("a"))
        assertThrows(Exception::class.java) { GroupsOfChildren(children, groups) }
    }


    @Test
    fun testEmpty() {
        val children = listOf<StructuralElement>()
        val groups = listOf(ChildGroup<StructuralElement>("a"), ChildGroup<StructuralElement>("b"))
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()
    }

    @Test
    fun testOneEmptyGroup() {
        val children = listOf("x")
        val groups = listOf(ChildGroup<String>("a"), ChildGroup("b", startIndex = 0, endIndex = 0))
        val k = GroupsOfChildren(children, groups)
        assertEquals(0, k.sizeOfGroup("a"))
        assertEquals(1, k.sizeOfGroup("b"))
        k.verifyGroups()
    }

    @Test
    fun testAddToEmptyGroup() {
        val children = mutableListOf("x")
        val groups = listOf(ChildGroup<String>("a"), ChildGroup("b", startIndex = 0, endIndex = 0))
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()

        children.add("foo")
        assertThrows(Exception::class.java) { k.verifyGroups() }
        k.addedToGroup("a", "foo")
        k.verifyGroups()
    }

    @Test
    fun testAddToMiddleEmptyGroup() {
        val children = mutableListOf("x0", "x1", "z0")
        val groups = listOf(
            ChildGroup<String>("x", { x -> x.startsWith("x") }, 0, 1),
            ChildGroup<String>("y", { y -> y.startsWith("y") }),
            ChildGroup<String>("z", { z -> z.startsWith("z") }, 2, 2)
        )
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()

        children.add(2, "y0")
        assertThrows(Exception::class.java) { k.verifyGroups() }
        k.addedToGroup("y", "y0")
        k.verifyGroups()
    }

    @Test
    fun testAddToMiddleNonEmptyGroup() {
        val children = mutableListOf("x0", "x1", "y0", "z0")
        val groups = listOf(
            ChildGroup<String>("x", { x -> x.startsWith("x") }, 0, 1),
            ChildGroup<String>("y", { y -> y.startsWith("y") }, 2, 2),
            ChildGroup<String>("z", { z -> z.startsWith("z") }, 3, 3)
        )
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()

        val element = "y1"
        /*
             adding at end of a group could be equivalent to adding and begging of next.
             the position is the same.
             but need to specify which of the 2 groups it is added to
         */
        children.add(3, element)
        assertThrows(Exception::class.java) { k.verifyGroups() }
        assertThrows(Exception::class.java) { k.addedToGroup("z", element) }

        k.addedToGroup("y", element)
        k.verifyGroups()

        assertEquals(k.endIndexForGroupInsertionInclusive("x"), k.startIndexForGroupInsertionInclusive("y"))
        assertEquals(k.endIndexForGroupInsertionInclusive("y"), k.startIndexForGroupInsertionInclusive("z"))
    }


    @Test
    fun testRemoveFromGroups() {
        val children = mutableListOf("x0", "x1", "y0", "z0")
        val groups = listOf(
            ChildGroup<String>("x", { x -> x.startsWith("x") }, 0, 1),
            ChildGroup<String>("y", { y -> y.startsWith("y") }, 2, 2),
            ChildGroup<String>("z", { z -> z.startsWith("z") }, 3, 3)
        )
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()

        children.removeAt(1)
        k.removedFromGroup("x")
        k.verifyGroups()

        children.removeAt(1)
        k.removedFromGroup("y")
        k.verifyGroups()

        children.removeAt(0)
        k.removedFromGroup("x")
        k.verifyGroups()

        children.removeAt(0)
        k.removedFromGroup("z")
        k.verifyGroups()
    }

}
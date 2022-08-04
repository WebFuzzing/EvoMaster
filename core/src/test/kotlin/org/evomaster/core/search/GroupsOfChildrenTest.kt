package org.evomaster.core.search

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GroupsOfChildrenTest{

    @Test
    fun testOneGroupOnly(){
        val children = listOf<StructuralElement>()
        val groups = listOf(ChildGroup<StructuralElement>("a"))
        assertThrows(Exception::class.java) { GroupsOfChildren(children, groups) }
    }


    @Test
    fun testEmpty(){
        val children = listOf<StructuralElement>()
        val groups = listOf(ChildGroup<StructuralElement>("a"), ChildGroup<StructuralElement>("b"))
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()
    }

    @Test
    fun testOneEmptyGroup(){
        val children = listOf("x")
        val groups = listOf(ChildGroup<String>("a"), ChildGroup("b", startIndex = 0, endIndex = 0))
        val k = GroupsOfChildren(children, groups)
        assertEquals(0, k.sizeOfGroup("a"))
        assertEquals(1, k.sizeOfGroup("b"))
        k.verifyGroups()
    }

    @Test
    fun testAddToEmptyGroup(){
        val children = mutableListOf("x")
        val groups = listOf(ChildGroup<String>("a"), ChildGroup("b", startIndex = 0, endIndex = 0))
        val k = GroupsOfChildren(children, groups)
        k.verifyGroups()

        children.add("foo")
        assertThrows(Exception::class.java){k.verifyGroups()}
        k.addedToGroup("a", "foo")
        k.verifyGroups()
    }

}
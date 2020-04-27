package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.implementation.java.service.JavaResourceAPI
import org.evomaster.resource.rest.generator.implementation.java.service.JavaRestMethod
import org.evomaster.resource.rest.generator.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-01-06
 */
class TestResourceGraph {

    private val graph = ObjectsForTest.getGraph()

    @Test
    fun testBreadthTraverse(){

        val abcde = listOf("A","B","C","D","E")
        val nodeA = ObjectsForTest.getResNode("A")
        assertNotNull(nodeA)
        graph.breadth(nodeA!!).forEachIndexed { index, resNode ->
            assertEquals(abcde[index], resNode.name)
        }

        val bd = listOf("B", "D")
        val nodeB = ObjectsForTest.getResNode("B")
        assertNotNull(nodeB)
        graph.breadth(nodeB!!).forEachIndexed { index, resNode ->
            assertEquals(bd[index], resNode.name)
        }
    }

    @Test
    fun testPathGeneration(){
        val idName = "id"
        val nodeA = ObjectsForTest.getResNode("A")
        assertNotNull(nodeA)
        val expectedAPath = listOf("B", "C", "D", "E").reversed().joinToString("") { "/${FormatUtil.formatResourceOnPath(it)}/{${FormatUtil.formatResourceIdAsPathParam(it, idName)}}" }
        assertEquals("$expectedAPath/${FormatUtil.formatResourceOnPath("A")}", graph.getPathWithIds(nodeA!!, idName, includeDependency = true))
    }


}
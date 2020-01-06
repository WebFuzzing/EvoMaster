package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-01-06
 */
class TestResourceGraph {

    @Test
    fun testBreadthTraverse(){
        val resNodeA = MultipleResNode("A")

        val a1 = InnerResNode("A1", resNodeA)
        val a2 = InnerResNode("A2", resNodeA)
        resNodeA.nodes.add(a1)
        resNodeA.nodes.add(a2)

        val resNodeB = ResNode("B")
        val resNodeC = ResNode("C")
        val resNodeD = ResNode("D")
        val resNodeE = ResNode("E")

        val aToB = ResEdge(resNodeA, resNodeB)
        resNodeA.outgoing.add(aToB)
        resNodeB.incoming.add(aToB)

        val aToC = ResEdge(resNodeA, resNodeC)
        resNodeA.outgoing.add(aToC)
        resNodeC.incoming.add(aToC)

        val bToD = ResEdge(resNodeB, resNodeD)
        resNodeB.outgoing.add(bToD)
        resNodeD.incoming.add(bToD)

        val cToE = ResEdge(resNodeC, resNodeE)
        resNodeC.outgoing.add(cToE)
        resNodeE.incoming.add(cToE)

        val graph = ResourceGraph(nodes = mutableMapOf(
                a1.name to a1,
                a2.name to a2,
                resNodeA.name to resNodeA,
                resNodeB.name to resNodeB,
                resNodeC.name to resNodeC,
                resNodeD.name to resNodeD,
                resNodeE.name to resNodeE
        ),
                edges = mutableListOf(aToB, aToC, bToD, cToE))

        val nodes = listOf("A","B","C","D","E")
        graph.breadth(resNodeA).forEachIndexed { index, resNode ->
            Assertions.assertEquals(nodes[index], resNode.name)
        }

        val bd = listOf("B", "D")
        graph.breadth(resNodeB).forEachIndexed { index, resNode ->
            Assertions.assertEquals(bd[index], resNode.name)
        }
    }
}
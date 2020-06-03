package org.evomaster.resource.rest.generator

import org.evomaster.resource.rest.generator.model.*

/**
 * created by manzh on 2020-01-07
 */
object ObjectsForTest {
    private val nodes = mutableMapOf<String, ResNode>()
    private val edges = mutableListOf<ResEdge>()
    private val graph : ResourceGraph

    init {
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

        nodes.clear()
        nodes.putAll(mutableMapOf(
                a1.name to a1,
                a2.name to a2,
                resNodeA.name to resNodeA,
                resNodeB.name to resNodeB,
                resNodeC.name to resNodeC,
                resNodeD.name to resNodeD,
                resNodeE.name to resNodeE))
        edges.clear()
        edges.addAll(mutableListOf(aToB, aToC, bToD, cToE))
        graph = ResourceGraph(nodes = nodes, edges = edges)
    }

    fun getGraph() = graph

    fun getResNode(name : String) = nodes[name]
}
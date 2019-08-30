package org.evomaster.resource.rest.generator.model

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.util.mxCellRenderer
import org.evomaster.resource.rest.generator.FormatUtil
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.jgrapht.io.*
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO


/**
 * created by manzh on 2019-08-19
 */
class ResourceGraph(
        val numOfNodes : Int,
        val multiplicity: List<EdgeMultiplicitySpecification>,
        private val graphName : String = "resourceGraph"
) {


    val nodes : MutableMap<String, ResNode> = mutableMapOf()
    val edges : MutableList<ResEdge> = mutableListOf()

    private val alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz"


    init {
        multiplicity.forEach { s ->
            if (s.n == 1){
                if (s.m == 1){
                    add(s.num)
                }else{
                    add(s.num, s.m, false)
                }
            }else if(s.m == 1){
                add(s.num, s.n, true)
            }else{
                add(s.num, s.n, s.m)
            }
        }

        if(numOfNodes < nodes.size)
            throw IllegalArgumentException("invalid number of nodes and edges")

        (0 until (numOfNodes - nodes.size)).forEach { _ ->
            ResNode(nameResource()).apply {
                nodes.put(this.name, this)
            }
        }
    }

    private fun add(numOfOneToOne : Int){
        var last : ResNode? = null
        (0 until numOfOneToOne).forEach { _ ->
            if (last == null) {
                last = ResNode(nameResource())
                nodes[last!!.name] = last!!
            }
            val current = ResNode(nameResource())
            nodes[current.name] = current

            val edge = ResEdge(last!!, current)
            edges.add(edge)

            edge.source.outgoing.add(edge)
            edge.target.incoming.add(edge)

            last = current
        }
    }

    private fun add(numOfOneToMulti : Int, multi : Int, isDirectToOne : Boolean){
        assert(multi > 1)
        var one : ResNode? = nodes.values.find{ if(isDirectToOne) it.incoming.isEmpty() else it.outgoing.isEmpty() }
        (0 until numOfOneToMulti).forEach { _ ->
            if (one == null) {
                one = ResNode(nameResource())
                nodes[one!!.name] = one!!
            }

            (0 until multi).forEach { _ ->
                val current = ResNode(nameResource())
                nodes[current.name] = current

                val edge = if (isDirectToOne) ResEdge(current, one!!) else ResEdge(one!!, current)
                edges.add(edge)

                edge.source.outgoing.add(edge)
                edge.target.incoming.add(edge)
            }

            one = nodes.values.find{ if(isDirectToOne) it.incoming.isEmpty() else it.outgoing.isEmpty() }
        }
    }

    private fun add(numOfMultiToMulti : Int, firstMulti: Int, secondMulti : Int){
        assert(firstMulti > 1 && secondMulti > 1)

        (0 until numOfMultiToMulti).forEach { _ ->
            var multi = MultipleResNode(nameResource())
            var innerNodes = (0 until firstMulti).map { InnerResNode(nameResource(), multi).also {
                nodes[it.name] = it
            } }.toList()
            multi.nodes.addAll(innerNodes)

            (0 until secondMulti).forEach { _ ->
                val current = ResNode(nameResource())
                nodes[current.name] = current

                val edge = ResEdge(multi, current)
                edges.add(edge)

                edge.source.outgoing.add(edge)
                edge.target.incoming.add(edge)
            }
            nodes[multi.name] = multi
        }
    }

    private fun nameResource() : String {
        var name = randomResourceName()
        var count = 0
        while (nodes.containsKey(name) && count < 3){
            name = randomResourceName()
            count++
        }
        if (count == 3) throw IllegalStateException("cannot find valid name")
        return name
    }

    private fun randomResourceName() = "R${random()}"

    private fun random(length : Int = ((Math.random() * 5).toInt().plus(1))) : String = (0 until length).map { alphaNumericString[(alphaNumericString.length * Math.random()).toInt()] }.joinToString("")


    fun getRootNodes() : Map<String, ResNode> = nodes.filter { it.value.incoming.isEmpty() }

    fun getLeafNodes() : Map<String, ResNode> = nodes.filter { it.value.outgoing.isEmpty() }

    fun getSoleNodes() : Map<String, ResNode> = nodes.filter { it.value.outgoing.isEmpty() && it.value.incoming.isEmpty() }

    fun save(outputFolder : String, format : GraphExportFormat = GraphExportFormat.DOT){
        val graph = DirectedMultigraph<ResNode, LabelEdge>(LabelEdge::class.java)
        nodes.values.forEach {
            graph.addVertex(it)
            if (it is MultipleResNode){
                it.nodes.forEach{i->
                    graph.addEdge(it, i, LabelEdge("<includes>"))
                }
            }
        }
        edges.forEach {
            graph.addEdge(it.source, it.target, LabelEdge("<depends>"))
        }
        val dir = FormatUtil.formatFolder(outputFolder)
        Files.createDirectories(Paths.get(dir))
        when(format){
            GraphExportFormat.DOT -> saveAsDOT(dir, graph)
            GraphExportFormat.PNG -> saveAsImage(dir, graph)
        }
    }

    inner class LabelEdge(val label : String) : DefaultEdge()

    /**
     * https://dreampuf.github.io/GraphvizOnline/
     */
    private fun saveAsDOT(outputFolder: String, graph : DirectedMultigraph<ResNode, LabelEdge>){
        val vertexProvider = ComponentNameProvider<ResNode> { p -> p!!.name }
        val edgeLabelProvider = ComponentNameProvider<LabelEdge> { p-> p!!.label  }
        val exporter = DOTExporter(vertexProvider, vertexProvider, edgeLabelProvider)
        exporter.exportGraph(graph, Paths.get("$outputFolder$graphName.dot").toFile())
    }

    private fun saveAsImage(outputFolder: String, graph : DirectedMultigraph<ResNode, LabelEdge>){
        val adapter = JGraphXAdapter<ResNode, LabelEdge>(graph)
        mxHierarchicalLayout(adapter).execute(adapter.defaultParent)
        val image = mxCellRenderer.createBufferedImage(adapter, null, 2.0, Color.WHITE, true, null)
        ImageIO.write(image, "PNG", Paths.get("$outputFolder$graphName.png").toFile())
    }
}

enum class GraphExportFormat{
    DOT,
    PNG
}
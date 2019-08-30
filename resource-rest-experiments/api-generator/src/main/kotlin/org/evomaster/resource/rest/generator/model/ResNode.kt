package org.evomaster.resource.rest.generator.model

/**
 * created by manzh on 2019-08-19
 */
open class ResNode (
        val name : String,
        val incoming : MutableList<ResEdge> = mutableListOf(),
        val outgoing : MutableList<ResEdge> = mutableListOf()
){
    open fun getFullName() : String = name
}

class MultipleResNode(
        name: String,
        val nodes : MutableList<InnerResNode> = mutableListOf(),
        incoming: MutableList<ResEdge> = mutableListOf(),
        outgoing: MutableList<ResEdge> = mutableListOf()
) : ResNode(name, incoming, outgoing){
    override fun getFullName(): String = "$name{${nodes.joinToString(",") { it.name }}}"
}

class ComplexResNode(
        name: String,
        val nodes : MutableList<ResNode> = mutableListOf(),
        incoming: MutableList<ResEdge> = mutableListOf(),
        outgoing: MutableList<ResEdge> = mutableListOf()
) : ResNode(name, incoming, outgoing)

class InnerResNode(
        name : String,
        val multipleResNode: MultipleResNode
) : ResNode(name){
    init {
        assert(incoming.isEmpty() && outgoing.isEmpty())
    }

    override fun getFullName(): String = "$name:${multipleResNode.name}"
}
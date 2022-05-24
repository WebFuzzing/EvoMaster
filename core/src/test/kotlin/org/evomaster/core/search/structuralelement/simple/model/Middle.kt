package org.evomaster.core.search.structuralelement.simple.model

import org.evomaster.core.search.StructuralElement

class Middle(val data: Int, val leaves : MutableList<Leaf>) : StructuralElement(children = mutableListOf<StructuralElement>().apply { addAll(leaves) }) {

    override fun copyContent(): Middle {
        return Middle(data, leaves.map { it.copy() as Leaf}.toMutableList())
    }

}
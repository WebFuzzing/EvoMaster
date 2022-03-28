package org.evomaster.core.search.structuralelement.simple.model

import org.evomaster.core.search.StructuralElement

class Root(val data: Double, val middles : MutableList<Middle>) : StructuralElement(children = mutableListOf<StructuralElement>().apply { addAll(middles) }) {

    override fun getChildren(): MutableList<Middle> {
        return middles
    }


    override fun copyContent(): StructuralElement {
        return Root(data, middles.map { it.copyContent() }.toMutableList())
    }

}
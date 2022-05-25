package org.evomaster.core.search.structuralelement.simple.model

import org.evomaster.core.search.RootElement
import org.evomaster.core.search.StructuralElement

class Root(val data: Double, val middles : MutableList<Middle>)
    : StructuralElement(children = mutableListOf<StructuralElement>().apply { addAll(middles) }), RootElement {




    override fun copyContent(): StructuralElement {
        return Root(data, middles.map { it.copy() as Middle}.toMutableList())
    }

}
package org.evomaster.core.search.service.minimizer

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.MainAction
import org.evomaster.core.search.gene.Gene

class MultiActionAction(val id: Int) : MainAction(isCleanUp = false, children = listOf()) {

    override fun getName(): String {
        return "MultiAction_$id"
    }

    override fun seeTopGenes(): List<Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return MultiActionAction(id)
    }
}

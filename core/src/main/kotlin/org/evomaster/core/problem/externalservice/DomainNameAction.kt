package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class DomainNameAction : EnvironmentAction(listOf()) {
    override fun getName(): String {
        return ""
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return DomainNameAction()
    }

}

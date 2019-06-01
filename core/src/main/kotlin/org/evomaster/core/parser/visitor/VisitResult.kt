package org.evomaster.core.parser.visitor

import org.evomaster.core.search.gene.Gene


class VisitResult(
        val genes: MutableList<Gene> = mutableListOf(),
        var data: Any? = null
){

    constructor(gene: Gene) : this() {
        genes.add(gene)
    }
}
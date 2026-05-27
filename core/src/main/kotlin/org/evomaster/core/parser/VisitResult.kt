package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene

/**
 *  A call on the parser visitor would return either Gene(s) or some data
 */
class VisitResult(
        val genes: MutableList<Gene> = mutableListOf(),
        var data: Any? = null,
        var hadFilteredContent: Boolean = false
){

    constructor(gene: Gene) : this() {
        genes.add(gene)
    }
}
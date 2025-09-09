package org.evomaster.core.search.gene.root

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

/**
 * A Gene that has internal genes.
 * If number of children is not mutable, ie fixed, then should rather use the subclass [CompositeFixedGene]
 */
abstract class CompositeGene(
        name: String,
        children: MutableList<out Gene>
) : Gene(name, children){

    constructor(name: String, child: Gene) : this(name, mutableListOf(child))

    open fun getDtoCall(actionName: String, counter: Integer): List<String> {
        throw RuntimeException("BUG: Gene $name (with type ${this::class.java.simpleName}) should not be creating DTOs")
    }

}

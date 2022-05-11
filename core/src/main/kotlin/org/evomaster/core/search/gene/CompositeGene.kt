package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement

/**
 * A Gene that has internal genes
 */
abstract class CompositeGene(
        name: String,
        children: MutableList<Gene>
        /*
            TODO should cases for mutable and non-mutable. override all modification methods.
            ie, pass as read-only List, and check if mutable. if so, handle it accordingly
         */
) : Gene(name, children){

    init {
        if(children.isEmpty() && !canBeChildless()){
            throw IllegalStateException("A composite gene must have at least 1 internal gene")
        }
    }

    open fun canBeChildless() = false
}
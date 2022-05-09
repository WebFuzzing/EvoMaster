package org.evomaster.core.search.gene

import org.evomaster.core.search.StructuralElement

/**
 * A Gene that has internal genes
 */
abstract class CompositeGene(
        name: String,
        children: List<out StructuralElement>
) : Gene(name, children){

    init {
        if(children.isEmpty() && !canBeChildless()){
            throw IllegalStateException("A composite gene must have at least 1 internal gene")
        }
    }

    open fun canBeChildless() = false
}
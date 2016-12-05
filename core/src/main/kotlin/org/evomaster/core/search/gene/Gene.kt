package org.evomaster.core.search.gene


/**
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 */
abstract class Gene(val name: String) {

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    abstract fun copy() : Gene

    open fun isMutable() = true
}
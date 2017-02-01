package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


/**
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 */
abstract class Gene(var name: String) {

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    abstract fun copy() : Gene

    open fun isMutable() = true

    abstract fun randomize(randomness: Randomness, forceNewValue: Boolean)

    abstract fun getValueAsString() : String
}
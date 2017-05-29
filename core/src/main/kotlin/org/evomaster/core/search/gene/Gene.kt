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

    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     * "foo" -> "\"foo\"" -> printed as "foo"
     */
    abstract fun getValueAsPrintableString() : String

    open fun getValueAsRawString() = getValueAsPrintableString()

    abstract fun copyValueFrom(other: Gene)

    /**
     * If this gene represents a variable, then return its name.
     */
    open fun getVariableName() = name
}
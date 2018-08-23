package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class StringGene (
        name: String,
        var value: String = "foo",
        /** Inclusive */
        val minLength: Int = 0,
        /** Inclusive */
        val maxLength: Int = 16
) : Gene(name) {

    /*
        Even if through mutation we can get large string, we should
        avoid sampling very large strings by default
     */
    private val maxForRandomizantion = 16

    override fun copy(): Gene {
        val copy = StringGene(name, value, minLength, maxLength)
        return copy
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        //TODO much more would need to be done here to handle strings...
        value = randomness.nextWordString(minLength,Math.min(maxLength, maxForRandomizantion))
    }

    override fun getValueAsPrintableString() : String{
        return "\"$value\""
    }

    override fun getValueAsRawString() : String {
        return value
    }

    override fun copyValueFrom(other: Gene){
        if(other !is StringGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }
}
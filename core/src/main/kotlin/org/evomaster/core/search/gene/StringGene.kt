package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.Randomness


class StringGene(
        name: String,
        var value: String = "foo",
        /** Inclusive */
        val minLength: Int = 0,
        /** Inclusive */
        val maxLength: Int = 16,
        /**
         * Depending on what a string is representing, there might be some chars
         * we do not want to use.
         * For example, in a URL Path variable, we do not want have "/", as otherwise
         * it would create 2 distinct paths
         */
        val invalidChars : List<Char> = listOf()
) : Gene(name) {

    /*
        Even if through mutation we can get large string, we should
        avoid sampling very large strings by default
     */
    private val maxForRandomizantion = 16

    private var validChar: String? = null

    override fun copy(): Gene {
        return StringGene(name, value, minLength, maxLength)
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        //TODO much more would need to be done here to handle strings...
        value = randomness.nextWordString(minLength, Math.min(maxLength, maxForRandomizantion))
        repair()
    }

    /**
     * Make sure no invalid chars is used
     */
    fun repair(){
        if(invalidChars.isEmpty()){
            //nothing to do
            return
        }

        if(validChar == null){
            //compute a valid char
            for(c in 'a' .. 'z'){
                if(! invalidChars.contains(c)){
                    validChar = c.toString()
                    break
                }
            }
        }
        if(validChar == null){
            //no basic char is valid??? TODO should handle this situation, although likely never happens
            return
        }

        for(invalid in invalidChars){
            value = value.replace("$invalid", validChar!!)
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        val rawValue = getValueAsRawString()
        if (mode != null && mode.equals("xml")) {
            return StringEscapeUtils.escapeXml(rawValue)
        } else {
            when {
                // TODO this code should be refactored with other getValueAsPrintableString() methods
                (targetFormat == null) -> return "\"$rawValue\""
                targetFormat.isKotlin() -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
                        .replace("$", "\\$")
                else -> return "\"$rawValue\""
                        .replace("\\", "\\\\")
            }
        }
    }

    override fun getValueAsRawString(): String {
        return value
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }

}
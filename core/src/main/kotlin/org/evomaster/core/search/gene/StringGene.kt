package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.getDelta
import org.evomaster.core.search.service.AdaptiveParameterControl
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
        val invalidChars: List<Char> = listOf()
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

        value = if (name == "type" && randomness.nextBoolean())
            //FIXME: tmp hack until we have proper seeding support
            randomness.choose(listOf("lov", "forskrift"))
        else
        //TODO much more would need to be done here to handle strings...
            randomness.nextWordString(minLength, Math.min(maxLength, maxForRandomizantion))

        repair()
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        val p = randomness.nextDouble()
        val s = value

        /*
            What type of mutations we do on Strings is strongly
            correlated on how we define the fitness functions.
            When dealing with equality, as we do left alignment,
            then it makes sense to prefer insertion/deletion at the
            end of the strings, and reward more "change" over delete/add
         */

        val others = allGenes.flatMap { g -> g.flatView() }
                .filterIsInstance<StringGene>()
                .map { g -> g.value }
                .filter { it != value }

        value = when {
            //seeding: replace
            p < 0.02 && !others.isEmpty() -> {
                randomness.choose(others)
            }
            //change
            p < 0.8 && s.isNotEmpty() -> {
                val delta = getDelta(randomness, apc, start = 6, end = 3)
                val sign = randomness.choose(listOf(-1, +1))
                val i = randomness.nextInt(s.length)
                val array = s.toCharArray()
                array[i] = s[i] + (sign * delta)
                String(array)
            }
            //delete last
            p < 0.9 && s.isNotEmpty() && s.length > minLength -> {
                s.dropLast(1)
            }
            //append new
            s.length < maxLength -> {
                if (s.isEmpty() || randomness.nextBoolean(0.8)) {
                    s + randomness.nextWordChar()
                } else {
                    val i = randomness.nextInt(s.length)
                    if (i == 0) {
                        randomness.nextWordChar() + s
                    } else {
                        s.substring(0, i) + randomness.nextWordChar() + s.substring(i, s.length)
                    }
                }
            }
            else -> {
                //do nothing
                s
            }
        }

        repair()
    }

    /**
     * Make sure no invalid chars is used
     */
    fun repair() {
        if (invalidChars.isEmpty()) {
            //nothing to do
            return
        }

        if (validChar == null) {
            //compute a valid char
            for (c in 'a'..'z') {
                if (!invalidChars.contains(c)) {
                    validChar = c.toString()
                    break
                }
            }
        }
        if (validChar == null) {
            //no basic char is valid??? TODO should handle this situation, although likely never happens
            return
        }

        for (invalid in invalidChars) {
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
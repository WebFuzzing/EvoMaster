package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
open class DateTimeGene(
        name: String,
        val date: DateGene = DateGene("date"),
        val time: TimeGene = TimeGene("time")
) : Gene(name) {

    override fun copy(): Gene = DateTimeGene(
            name,
            date.copy() as DateGene,
            time.copy() as TimeGene
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        /**
         * If forceNewValue==true both date and time
         * get a new value, but it only might need
         * one to be different to get a new value.
         *
         * Shouldn't this method decide randomly if
         * date, time or both get a new value?
         */
        date.randomize(randomness, forceNewValue, allGenes)
        time.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        val gene = randomness.choose(listOf(date, time))
        gene.standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return "${date.getValueAsRawString()}" +
                "T" +
                "${time.getValueAsRawString()}"
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.date.copyValueFrom(other.date)
        this.time.copyValueFrom(other.time)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateTimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.date.containsSameValueAs(other.date)
                && this.time.containsSameValueAs(other.time)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(date.flatView(excludePredicate)).plus(time.flatView(excludePredicate))
    }
}
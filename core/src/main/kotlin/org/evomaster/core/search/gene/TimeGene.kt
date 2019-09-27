package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
class TimeGene(
        name: String,
        //note: ranges deliberately include wrong values.
        val hour: IntegerGene = IntegerGene("hour", 0, -1, 25),
        val minute: IntegerGene = IntegerGene("minute", 0, -1, 60),
        val second: IntegerGene = IntegerGene("second", 0, -1, 60),
        val timeGeneFormat: TimeGeneFormat = TimeGeneFormat.TIME_WITH_MILLISECONDS
) : Gene(name) {

    enum class TimeGeneFormat {
        // format HH:MM:SS
        ISO_LOCAL_DATE_FORMAT,

        // HH:MM:SS.000Z
        TIME_WITH_MILLISECONDS
    }

    /*
        Note: would need to handle timezone and second fractions,
        but not sure how important for testing purposes
     */

    override fun copy(): Gene = TimeGene(
            name,
            hour.copy() as IntegerGene,
            minute.copy() as IntegerGene,
            second.copy() as IntegerGene,
            timeGeneFormat = this.timeGeneFormat
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        hour.randomize(randomness, forceNewValue, allGenes)
        minute.randomize(randomness, forceNewValue, allGenes)
        second.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        val gene = randomness.choose(listOf(hour, minute, second))
        gene.standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return when (timeGeneFormat) {
            TimeGeneFormat.ISO_LOCAL_DATE_FORMAT -> {
                GeneUtils.let {
                    "${it.padded(hour.value, 2)}:${it.padded(minute.value, 2)}:${it.padded(second.value, 2)}"
                }
            }
            TimeGeneFormat.TIME_WITH_MILLISECONDS -> {
                GeneUtils.let {
                    "${it.padded(hour.value, 2)}:${it.padded(minute.value, 2)}:${it.padded(second.value, 2)}.000Z"
                }
            }
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.hour.copyValueFrom(other.hour)
        this.minute.copyValueFrom(other.minute)
        this.second.copyValueFrom(other.second)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TimeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.hour.containsSameValueAs(other.hour)
                && this.minute.containsSameValueAs(other.minute)
                && this.second.containsSameValueAs(other.second)
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(hour.flatView(excludePredicate))
                .plus(minute.flatView(excludePredicate))
                .plus(second.flatView(excludePredicate))
    }

    private fun isValidHourRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 23
    }

    private fun isValidMinuteRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 59
    }

    private fun isValidSecondRange(gene: IntegerGene): Boolean {
        return gene.min == 0 && gene.max == 59
    }

    /**
     * Queries the time gent to know if it can
     * store only valid hours (i.e. range 00:00:00 to 23:59:59)
     */
    val onlyValidHours: Boolean
        get() = isValidHourRange(this.hour)
                && isValidMinuteRange(this.minute)
                && isValidSecondRange(this.second)

}
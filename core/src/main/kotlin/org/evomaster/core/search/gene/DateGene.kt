package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
class DateGene(
        name: String,
        //note: ranges deliberately include wrong values.
        val year: IntegerGene = IntegerGene("year", 2016, 1900, 2100),
        val month: IntegerGene = IntegerGene("month", 3, 0, 13),
        val day: IntegerGene = IntegerGene("day", 12, 0, 32),
        val onlyValidDates: Boolean = false
) : Gene(name) {


    override fun copy(): Gene = DateGene(name,
            year.copy() as IntegerGene,
            month.copy() as IntegerGene,
            day.copy() as IntegerGene,
            onlyValidDates = onlyValidDates)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        do {
            year.randomize(randomness, forceNewValue, allGenes)
            month.randomize(randomness, forceNewValue, allGenes)
            day.randomize(randomness, forceNewValue, allGenes)
        } while (onlyValidDates && !isValidDate())
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        do {
            val gene = randomness.choose(listOf(year, month, day))
            gene.standardMutation(randomness, apc, allGenes)
        } while (onlyValidDates && !isValidDate())

    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return GeneUtils.let {
            "${it.padded(year.value, 4)}-${it.padded(month.value, 2)}-${it.padded(day.value, 2)}"
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.onlyValidDates && !other.isValidDate()) {
            throw IllegalArgumentException("Cannot copy invalid date %s to gene accepting only valid values".format(other.getValueAsRawString()))
        }
        this.year.copyValueFrom(other.year)
        this.month.copyValueFrom(other.month)
        this.day.copyValueFrom(other.day)
    }

    /**
     * Checks if the current date is valid or not (e.g. max days in months, leap years)
     */
    private fun isValidDate(): Boolean {
        return try {
            LocalDate.parse(getValueAsRawString(), DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (ex: DateTimeParseException) {
            false
        }
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.year.containsSameValueAs(other.year)
                && this.month.containsSameValueAs(other.month)
                && this.day.containsSameValueAs(other.day)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(year.flatView(excludePredicate))
                    .plus(month.flatView(excludePredicate))
                    .plus(day.flatView(excludePredicate))
    }

}
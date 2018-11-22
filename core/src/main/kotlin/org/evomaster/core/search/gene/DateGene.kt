package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

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
        val day: IntegerGene = IntegerGene("day", 12, 0, 32)
) : Gene(name) {


    override fun copy(): Gene = DateGene(name,
            year.copy() as IntegerGene,
            month.copy() as IntegerGene,
            day.copy() as IntegerGene)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        year.randomize(randomness, forceNewValue, allGenes)
        month.randomize(randomness, forceNewValue, allGenes)
        day.randomize(randomness, forceNewValue, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?): String {
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
        this.year.copyValueFrom(other.year)
        this.month.copyValueFrom(other.month)
        this.day.copyValueFrom(other.day)
    }


    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DateGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.year.containsSameValueAs(other.year)
                && this.month.containsSameValueAs(other.month)
                && this.day.containsSameValueAs(other.day)
    }

    override fun flatView(): List<Gene> {
        return listOf(this, year, month, day)
    }

    override fun flatViewWithTypeFilter(predicate: (Gene) -> Boolean): List<Gene>{
        return if(predicate(this)) listOf(this)
        else listOf(this).plus(year.flatViewWithTypeFilter(predicate))
                .plus(month.flatViewWithTypeFilter(predicate))
                .plus(day.flatViewWithTypeFilter(predicate))
    }

}
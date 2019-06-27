package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat


class SqlTimestampGene(
        name: String,
        date: DateGene = DateGene("date",
                year = IntegerGene("year", 2016, 1900, 2100),
                month = IntegerGene("month", 3, 1, 12),
                day = IntegerGene("day", 12, 1, 31),
                onlyValidDates = true),
        time: TimeGene = TimeGene("time",
                hour = IntegerGene("hour", 0, 0, 23),
                minute = IntegerGene("minute", 0, 0, 59),
                second = IntegerGene("second", 0, 0, 59),
                withMsZ = false)
) : DateTimeGene(name, date, time) {

    init {
        require(!time.withMsZ) { "Must not have MsZ in SQL Timestamps" }
    }

    override fun copy(): Gene = SqlTimestampGene(
            name,
            date.copy() as DateGene,
            time.copy() as TimeGene
    )

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return "${date.getValueAsRawString()}" +
                " " +
                "${time.getValueAsRawString()}"
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlTimestampGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.date.copyValueFrom(other.date)
        this.time.copyValueFrom(other.time)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTimestampGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.date.containsSameValueAs(other.date)
                && this.time.containsSameValueAs(other.time)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(date.flatView(excludePredicate))
                    .plus(time.flatView(excludePredicate))
    }
}
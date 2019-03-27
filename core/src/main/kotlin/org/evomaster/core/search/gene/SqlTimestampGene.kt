package org.evomaster.core.search.gene


class SqlTimestampGene(
        name: String,
        date: DateGene = DateGene("date",
                IntegerGene("year", 2016, 1900, 2100),
                IntegerGene("month", 3, 1, 12),
                IntegerGene("day", 12, 1, 28)),
        time: TimeGene = TimeGene("time",
                IntegerGene("hour", 0, 0, 23),
                IntegerGene("minute", 0, 0, 59),
                IntegerGene("second", 0, 0, 59),
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

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?): String {
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

}
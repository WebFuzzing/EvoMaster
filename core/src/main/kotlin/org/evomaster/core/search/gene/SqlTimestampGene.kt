package org.evomaster.core.search.gene


class SqlTimestampGene(
        name: String,
        date: DateGene = DateGene("date"),
        time: TimeGene = TimeGene("time", withMsZ = false)
) : DateTimeGene(name, date, time) {

    init{
        require(! time.withMsZ){"Must not have MsZ in SQL Timestamps"}
    }

    override fun copy(): Gene = SqlTimestampGene(
            name,
            date.copy() as DateGene,
            time.copy() as TimeGene
    )

    override fun getValueAsPrintableString(): String {
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
}
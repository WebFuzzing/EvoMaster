package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

/**
 * Using RFC3339
 *
 * https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
 */
class DateTimeGene(
        name: String,
        val date: DateGene = DateGene("date"),
        val time: TimeGene = TimeGene("time")
): Gene(name) {


    override fun copy(): Gene = DateTimeGene(
            name,
            date.copy() as DateGene,
            time.copy() as TimeGene
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        date.randomize(randomness, forceNewValue)
        time.randomize(randomness, forceNewValue)
    }

    override fun getValueAsString(): String {
        return "\"${date.getValueAsString().replace("\"","")}T" +
                "${time.getValueAsString().replace("\"","")}\""
    }

    override fun copyValueFrom(other: Gene){
        if(other !is DateTimeGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.date.copyValueFrom(other.date)
        this.time.copyValueFrom(other.time)
    }
}
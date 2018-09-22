package org.evomaster.core.search.gene

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
        val second: IntegerGene = IntegerGene("second", 0, -1, 60)
) : Gene(name){

    /*
        Note: would need to handle timezone and second fractions,
        but not sure how important for testing purposes
     */

    override fun copy(): Gene = TimeGene(
            name,
            hour.copy() as IntegerGene,
            minute.copy() as IntegerGene,
            second.copy() as IntegerGene
            )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        hour.randomize(randomness, forceNewValue)
        minute.randomize(randomness, forceNewValue)
        second.randomize(randomness, forceNewValue)
    }

    override fun getValueAsPrintableString(): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString() : String {

        return GeneUtils.let {
            "${it.padded(hour.value,2)}:${it.padded(minute.value,2)}:${it.padded(second.value,2)}.000Z"
        }
    }

    override fun copyValueFrom(other: Gene){
        if(other !is TimeGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.hour.copyValueFrom(other.hour)
        this.minute.copyValueFrom(other.minute)
        this.second.copyValueFrom(other.second)
    }

    override fun flatView(): List<Gene>{
        return listOf(this, hour, minute, second)
    }

}
package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import java.util.*


class Base64StringGene(
        name: String,
        val data: StringGene = StringGene("data")
) : Gene(name) {

    override fun copy(): Gene = Base64StringGene(name, data.copy() as StringGene)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {
        data.randomize(randomness, forceNewValue)
    }

    override fun getValueAsPrintableString(): String {
        return Base64.getEncoder().encodeToString(data.value.toByteArray())
    }

    override fun copyValueFrom(other: Gene){
        if(other !is Base64StringGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.data.copyValueFrom(other.data)
    }
}
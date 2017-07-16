package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


open class ObjectGene(name: String, val fields: List<out Gene>) : Gene(name) {

    override fun copy(): Gene {
        return ObjectGene(name, fields.map(Gene::copy))
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in 0 until fields.size) {
            this.fields[i].copyValueFrom(other.fields[i])
        }
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        fields.forEach { f -> f.randomize(randomness, forceNewValue) }
    }

    override fun getValueAsPrintableString(): String {

        //by default, return in JSON format

        val buffer = StringBuffer()
        buffer.append("{")

        fields.filter { f ->
            f !is CycleObjectGene &&
                    (f !is OptionalGene || f.isActive)
        }.map { f ->
            "\"${f.name}\":${f.getValueAsPrintableString()}"
        }.joinTo(buffer, ", ")

        buffer.append("}")

        return buffer.toString()
    }

    override fun flatView(): List<Gene> {
        return listOf(this).plus(fields.flatMap { g -> g.flatView() })
    }

}
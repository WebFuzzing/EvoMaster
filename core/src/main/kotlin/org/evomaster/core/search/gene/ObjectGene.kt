package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class ObjectGene(name: String, val fields: List<out Gene>) : Gene(name) {

    override fun copy(): Gene {
        return ObjectGene(name, fields.map(Gene::copy))
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        fields.forEach { f -> f.randomize(randomness, forceNewValue) }
    }

    override fun getValueAsString(): String {

        //by default, return in JSON format

        val buffer = StringBuffer()
        buffer.append("{")

        fields.map { f ->
            """
            |"${f.name}"=${f.getValueAsString()}
            """
        }.joinTo(buffer, ",")

        buffer.append("}")

        return buffer.toString()
    }
}
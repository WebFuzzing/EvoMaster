package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * @property refType presents the name of reference type of the object
 */
open class ObjectGene(name: String, val fields: List<out Gene>, val refType : String? = null) : Gene(name) {

    companion object {
        val JSON_MODE = "json"

        val XML_MODE = "xml"

    }
    override fun copy(): Gene {
        return ObjectGene(name, fields.map(Gene::copy), refType)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in 0 until fields.size) {
            this.fields[i].copyValueFrom(other.fields[i])
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.fields.size == other.fields.size
                && this.fields.zip(other.fields) { thisField, otherField ->
            thisField.containsSameValueAs(otherField)
        }.all { it == true }
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        fields.forEach { f -> f.randomize(randomness, forceNewValue, allGenes) }
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(fields.isEmpty()){
            return
        }

        val gene = randomness.choose(fields)
        gene.standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: String?, targetFormat: OutputFormat?) : String{

        val buffer = StringBuffer()

        //by default, return in JSON format
        if (mode == null || mode.equals(JSON_MODE, ignoreCase = true)) {
            buffer.append("{")

            fields.filter {
                it !is CycleObjectGene &&
                        (it !is OptionalGene || it.isActive)
            }.map {
                "\"${it.name}\":${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }.joinTo(buffer, ", ")

            buffer.append("}")

        } else if (mode.equals(XML_MODE, ignoreCase = true)) {

            /*
                Note: this is a very basic support, which should not really depend
                much on. Problem is that we would need to access to the XSD schema
                to decide when fields should be represented with tags or attributes
             */

            buffer.append(openXml(name))
            fields.filter {
                it !is CycleObjectGene &&
                        (it !is OptionalGene || it.isActive)
            }.forEach {
                buffer.append(it.getValueAsPrintableString(previousGenes, mode, targetFormat))
            }

            buffer.append(closeXml(name))
        } else {
            throw IllegalArgumentException("Unrecognized mode: $mode")
        }

        return buffer.toString()
    }

    private fun openXml(tagName: String) = "<$tagName>"

    private fun closeXml(tagName: String) = "</$tagName>"


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(fields.flatMap { g -> g.flatView(excludePredicate) })
    }
}
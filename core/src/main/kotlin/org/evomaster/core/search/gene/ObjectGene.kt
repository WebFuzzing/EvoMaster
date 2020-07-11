package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder

/**
 * @property refType presents the name of reference type of the object
 */
open class ObjectGene(name: String, val fields: List<out Gene>, val refType: String? = null) : Gene(name) {

    companion object {
        val JSON_MODE = "json"

        val XML_MODE = "xml"

        private val log: Logger = LoggerFactory.getLogger(ObjectGene::class.java)

    }

    init {
        for(f in fields){
            f.parent = this
        }
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

    override fun isMutable(): Boolean {
        return fields.any { it.isMutable() }
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

        fields.filter { it.isMutable()}
                .forEach { it.randomize(randomness, forceNewValue, allGenes) }
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return fields.filter { it.isMutable() }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {

        val buffer = StringBuffer()

        val includedFields = fields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        }

        //by default, return in JSON format
        if (mode == null || mode == GeneUtils.EscapeMode.JSON) {
            buffer.append("{")

            includedFields.map {
                "\"${it.name}\":${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }.joinTo(buffer, ", ")

            buffer.append("}")

        } else if (mode == GeneUtils.EscapeMode.XML) {

            // TODO might have to handle here: <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            /*
                Note: this is a very basic support, which should not really depend
                much on. Problem is that we would need to access to the XSD schema
                to decide when fields should be represented with tags or attributes
             */

            buffer.append(openXml(name))
            includedFields.forEach {
                //FIXME put back, but then update all broken tests
                //buffer.append(openXml(it.name))
                buffer.append(it.getValueAsPrintableString(previousGenes, mode, targetFormat))
                //buffer.append(closeXml(it.name))
            }
            buffer.append(closeXml(name))

        } else if (mode == GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED) {

            buffer.append(includedFields.map {
                val name = URLEncoder.encode(it.getVariableName(), "UTF-8")
                val value = URLEncoder.encode(it.getValueAsRawString(), "UTF-8")
                "$name=$value"
            }.joinToString("&"))

        } else {
            throw IllegalArgumentException("Unrecognized mode: $mode")
        }

        return buffer.toString()
    }

    private fun openXml(tagName: String) = "<$tagName>"

    private fun closeXml(tagName: String) = "</$tagName>"


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(fields.flatMap { g -> g.flatView(excludePredicate) })
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): List<Pair<Gene, AdditionalGeneSelectionInfo?>> {

        if (additionalGeneMutationInfo.impact != null
                && additionalGeneMutationInfo.impact is ObjectGeneImpact){
            val impacts = internalGenes.map { additionalGeneMutationInfo.impact.fields.getValue(it.name)}
            val selected = mwc.selectSubGene(
                    internalGenes, true, additionalGeneMutationInfo.targets, individual = null, impacts = impacts, evi = additionalGeneMutationInfo.evi
            )
            val map = selected.map { internalGenes.indexOf(it) }
            return map.map { internalGenes[it] to additionalGeneMutationInfo.copyFoInnerGene(impact = impacts[it] as? GeneImpact) }
        }
        throw IllegalArgumentException("impact is null or not ObjectGeneImpact")
    }


    override fun archiveMutationUpdate(original: Gene, mutated: Gene, targetsEvaluated: Map<Int, EvaluatedMutation>, archiveMutator: ArchiveGeneMutator) {
        original as? ObjectGene ?: throw IllegalStateException("$original should be ObjectGene")
        mutated as? ObjectGene ?: throw IllegalStateException("$mutated should be ObjectGene")

        mutated.fields.zip(original.fields) { cf, pf ->
            Pair(Pair(cf, pf), cf.containsSameValueAs(pf))
        }.filter { !it.second }.map { it.first }.forEach { g ->
            val current = fields.find { it.name == g.first.name }
                    ?: throw IllegalArgumentException("mismatched field")
            current.archiveMutationUpdate(original = g.second, mutated = g.first, targetsEvaluated = targetsEvaluated, archiveMutator = archiveMutator)
        }
    }

    override fun reachOptimal(targets: Set<Int>): Boolean {
        return fields.all { it.reachOptimal(targets) }
    }

    override fun mutationWeight(): Double = fields.map { it.mutationWeight() }.sum()

}
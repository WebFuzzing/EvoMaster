package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.value.ObjectGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @property refType presents the name of reference type of the object
 */
open class ObjectGene(name: String, val fields: List<out Gene>, val refType : String? = null) : Gene(name) {

    companion object {
        val JSON_MODE = "json"

        val XML_MODE = "xml"

        private val log: Logger = LoggerFactory.getLogger(ObjectGene::class.java)

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

    /**
     * @param geneImpact null is only allowed when the gene is root.
     */
    override fun archiveMutation(
            randomness: Randomness,
            allGenes: List<Gene>,
            apc: AdaptiveParameterControl,
            selection: ImpactMutationSelection,
            impact: GeneImpact?,
            geneReference : String,
            archiveMutator: ArchiveMutator,
            evi: EvaluatedIndividual<*>) {

        if (!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }

        val canFields = fields.filter { !it.reachOptimal() || !archiveMutator.withinNormal()}.run {
            if (isEmpty())
                fields
            else this
        }
        var genes : List<Pair<Gene, Impact>>? = null
        val selects =  if (impact != null && impact is ObjectGeneImpact && archiveMutator.applyArchiveSelection()){
            genes = canFields.map { Pair(it, impact.fields.getValue(it.name)) }
            archiveMutator.selectGenesByArchive(genes, 1.0/canFields.size)
        }else canFields

        val selected = randomness.choose(if (selects.isNotEmpty()) selects else canFields)
        val selectedImpact = genes?.first { it.first == selected }?.second as? GeneImpact
        selected.archiveMutation(randomness, allGenes, apc, selection, selectedImpact, geneReference,archiveMutator, evi)
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            original as? ObjectGene ?:throw IllegalStateException("$original should be ObjectGene")
            mutated as? ObjectGene ?:throw IllegalStateException("$mutated should be ObjectGene")

            mutated.fields.zip(original.fields) { cf, pf ->
                Pair(Pair(cf, pf), cf.containsSameValueAs(pf))
            }.filter { !it.second }.map { it.first }.forEach { g->
                val current = fields.find { it.name ==  g.first.name}?: throw IllegalArgumentException("mismatched field")
                current.archiveMutationUpdate(original = g.second, mutated = g.first, doesCurrentBetter = doesCurrentBetter, archiveMutator = archiveMutator)
            }
        }
    }

    override fun reachOptimal(): Boolean {
        return fields.all { it.reachOptimal() }
    }

}
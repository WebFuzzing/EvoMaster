package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class Base64StringGene(
        name: String,
        val data: StringGene = StringGene("data")
) : Gene(name, mutableListOf(data)) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(Base64StringGene::class.java)
    }

    override fun getChildren(): MutableList<StringGene> = mutableListOf(data)

    override fun copyContent(): Gene = Base64StringGene(name, data.copyContent() as StringGene)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        data.randomize(randomness, forceNewValue)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return listOf(data)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return Base64.getEncoder().encodeToString(data.value.toByteArray())
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.data.copyValueFrom(other.data)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.data.containsSameValueAs(other.data)
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if(excludePredicate(this)) listOf(this) else listOf(this).plus(data.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when(gene){
            is Base64StringGene -> data.bindValueBasedOn(gene.data)
            is StringGene -> data.bindValueBasedOn(gene)
            else->{
                LoggingUtil.uniqueWarn(log, "cannot bind the Base64StringGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }
}
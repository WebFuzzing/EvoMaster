package org.evomaster.core.search.gene.uri

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.CompositeFixedGene
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


/**
 * Form:
 * <user>:<password>@<host>:<port>/<url-path>
 */
class UrlHttpGene(
    name: String,
    val scheme : EnumGene<String> = EnumGene("scheme", listOf("http","https"))

) : CompositeFixedGene(name, mutableListOf()) {


    /*



   //
     */
    override fun copyContent(): Gene {
        TODO("Not yet implemented")
    }

    override fun isLocallyValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        TODO("Not yet implemented")
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        TODO("Not yet implemented")
    }

    override fun innerGene(): List<Gene> {
        TODO("Not yet implemented")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        TODO("Not yet implemented")
    }

    override fun copyValueFrom(other: Gene) {
        TODO("Not yet implemented")
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        TODO("Not yet implemented")
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        TODO("Not yet implemented")
    }


}
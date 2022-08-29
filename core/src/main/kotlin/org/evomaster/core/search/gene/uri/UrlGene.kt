package org.evomaster.core.search.gene.uri

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


/**
 * https://www.rfc-editor.org/rfc/rfc1738
 *
 * A URI is a set including URL and URN.
 * All URLs are URIs, but not vice-versa.
 * For example, in URIs the "scheme" is optional, whereas it is
 * mandatory for URLs.
 *
 * For time being, we treat all URIs as URLs.
 * Maybe in future we ll make distinction for URN.
 *
 * Also based on the scheme type, the syntax of URL changes.
 *    <scheme>:<scheme-specific-part>
 * TODO might want to support all protocols as well in future, but many seem they are seldom used
 *
 *     ftp                     File Transfer protocol
 *     http                    Hypertext Transfer Protocol
 *     gopher                  The Gopher protocol
 *     mailto                  Electronic mail address
 *     news                    USENET news
 *     nntp                    USENET news using NNTP access
 *     telnet                  Reference to interactive sessions
 *     wais                    Wide Area Information Servers
 *     file                    Host-specific file names
 *     prospero                Prospero Directory Service
 *
 *     Another one is "data"
 */
class UrlGene(name: String,
            val gene : ChoiceGene<Gene> = ChoiceGene(name, listOf(
                UrlHttpGene(name), UrlDataGene(name)
            ))
              ) : CompositeFixedGene(name, mutableListOf(gene)) {

    override fun copyContent(): Gene {
       return UrlGene(name, gene.copy() as ChoiceGene<Gene>)
    }

    override fun isLocallyValid(): Boolean {
        return gene.isLocallyValid()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        gene.randomize(randomness, tryToForceNewValue)
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(gene)
    }

    override fun innerGene(): List<Gene> {
        return listOf(gene)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is UrlGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is UrlGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return gene.containsSameValueAs(other.gene)
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        return gene.bindValueBasedOn(gene)
    }
}
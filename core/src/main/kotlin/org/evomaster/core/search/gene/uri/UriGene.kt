package org.evomaster.core.search.gene.uri

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.optional.ChoiceGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy


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
 *     Another one is "data", but it is not considered a valid URL scheme (at least for JDK).
 *     Need to use URI for it
 */
class UriGene(name: String,
              val gene : ChoiceGene<Gene> = ChoiceGene(name, listOf(
                UrlHttpGene(name), UriDataGene(name)
            ))
              ) : CompositeFixedGene(name, mutableListOf(gene)) {

    override fun copyContent(): Gene {
       return UriGene(name, gene.copy() as ChoiceGene<Gene>)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        gene.randomize(randomness, tryToForceNewValue)
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is UriGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid({gene.copyValueFrom(other.gene)}, false)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is UriGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return gene.containsSameValueAs(other.gene)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return gene.setValueBasedOn(gene)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}
package org.evomaster.core.search.gene.uri

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/**
 * https://www.rfc-editor.org/rfc/rfc1738
 *
 * A URI is a set including URL and URN.
 * All URLs are URIs, but not vice-versa.
 *
 * For time being, we treat all URIs as URLs.
 * Maybe in future we ll make distinction for URN.
 *
 * Also based on the scheme type, the syntax of URL changes.
 * For now, we just deal with HTTP(S).
 * TODO might want to extend to other protocols as well in future, but seems they are seldom used
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
 */
class UrlHttpGene(
    name: String

) : CompositeFixedGene(name) {


    /*

   <scheme>:<scheme-specific-part>

   //<user>:<password>@<host>:<port>/<url-path>
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
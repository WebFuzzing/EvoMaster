package org.evomaster.core.search.gene.cassandra

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * A value of one of the Cassandra collection types, ie a list, a set or a map.
 *
 * The [kind] is kept explicitly because the CQL literals of a list and of a set are built from the
 * same kind of gene but written with different delimiters, and so could not be told apart otherwise.
 */
class CqlCollectionGene(
    name: String,
    val kind: CqlCollectionKind,
    /**
     * The elements of the collection, held in the gene handling that kind of collection, ie an
     * ArrayGene for a list and a set, and a FixedMapGene for a map. All of the behaviour of this
     * gene during the search is delegated to it.
     */
    val content: Gene
) : CompositeFixedGene(name, mutableListOf(content)) {

    init {
        if (!kind.isValidContent(content)) {
            throw IllegalArgumentException("The elements of a CQL ${kind.cqlName} cannot be held in" +
                    " a ${content.javaClass.simpleName}")
        }
    }

    override fun copyContent(): Gene = CqlCollectionGene(name, kind, content.copy())

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        content.randomize(randomness, tryToForceNewValue)
    }

    /**
     * Note that this is not how the collection is written in a CQL statement, as each of its
     * elements would have to be written the way a CQL literal of its own type is. Building that
     * literal is the job of CassandraLiteralRenderer, which recurses over [content].
     */
    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return content.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    override fun getValueAsRawString(): String {
        return content.getValueAsRawString()
    }

    override fun isPrintable(): Boolean {
        return content.isPrintable()
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is CqlCollectionGene || other.kind != this.kind) {
            return false
        }

        return this.content.unsafeCopyValueFrom(other.content)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is CqlCollectionGene || other.kind != this.kind) {
            return false
        }

        return this.content.containsSameValueAs(other.content)
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
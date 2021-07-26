package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlPrimaryKeyGeneImpact
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A wrapper around a Gene to represent a column in a SQL database
 * where such column is a Primary Key.
 * This is important to check Foreign Keys referencing it.
 */
class SqlPrimaryKeyGene(name: String,
                        val tableName: String,
                        val gene: Gene,
                        /**
                         * Important for the Foreign Keys referencing it.
                         * Cannot be negative
                         */
                        val uniqueId: Long
) : SqlWrapperGene(name, mutableListOf(gene)) {


    init {
        if (uniqueId < 0) {
            throw IllegalArgumentException("Negative unique id")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlPrimaryKeyGene::class.java)
    }

    override fun getForeignKey(): SqlForeignKeyGene? {
        if(gene is SqlWrapperGene){
            return gene.getForeignKey()
        }
        return null
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(gene)

    override fun copyContent() = SqlPrimaryKeyGene(name, tableName, gene.copyContent(), uniqueId)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, false, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return if (isMutable()) listOf(gene) else emptyList()
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlPrimaryKeyGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not SqlPrimaryKeyGeneImpact")

    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        //do nothing since the gene is not mutable
        return true
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.gene.containsSameValueAs(other.gene)
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun isMutable() = gene.isMutable()

    override fun isPrintable() = gene.isPrintable()


    fun isReferenceToNonPrintable(previousGenes: List<Gene>): Boolean {
        if (gene !is SqlForeignKeyGene) {
            return false
        }

        return gene.isReferenceToNonPrintable(previousGenes)
    }

    override fun innerGene(): List<Gene> = listOf(gene)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
    }

}
package org.evomaster.core.search.gene.sql

import org.evomaster.core.Lazy
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlPrimaryKeyGeneImpact
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
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
) : SqlWrapperGene, CompositeGene(name, mutableListOf(gene)) {


    init {
        if (uniqueId < 0) {
            throw IllegalArgumentException("Negative unique id")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlPrimaryKeyGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun getForeignKey(): SqlForeignKeyGene? {
        if(gene is SqlWrapperGene){
            return gene.getForeignKey()
        }
        return null
    }

    override fun copyContent() = SqlPrimaryKeyGene(name, tableName, gene.copy(), uniqueId)

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        if(gene.isMutable())
            gene.randomize(randomness, false)
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlPrimaryKeyGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(gene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain gene")
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not SqlPrimaryKeyGeneImpact")

    }


    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.gene.copyValueFrom(other.gene)}, false
        )
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



    override fun isMutable() = gene.isMutable()

    override fun isPrintable() = gene.isPrintable()


    fun isReferenceToNonPrintable(previousGenes: List<Gene>): Boolean {
        if (gene !is SqlForeignKeyGene) {
            return false
        }

        return gene.isReferenceToNonPrintable(previousGenes)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
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
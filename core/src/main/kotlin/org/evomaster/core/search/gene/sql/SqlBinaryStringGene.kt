package org.evomaster.core.search.gene.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Binary strings are strings of 1's and 0's.
 */
class SqlBinaryStringGene(
    /**
         * The name of this gene
         */
        name: String,

    val minSize: Int = 0,

    val maxSize: Int = ArrayGene.MAX_SIZE,

    private val binaryArrayGene: ArrayGene<IntegerGene> = ArrayGene(name, template = IntegerGene(name, min = 0, max = 255), minSize = minSize, maxSize = maxSize),

    val databaseType: DatabaseType = DatabaseType.POSTGRES

) :  CompositeFixedGene(name, mutableListOf( binaryArrayGene)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlBinaryStringGene::class.java)

        const val EMPTY_STR = ""

    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        binaryArrayGene.randomize(randomness, tryToForceNewValue)
    }

    override fun mutablePhenotypeChildren(): List<Gene> {
        return listOf(binaryArrayGene)
    }

    private fun toHex2(value: Int) = value.toString(16).padStart(2, '0')


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val hexString = binaryArrayGene.getViewOfChildren().map { g ->
            toHex2((g as IntegerGene).value)
        }.joinToString(EMPTY_STR)

        return when (databaseType) {
            DatabaseType.H2,
            DatabaseType.MYSQL -> "X${GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER}${hexString}${GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER}"
            DatabaseType.POSTGRES -> "\"\\x${hexString}\""
            else -> throw IllegalArgumentException("getValueAsPrintableString() not supported for ${databaseType}")
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlBinaryStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {binaryArrayGene.copyValueFrom(other.binaryArrayGene)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlBinaryStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return binaryArrayGene.containsSameValueAs(other.binaryArrayGene)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlBinaryStringGene) {
            return binaryArrayGene.setValueBasedOn(gene.binaryArrayGene)
        }
        LoggingUtil.uniqueWarn(log, "cannot bind SqlBitstringGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun copyContent() = SqlBinaryStringGene(name,
            minSize = minSize,
            maxSize = maxSize,
            binaryArrayGene.copy() as ArrayGene<IntegerGene>,
            databaseType=databaseType)

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}
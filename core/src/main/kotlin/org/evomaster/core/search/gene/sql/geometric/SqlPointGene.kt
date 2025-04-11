package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlPointGene(
    name: String,
    val x: FloatGene = FloatGene(name = "x"),
    val y: FloatGene = FloatGene(name = "y"),
    val databaseType: DatabaseType = DatabaseType.POSTGRES
) : CompositeFixedGene(name, mutableListOf(x, y)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPointGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlPointGene(
            name,
            x.copy() as FloatGene,
            y.copy() as FloatGene,
            databaseType = databaseType
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        x.randomize(randomness, tryToForceNewValue)
        y.randomize(randomness, tryToForceNewValue)
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.POSTGRES,
            DatabaseType.H2 -> "\"${getValueAsRawString()}\""
            DatabaseType.MYSQL -> getValueAsRawString()
            else ->
                throw IllegalArgumentException("SqlPointGene.getValueAsPrintableString is not supported for databasetype: ${databaseType}")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.H2 -> "POINT(${x.getValueAsRawString()} ${y.getValueAsRawString()})"
            DatabaseType.MYSQL -> "POINT(${x.getValueAsRawString()}, ${y.getValueAsRawString()})"
            DatabaseType.POSTGRES -> "(${x.getValueAsRawString()}, ${y.getValueAsRawString()})"
            else ->
                throw IllegalArgumentException("SqlPointGene.getValueAsPrintableString is not supported for databasetype: ${databaseType}")
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.x.copyValueFrom(other.x)
                    && this.y.copyValueFrom(other.y)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.x.containsSameValueAs(other.x)
                && this.y.containsSameValueAs(other.y)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPointGene -> {
                x.setValueBasedOn(gene.x) &&
                        y.setValueBasedOn(gene.y)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
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
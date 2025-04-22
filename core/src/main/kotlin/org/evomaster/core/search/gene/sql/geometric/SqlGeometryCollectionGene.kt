package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a collection of geometric objects.
 */
class SqlGeometryCollectionGene(
        name: String,
        /**
         * The database type of the source column for this gene
         */
        val databaseType: DatabaseType = DatabaseType.H2,
        template: Gene,
        val elements: ArrayGene<Gene> = ArrayGene(
                name = "points",
                minSize = 1,
                template = template)
) : CompositeFixedGene(name, mutableListOf(elements)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlGeometryCollectionGene::class.java)
    }

    override fun copyContent(): Gene {
        val newElements = elements.copy() as ArrayGene<Gene>
        return SqlGeometryCollectionGene(
                name,
                databaseType = this.databaseType,
                template = newElements.template,
                elements = newElements
        )
    }

    override fun checkForLocallyValidIgnoringChildren()= true


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        elements.randomize(randomness, tryToForceNewValue)
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.MYSQL -> getValueAsRawString()
            DatabaseType.H2 -> "\"${getValueAsRawString()}\""
            else -> throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsPrintableString() for $databaseType")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.MYSQL,
            DatabaseType.H2 -> {
                if (elements.getViewOfElements().isEmpty()) "GEOMETRYCOLLECTION EMPTY"
                else
                    "GEOMETRYCOLLECTION(${
                        elements.getViewOfElements().joinToString(", ") {
                            it.getValueAsRawString()
                        }
                    })"
            }
            else -> throw IllegalArgumentException("Unsupported SqlGeometryCollectionGene.getValueAsRawString() for $databaseType")

        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlGeometryCollectionGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.elements.copyValueFrom(other.elements)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlGeometryCollectionGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.containsSameValueAs(other.elements)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when (gene) {
            is SqlGeometryCollectionGene -> {
                elements.setValueBasedOn(gene.elements)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
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
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
 * Represents a collection of polygons.
 * It is intended to hold values for Column types MULTIPOLYGON
 * of databases H2 and MySQL.
 */
class SqlMultiPolygonGene(
        name: String,
        /**
         * The database type of the source column for this gene
         */
        val databaseType: DatabaseType = DatabaseType.H2,
        val polygons: ArrayGene<SqlPolygonGene> = ArrayGene(
                name = "polygons",
                minSize = 1,
                template = SqlPolygonGene("polygon",
                        minLengthOfPolygonRing = 3,
                        databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(polygons)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPolygonGene::class.java)
    }

    init {
        if (databaseType!=DatabaseType.H2 && databaseType!=DatabaseType.MYSQL) {
            IllegalArgumentException("Cannot create a SqlMultiPolygonGene with database type ${databaseType}")
        }
    }
    override fun copyContent(): Gene = SqlMultiPolygonGene(
            name,
            databaseType = this.databaseType,
            polygons = polygons.copy() as ArrayGene<SqlPolygonGene>
    )

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        polygons.randomize(randomness, tryToForceNewValue)
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2 -> "\"${getValueAsRawString()}\""
            DatabaseType.MYSQL -> getValueAsRawString()
            else -> throw IllegalArgumentException("Unsupported SqlMultiPolygonGene.getValueAsPrintableString() for $databaseType")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                if (polygons.getViewOfElements().isEmpty()) "MULTIPOLYGON EMPTY"
                else
                    "MULTIPOLYGON(${
                        polygons.getViewOfElements().joinToString(", ") { polygon ->
                            "((${
                                polygon.points.getViewOfElements().joinToString(", ") { point ->
                                    point.x.getValueAsRawString() +
                                            " " + point.y.getValueAsRawString()
                                } + ", " + polygon.points.getViewOfElements()[0].x.getValueAsRawString() +
                                        " " + polygon.points.getViewOfElements()[0].y.getValueAsRawString()
                            }))"
                        }
                    })"
            }
            DatabaseType.MYSQL -> {
                "MULTIPOLYGON(${polygons.getViewOfElements().joinToString(", "){ it.getValueAsRawString() }})"
            }
            else -> throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsRawString() for $databaseType")

        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlMultiPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.polygons.copyValueFrom(other.polygons)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.polygons.containsSameValueAs(other.polygons)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPolygonGene -> {
                polygons.setValueBasedOn(gene.polygons)
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
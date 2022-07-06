package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a collection of polygons.
 * It is intended to hold values for Column types MULTIPOINT
 * of databases H2 and PostgreSQL.
 */
class SqlMultiPolygonGene(
        name: String,
        /**
         * The database type of the source column for this gene
         */
        val databaseType: DatabaseType = DatabaseType.POSTGRES,
        val polygons: ArrayGene<SqlPolygonGene> = ArrayGene(
                name = "polygons",
                minSize = 0,
                template = SqlPolygonGene("polygon",
                        minLengthOfPolygonRing = 3,
                        databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(polygons)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPolygonGene::class.java)
    }

    override fun copyContent(): Gene = SqlMultiPolygonGene(
            name,
            databaseType = this.databaseType,
            polygons = polygons.copy() as ArrayGene<SqlPolygonGene>
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        polygons.randomize(randomness, tryToForceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(polygons)
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                if (polygons.getViewOfElements().isEmpty()) "\"MULTIPOLYGON EMPTY\""
                else
                    "\"MULTIPOLYGON(${
                        polygons.getViewOfElements().joinToString(", ") {
                            polygon -> 
                            "((${polygon.points.getViewOfElements().joinToString(", ") {
                                point -> 
                                point.x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                        " " + point.y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                            } + ", " + polygon.points.getViewOfElements().get(0).x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                    " " + polygon.points.getViewOfElements().get(0).y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                            }))"
                        }
                    })\""
            }
            else -> {
                throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsPrintableString() for $databaseType")
            }
        }
    }

    override fun getValueAsRawString(): String {
        return "( ${
            polygons.getViewOfElements()
                    .map { it.getValueAsRawString() }
                    .joinToString(" , ")
        } ) "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultiPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.polygons.copyValueFrom(other.polygons)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.polygons.containsSameValueAs(other.polygons)
    }


    override fun innerGene(): List<Gene> = listOf(polygons)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPolygonGene -> {
                polygons.bindValueBasedOn(gene.polygons)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

}
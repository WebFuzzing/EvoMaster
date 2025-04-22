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
 * A LineString/Path is a Polyline or from Wikipedia "a curve specified by the sequence of points".
 * It must have at least two points to be valid.
 */
class SqlPathGene(
        name: String,
        val databaseType: DatabaseType = DatabaseType.POSTGRES,
        val points: ArrayGene<SqlPointGene> = ArrayGene(
                name = "points",
                // paths are lists of at least 2 points
                minSize = 2,
                template = SqlPointGene("p", databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(points)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPathGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlPathGene(
            name,
            points = points.copy() as ArrayGene<SqlPointGene>,
            databaseType = this.databaseType
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        points.randomize(randomness, tryToForceNewValue)
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2,
            DatabaseType.POSTGRES -> "\"${this.getValueAsRawString()}\""
            DatabaseType.MYSQL -> this.getValueAsRawString()
            else ->throw IllegalArgumentException("Unsupported SqlPathGene.getValueAsPrintableString() for $databaseType")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.POSTGRES -> {
                "(${
                    points.getViewOfElements().joinToString(", ") { it.getValueAsRawString() }
                })"
            }
            DatabaseType.H2 -> {
                if (points.getViewOfElements().isEmpty()) "LINESTRING EMPTY"
                else
                    "LINESTRING(${
                        points.getViewOfElements().joinToString(", ") {
                            it.x.getValueAsRawString() +
                                    " " + it.y.getValueAsRawString()
                        }
                    })"
            }
            DatabaseType.MYSQL -> {
                "LINESTRING(${
                    points.getViewOfElements()
                            .joinToString(", ")
                            { it.getValueAsRawString() }
                })"
            }
            else -> throw IllegalArgumentException("Unsupported SqlPathGene.getValueAsRawString() for $databaseType")
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.points.copyValueFrom(other.points)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPathGene -> {
                points.setValueBasedOn(gene.points)
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
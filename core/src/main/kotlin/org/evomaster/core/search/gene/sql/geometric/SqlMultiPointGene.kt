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
 * Represents a collection of points without lines between them.
 * Column types MULTIPOINT of databases H2 and MYSQL are supported by
 * this gene.
 */
class SqlMultiPointGene(
    name: String,
    /**
     * The database type of the source column for this gene
     */
    val databaseType: DatabaseType = DatabaseType.H2,
    val points: ArrayGene<SqlPointGene> = ArrayGene(
        name = "points",
        minSize = 1, //looks like insertion fails if 0, due to "bad" syntax
        template = SqlPointGene("p", databaseType = databaseType)
    )
) : CompositeFixedGene(name, mutableListOf(points)) {

    init {
        if (databaseType!=DatabaseType.H2 && databaseType!=DatabaseType.MYSQL) {
            IllegalArgumentException("Cannot create a SqlMultiPointGene with database type ${databaseType}")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPointGene::class.java)
    }

    override fun copyContent(): Gene = SqlMultiPointGene(
        name,
        databaseType = this.databaseType,
        points = points.copy() as ArrayGene<SqlPointGene>
    )

    override fun checkForLocallyValidIgnoringChildren() = true

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
            DatabaseType.H2 -> "\"${getValueAsRawString()}\""
            DatabaseType.MYSQL -> getValueAsRawString()
            else -> throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsPrintableString() for $databaseType")

        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                if (points.getViewOfElements().isEmpty()) "MULTIPOINT EMPTY"
                else
                    "MULTIPOINT(${
                        points.getViewOfElements().joinToString(", ") {
                            it.x.getValueAsRawString() + " " + it.y.getValueAsRawString()
                        }
                    })"
            }
            DatabaseType.MYSQL -> {
                "MULTIPOINT(${
                    points.getViewOfElements().joinToString(", ") {
                        it.getValueAsRawString()
                    }
                })"
            }
            else -> throw IllegalArgumentException("Unsupported SqlMultiPointGene.getValueAsRawString() for $databaseType")

        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlMultiPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid({this.points.copyValueFrom(other.points)}, false)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPointGene -> {
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
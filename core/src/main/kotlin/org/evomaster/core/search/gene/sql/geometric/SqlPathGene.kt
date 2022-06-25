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

class SqlPathGene(
        name: String,
        val databaseType: DatabaseType = DatabaseType.POSTGRES,
        val points: ArrayGene<SqlPointGene> = ArrayGene(
                name = "points",
                // paths are lists of at least 2 points
                minSize = 2,
                template = SqlPointGene("p", databaseType = databaseType))
) : Gene(name, mutableListOf(points)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPathGene::class.java)
    }


    override fun getChildren(): MutableList<Gene> = mutableListOf(points)

    override fun copyContent(): Gene = SqlPathGene(
            name,
            points = points.copyContent() as ArrayGene<SqlPointGene>,
            databaseType = this.databaseType
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        points.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(points)
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                "\"LINESTRING(${
                    points.getAllElements().joinToString(" , ") {
                        it.x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                " " + it.y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                    }
                })\""
            }
            DatabaseType.POSTGRES -> {
                "\" ( ${
                    points.getAllElements().joinToString(" , ") { it.getValueAsRawString() }
                } ) \""
            }
            DatabaseType.MYSQL -> {
                "LINESTRING(${
                    points.getAllElements()
                            .joinToString(" , ")
                            { it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) }
                })"
            }
            else -> {
                throw IllegalArgumentException("Unsupported SqlPathGene.getValueAsPrintableString() for $databaseType")
            }
        }
    }

    override fun getValueAsRawString(): String {
        return "( ${
            points.getAllElements()
                    .map { it.getValueAsRawString() }
                    .joinToString(" , ")
        } ) "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.points.copyValueFrom(other.points)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(points.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(points)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPathGene -> {
                points.bindValueBasedOn(gene.points)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

}
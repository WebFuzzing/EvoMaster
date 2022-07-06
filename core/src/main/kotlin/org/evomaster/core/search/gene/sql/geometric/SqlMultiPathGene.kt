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

class SqlMultiPathGene(
        name: String,
        /**
         * The database type of the source column for this gene
         */
        val databaseType: DatabaseType = DatabaseType.POSTGRES,
        val paths: ArrayGene<SqlPathGene> = ArrayGene(
                name = "points",
                minSize = 0,
                template = SqlPathGene("p", databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(paths)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPathGene::class.java)
    }

    override fun copyContent(): Gene = SqlMultiPathGene(
            name,
            databaseType = this.databaseType,
            paths = paths.copy() as ArrayGene<SqlPathGene>
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        paths.randomize(randomness, tryToForceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(paths)
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                if (paths.getViewOfElements().isEmpty())
                    "\"MULTILINESTRING EMPTY\""
                else
                    "\"MULTILINESTRING(${
                        paths.getViewOfElements().joinToString(", ") { path ->
                            if (path.points.getViewOfElements().isEmpty()) {
                                "EMPTY"
                            } else {
                                "(" + path.points.getViewOfElements().joinToString(", ") { point ->
                                    point.x.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                                            " " + point.y.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                                } + ")"
                            }
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
            paths.getViewOfElements()
                    .map { it.getValueAsRawString() }
                    .joinToString(" , ")
        } ) "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultiPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.paths.copyValueFrom(other.paths)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.paths.containsSameValueAs(other.paths)
    }


    override fun innerGene(): List<Gene> = listOf(paths)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPathGene -> {
                paths.bindValueBasedOn(gene.paths)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

}
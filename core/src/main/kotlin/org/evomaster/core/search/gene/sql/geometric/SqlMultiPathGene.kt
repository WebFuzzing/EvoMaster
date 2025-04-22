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

class SqlMultiPathGene(
    name: String,
    /**
     * The database type of the source column for this gene
     */
    val databaseType: DatabaseType = DatabaseType.MYSQL,
    val paths: ArrayGene<SqlPathGene> = ArrayGene(
        name = "points",
        minSize = 1,
        template = SqlPathGene("p", databaseType = databaseType)
    )
) : CompositeFixedGene(name, mutableListOf(paths)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiPathGene::class.java)
    }

    override fun copyContent(): Gene = SqlMultiPathGene(
        name,
        databaseType = this.databaseType,
        paths = paths.copy() as ArrayGene<SqlPathGene>
    )

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        paths.randomize(randomness, tryToForceNewValue)
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.H2,
            DatabaseType.POSTGRES -> "\"${getValueAsRawString()}\""
            DatabaseType.MYSQL -> getValueAsRawString()
            else -> throw IllegalArgumentException("Unsupported getValueAsPrintableString() for database type: $databaseType")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            DatabaseType.H2 -> {
                if (paths.getViewOfElements().isEmpty())
                    "MULTILINESTRING EMPTY"
                else
                    "MULTILINESTRING(${
                        paths.getViewOfElements().joinToString(", ") { path ->
                            if (path.points.getViewOfElements().isEmpty()) {
                                "EMPTY"
                            } else {
                                "(" + path.points.getViewOfElements().joinToString(", ") { point ->
                                    point.x.getValueAsRawString() +
                                            " " + point.y.getValueAsRawString()
                                } + ")"
                            }
                        }
                    })"
            }
            DatabaseType.MYSQL -> {
                "MULTILINESTRING(${
                    paths.getViewOfElements().joinToString(", ") {
                        it.getValueAsRawString()
                    }
                })"
            }
            else -> throw IllegalArgumentException("Unsupported getValueAsRawString() for database type: $databaseType")

        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlMultiPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.paths.copyValueFrom(other.paths)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.paths.containsSameValueAs(other.paths)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiPathGene -> {
                paths.setValueBasedOn(gene.paths)
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
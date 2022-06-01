package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlPolygonGene(
    name: String,
    val points: ArrayGene<SqlPointGene> = ArrayGene(
            name = "points",
            // polygons have at least 2 points
            minSize = 2,
            template = SqlPointGene("p"))
) : CompositeFixedGene(name, mutableListOf(points)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPolygonGene::class.java)
    }

    override fun copyContent(): Gene = SqlPolygonGene(
        name,
        points.copy() as ArrayGene<SqlPointGene>
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        points.randomize(randomness, tryToForceNewValue, allGenes)
        /*
         *  A geometric polygon must be always a non-empty list
         */
        if (points.getAllElements().isEmpty()) {
            points.addElement(SqlPointGene("p"))
        }
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
        return "\" (  ${
            points.getAllElements()
                .map { it.getValueAsRawString() }
                .joinToString(" , ")
        } ) \""
    }

    override fun getValueAsRawString(): String {
        return "( ${
            points.getAllElements()
                .map { it.getValueAsRawString() }
                .joinToString(" , ")
        } ) "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.points.copyValueFrom(other.points)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }



    override fun innerGene(): List<Gene> = listOf(points)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPolygonGene -> {
                points.bindValueBasedOn(gene.points)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}
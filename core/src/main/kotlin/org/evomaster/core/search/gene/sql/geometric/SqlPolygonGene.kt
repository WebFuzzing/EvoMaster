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
import java.awt.geom.Line2D

class SqlPolygonGene(
    name: String,
    val databaseType: DatabaseType = DatabaseType.POSTGRES,
    val minLengthOfPolygonRing: Int = 2,
    val onlyNonIntersectingPolygons: Boolean = false,
    val points: ArrayGene<SqlPointGene> = ArrayGene(
            name = "points",
            // polygons have at least 2 points
            minSize = minLengthOfPolygonRing,
            template = SqlPointGene("p", databaseType = databaseType))
) : CompositeFixedGene(name, mutableListOf(points)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPolygonGene::class.java)
    }

    override fun copyContent(): Gene = SqlPolygonGene(
        name,
        minLengthOfPolygonRing = minLengthOfPolygonRing,
        onlyNonIntersectingPolygons = onlyNonIntersectingPolygons,
        points = points.copy() as ArrayGene<SqlPointGene>,
        databaseType = databaseType
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        do {
            points.randomize(randomness, tryToForceNewValue, allGenes)
        } while (!isValid())
    }

    /**
     * If [onlyNonIntersectingPolygons] is enabled, checks if the
     * linestring has an intersection by checking all segments
     * againts each other O(n^2)
     * Source: https://stackoverflow.com/questions/4876065/is-there-an-easy-and-fast-way-of-checking-if-a-polygon-is-self-intersecting
     */
    override fun isValid(): Boolean {
        if (!onlyNonIntersectingPolygons)
            return true

        val len = points.getViewOfChildren().size

        /*
         * No cross-over if len < 4
         */
        if (len < 4) {
            return true
        }

        for (i in 0 until (len - 1)) {
            for (j in (i + 2) until len) {
                /*
                 * Eliminate combinations already checked
                 * or not valid
                 */
                if ((i == 0) && (j == (len - 1))) {
                    continue
                }

                val cut = Line2D.linesIntersect(
                        points.getAllElements()[i].x.value.toDouble(),
                        points.getAllElements()[i].y.value.toDouble(),
                        points.getAllElements()[i + 1].x.value.toDouble(),
                        points.getAllElements()[i + 1].y.value.toDouble(),
                        points.getAllElements()[j].x.value.toDouble(),
                        points.getAllElements()[j].y.value.toDouble(),
                        points.getAllElements()[(j + 1) % len].x.value.toDouble(),
                        points.getAllElements()[(j + 1) % len].y.value.toDouble())

                if (cut) {
                    return false
                }
            }
        }
        return true
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
            /*
             * In MySQL, the first and the last point of
             * the polygon should be equal. We enforce
             * this by repeating the first point at the
             * end of the list
             */
            DatabaseType.MYSQL -> {
                "POLYGON(" + points.getAllElements()
                        .joinToString(" , ")
                        { it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) } + "," +
                        points.getAllElements().get(0).getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) +
                        ")"
            }
            /*
             * In PostgreSQL, the (..) denotes a closed path
             */
            DatabaseType.POSTGRES -> {
                "\" (  ${
                    points.getAllElements().joinToString(" , ")
                    { it.getValueAsRawString() }
                } ) \""
            }
            else -> {
                throw IllegalArgumentException("Unsupported SqlPolygonGene.getValueAsPrintableString() for ${databaseType}")
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
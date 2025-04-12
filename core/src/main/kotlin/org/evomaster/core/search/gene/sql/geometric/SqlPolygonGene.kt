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
import java.awt.geom.Line2D

/**
 * Represents a polygon (closed area).
 * It does not contain more than a single linestring.
 */
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

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        val pointList = mutableListOf<SqlPointGene>()
        repeat(minLengthOfPolygonRing) {
            val newGene = points.template.copy() as SqlPointGene
            pointList.add(newGene)
            do {
                newGene.randomize(randomness, tryToForceNewValue)
            } while (onlyNonIntersectingPolygons && !noCrossOvers(pointList))
        }
        points.randomize(randomness, tryToForceNewValue)
        points.killAllChildren()
        pointList.map { points.addChild(it) }
        assert(isLocallyValid())
    }

    /**
     * If [onlyNonIntersectingPolygons] is enabled, checks if the
     * linestring has an intersection by checking all segments
     * againts each other O(n^2)
     * Source: https://stackoverflow.com/questions/4876065/is-there-an-easy-and-fast-way-of-checking-if-a-polygon-is-self-intersecting
     */
    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        if (!onlyNonIntersectingPolygons)
            return true
        val pointList = points.getViewOfChildren() as List<SqlPointGene>
        return noCrossOvers(pointList)
    }

    /**
     * checks if the
     * linestring has an intersection by checking all segments
     * againts each other O(n^2)
     * Source: https://stackoverflow.com/questions/4876065/is-there-an-easy-and-fast-way-of-checking-if-a-polygon-is-self-intersecting
     */
    private fun noCrossOvers(pointList: List<SqlPointGene>): Boolean {
        val len = pointList.size

        /*
         * No cross-over if len <= 3
         */
        if (len <= 3) {
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
                        pointList[i].x.value.toDouble(),
                        pointList[i].y.value.toDouble(),
                        pointList[i + 1].x.value.toDouble(),
                        pointList[i + 1].y.value.toDouble(),
                        pointList[j].x.value.toDouble(),
                        pointList[j].y.value.toDouble(),
                        pointList[(j + 1) % len].x.value.toDouble(),
                        pointList[(j + 1) % len].y.value.toDouble())

                if (cut) {
                    return false
                }
            }
        }
        return true
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return when (databaseType) {
            DatabaseType.POSTGRES,
            DatabaseType.H2 -> "\"${getValueAsRawString()}\""
            DatabaseType.MYSQL -> getValueAsRawString()
            else ->
                throw IllegalArgumentException("Unsupported SqlPolygonGene.getValueAsPrintableString() for ${databaseType}")
        }
    }

    override fun getValueAsRawString(): String {
        return when (databaseType) {
            /*
             * In MySQL, the first and the last point of
             * the polygon should be equal. We enforce
             * this by repeating the first point at the
             * end of the list
             */
            DatabaseType.MYSQL -> {
                "POLYGON(LINESTRING(" + points.getViewOfElements()
                        .joinToString(", ")
                        { it.getValueAsRawString() } + ", " +
                        points.getViewOfElements()[0].getValueAsRawString() +
                        "))"
            }
            /*
             * In PostgreSQL, the (..) denotes a closed path
             */
            DatabaseType.POSTGRES -> {
                "(${
                    points.getViewOfElements().joinToString(", ")
                    { it.getValueAsRawString() }
                })"
            }

            /*
             * In H2, polygon's last point must be equal to
             * first point, we enforce that by repeating the
             * first point.
             */
            DatabaseType.H2 -> {
                "POLYGON((${
                    points.getViewOfElements().joinToString(", ") {
                        it.x.getValueAsRawString() +
                                " " + it.y.getValueAsRawString()
                    } + ", " + points.getViewOfElements()[0].x.getValueAsRawString() +
                            " " + points.getViewOfElements()[0].y.getValueAsRawString()
                }))"
            }
            else -> throw IllegalArgumentException("Unsupported SqlPolygonGene.getValueAsRawString() for ${databaseType}")

        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid({this.points.copyValueFrom(other.points)}, false)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPolygonGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.points.containsSameValueAs(other.points)
    }



    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPolygonGene -> {
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
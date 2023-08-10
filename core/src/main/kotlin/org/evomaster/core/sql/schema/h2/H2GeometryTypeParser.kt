package org.evomaster.core.sql.schema.h2

import java.util.regex.Pattern

/**
 * Translates a string representing an H2 geometry type definition
 * (defined in https://www.h2database.com/html/datatypes.html#geometry_type)
 * into a standard SQL spatial type (https://dev.mysql.com/doc/refman/5.7/en/spatial-type-overview.html).
 * Examples:
 * "GEOMETRY(POINT)" -> "POINT"
 * "GEOMETRY(LINESTRING)" -> "LINESTRING"
 * "GEOMETRY(POLYGON)" -> "POLYGON"
 */
class H2GeometryTypeParser {

    companion object {
        /**
         * https://www.h2database.com/html/datatypes.html#geometry_type
         * GEOMETRY
         * [({ GEOMETRY |
         * { POINT
         * | LINESTRING
         * | POLYGON
         * | MULTIPOINT
         * | MULTILINESTRING
         * | MULTIPOLYGON
         * | GEOMETRYCOLLECTION } [Z|M|ZM]}
         * [, sridInt] )]
         *
         * Examples:
         * GEOMETRY
         * GEOMETRY(POINT)
         * GEOMETRY(POINT Z)
         * GEOMETRY(POINT Z, 4326)
         * GEOMETRY(GEOMETRY, 4326)
         */
        private const val H2_GEOMETRY_REGEX = "GEOMETRY(\\s*)\\((\\s*)(?<geometricObject>GEOMETRY|POINT|LINESTRING|POLYGON|MULTIPOINT|MULTILINESTRING|MULTIPOLYGON|GEOMETRYCOLLECTION)(\\s*)(?<geometricDimension>Z|M|ZM)?(\\s*)(,\\s*(?<spatialReferenceSystemIdentifier>\\d+)\\s*)?\\)"

        /**
         * Precompiled pattern for the H2 Geometry definition grammar
         */
        private val pattern = Pattern.compile(H2_GEOMETRY_REGEX)

    }

    /**
     * Returns if the given string is a parsable
     * H2 Geometry type definition.
     */
    fun isParsable(typeAsString: String): Boolean {
        val matcher = pattern.matcher(typeAsString.trim())
        return matcher.matches()
    }


    /**
     * Requires the string to be a valid H2 Geometry type definition
     * (i.e. [isParsable()] must returns true).
     *
     * It returns a H2GeometryType instance with the geometricObject string,
     * the dimension (if any) and the spatialReferenceSystemIdentifier (if any)
     */
    fun parse(typeAsString: String): H2GeometryType {
        if (!isParsable(typeAsString)) {
            throw IllegalArgumentException("Cannot translate an invalid H2 geometry type definition: $typeAsString")
        }
        val matcher = pattern.matcher(typeAsString.trim())
        matcher.matches()
        return H2GeometryType(geometricObjectString = matcher.group("geometricObject"),
                geometricDimensionString = matcher.group("geometricDimension"),
                spatialReferenceSystemIdentifierInt = matcher.group("spatialReferenceSystemIdentifier")?.toInt())
    }


}
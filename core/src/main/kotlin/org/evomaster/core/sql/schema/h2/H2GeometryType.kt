package org.evomaster.core.sql.schema.h2

/**
 * Represents the results of parsing a H2 Geometry
 * column type definition
 * (https://www.h2database.com/html/datatypes.html#geometry_type)
 */
class H2GeometryType(val geometricObjectString: String,
                     val geometricDimensionString: String?,
                     val spatialReferenceSystemIdentifierInt: Int? ) {

}
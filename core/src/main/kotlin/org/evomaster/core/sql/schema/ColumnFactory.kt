package org.evomaster.core.sql.schema

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.CompositeTypeColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.CompositeTypeDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.schema.h2.H2GeometryType
import org.evomaster.core.sql.schema.h2.H2GeometryTypeParser

object ColumnFactory {

    const val BLANK_SPACE: String = " "

    const val UNDERSCORE: String = "_"

    fun createColumnFromCompositeTypeDto(
            columnDto: CompositeTypeColumnDto,
            compositeTypes: List<CompositeTypeDto>,
            databaseType: DatabaseType
    ): Column {
        val typeAsString = columnDto.type
        val columns: List<Column>?
        val columnType: ColumnDataType
        if (columnDto.columnTypeIsComposite) {
            columns = compositeTypes
                    .first { it.name.equals(typeAsString) }
                    .columns
                    .map { createColumnFromCompositeTypeDto(it, compositeTypes, databaseType) }
                    .toList()
            columnType = ColumnDataType.COMPOSITE_TYPE
        } else {
            columnType = parseColumnDataType(typeAsString)
            columns = null
        }
        return Column(
                name = columnDto.name,
                size = columnDto.size,
                type = columnType,
                dimension = columnDto.numberOfDimensions,
                isUnsigned = columnDto.isUnsigned,
                nullable = columnDto.nullable,
                databaseType = databaseType,
                scale = columnDto.scale,
                compositeType = columns
        )
    }

    fun createColumnFromDto(
            columnDto: ColumnDto,
            lowerBoundForColumn: Long? = null,
            upperBoundForColumn: Long? = null,
            enumValuesForColumn: List<String>? = null,
            similarToPatternsForColumn: List<String>? = null,
            likePatternsForColumn: List<String>? = null,
            compositeTypes: Map<String, CompositeType> = mapOf(),
            databaseType: DatabaseType
    ): Column {

        val columnDataType = buildColumnDataType(columnDto)
        if (columnDataType == ColumnDataType.COMPOSITE_TYPE) {
            if (!compositeTypes.containsKey(columnDto.type)) {
                throw IllegalArgumentException("Missing composite type declaration for ${columnDto.name} of type ${columnDto.type}")
            }
        }
        return Column(
                name = columnDto.name,
                size = columnDto.size,
                type = columnDataType,
                dimension = columnDto.numberOfDimensions,
                isUnsigned = columnDto.isUnsigned,
                primaryKey = columnDto.primaryKey,
                autoIncrement = columnDto.autoIncrement,
                foreignKeyToAutoIncrement = columnDto.foreignKeyToAutoIncrement,
                nullable = columnDto.nullable,
                unique = columnDto.unique,
                lowerBound = lowerBoundForColumn,
                upperBound = upperBoundForColumn,
                enumValuesAsStrings = enumValuesForColumn,
                similarToPatterns = similarToPatternsForColumn,
                likePatterns = likePatternsForColumn,
                databaseType = databaseType,
                scale = columnDto.scale,
                compositeType = if (columnDataType.equals(ColumnDataType.COMPOSITE_TYPE)) compositeTypes[columnDto.type]!!.columns else null,
                compositeTypeName = columnDto.type
        )
    }

    private fun buildColumnDataType(columnDto: ColumnDto): ColumnDataType {
        return if (columnDto.isEnumeratedType) ColumnDataType.VARCHAR
        else if (columnDto.isCompositeType) ColumnDataType.COMPOSITE_TYPE
        else parseColumnDataType(columnDto)
    }

    private fun parseColumnDataType(columnDto: ColumnDto): ColumnDataType {
        val typeAsString = columnDto.type
        return parseColumnDataType(typeAsString)
    }

    private fun isH2GeometryType(typeAsString: String):Boolean {
        val parser = H2GeometryTypeParser()
        return parser.isParsable(typeAsString)
    }

    private fun parseH2GeometryType(typeAsString: String): H2GeometryType {
        val parser = H2GeometryTypeParser()
        return parser.parse(typeAsString)
    }

    private fun parseColumnDataType(typeAsString: String): ColumnDataType {
        try {
            val t = prepareTypeString(typeAsString)
            return ColumnDataType.valueOf(t)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                    String.format(
                            "Column data type %s is not supported in EvoMaster data types." +
                                    " Note that EvoMaster only support certain databases, including" +
                                    " Postgres and MySQL. You can see the full, updated list on the main documentation" +
                                    " page of EvoMaster." +
                                    " If your database is listed there, please report this as an issue." +
                                    " If not, you can still open a 'feature request' to ask to add support for such database." +
                                    " But, of course, no guarantee of if/when it will be supported.",
                            typeAsString
                    )
            )
        }
    }

    /**
     * This function normalizes several details of
     * the typeAsString before converting it to a ColumnDataType
     * (e.g. replaces blank spaces, removes underscores, handles
     * H2 geometric type definitions, etc.)
     */
    private fun prepareTypeString(typeAsString: String): String {
        var t = typeAsString
        if (isH2GeometryType(t)) {
            val h2GeometryType = parseH2GeometryType(t)
            if (h2GeometryType.geometricDimensionString != null || h2GeometryType.spatialReferenceSystemIdentifierInt != null) {
                throw IllegalArgumentException("Cannot handle H2 geometry type with specific  dimension or spatial reference system Identifier: $t")
            }
            t = h2GeometryType.geometricObjectString
        }

        t = if (typeAsString.startsWith(UNDERSCORE))
            t.substring(1).uppercase()
        else
            t.uppercase()

        if (t.contains(BLANK_SPACE)) {
            t = t.replace(BLANK_SPACE, UNDERSCORE)
        }
        return t
    }

}

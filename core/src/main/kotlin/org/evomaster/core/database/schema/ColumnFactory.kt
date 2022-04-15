package org.evomaster.core.database.schema

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.CompositeTypeColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.CompositeTypeDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType

object ColumnFactory {


    fun createColumnFromCompositeTypeDto(
            columnDto: CompositeTypeColumnDto,
            compositeTypes: List<CompositeTypeDto>,
            databaseType: DatabaseType
    ): Column {
        val typeAsString = columnDto.type
        val columns: List<Column>?
        val columnType: ColumnDataType
        if (columnDto.isCompositeType) {
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
                precision = columnDto.precision,
                compositeType = columns
        )
    }

    fun createColumnFromDto(
            columnDto: ColumnDto,
            lowerBoundForColumn: Int? = null,
            upperBoundForColumn: Int? = null,
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
                precision = columnDto.precision,
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

    private fun parseColumnDataType(typeAsString: String): ColumnDataType {
        try {
            val t =
                    if (typeAsString.startsWith("_")) typeAsString.substring(1).uppercase() else typeAsString.uppercase()
            return ColumnDataType.valueOf(t)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                    String.format(
                            "Column data type %s is not supported in EvoMaster Data types",
                            typeAsString
                    )
            )
        }
    }

}

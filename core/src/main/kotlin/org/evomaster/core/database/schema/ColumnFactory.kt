package org.evomaster.core.database.schema

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType

object ColumnFactory {

    fun createColumnFromDto(
        columnDto: ColumnDto, lowerBoundForColumn: Int?, upperBoundForColumn: Int?,
        enumValuesForColumn: List<String>?, similarToPatternsForColumn: List<String>?,
        likePatternsForColumn: List<String>?, databaseType: DatabaseType
    ): Column {

        return Column(
            name = columnDto.name,
            size = columnDto.size,
            type = if (columnDto.isEnumeratedType) ColumnDataType.VARCHAR else parseColumnDataType(columnDto),
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
            precision = columnDto.precision
        )
    }

    private fun parseColumnDataType(columnDto: ColumnDto): ColumnDataType {
        val typeAsString = columnDto.type
        try {
            val t =
                if (typeAsString.startsWith("_")) "ARRAY${typeAsString.uppercase()}" else typeAsString.uppercase()
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

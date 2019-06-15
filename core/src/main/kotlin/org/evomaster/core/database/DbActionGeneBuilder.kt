package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.gene.*

class DbActionGeneBuilder {


    private fun getForeignKey(table: Table, column: Column): ForeignKey? {

        //TODO: what if a column is part of more than 1 FK? is that even possible?

        return table.foreignKeys.find { it.sourceColumns.contains(column) }
    }

    fun buildGene(id: Long, table: Table, column: Column): Gene {

        val fk = getForeignKey(table, column)

        /*
            TODO should nullable columns be wrapped in a OptionalGene?
            Maybe not, as need special gene to represent NULL even for
            numeric values
         */

        val gene = when {
            //TODO handle all constraints and cases
            column.autoIncrement ->
                SqlAutoIncrementGene(column.name)
            fk != null ->
                SqlForeignKeyGene(column.name, id, fk.targetTable, column.nullable)

            else -> when (column.type) {
                /**
                 * BOOLEAN(1) is assumed to be a boolean/Boolean field
                 */
                ColumnDataType.BOOLEAN, ColumnDataType.BOOL -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(column.name, column.enumValuesAsStrings.map { it.toBoolean() })
                    } else {
                        BooleanGene(column.name)
                    }
                }
                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 */
                ColumnDataType.TINYINT -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
                    } else {
                        IntegerGene(column.name,
                                min = column.lowerBound ?: Byte.MIN_VALUE.toInt(),
                                max = column.upperBound ?: Byte.MAX_VALUE.toInt())
                    }
                }
                /**
                 * SMALLINT(5) is assumed as a short/Short field
                 */
                ColumnDataType.INT2, ColumnDataType.SMALLINT -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
                    } else {
                        IntegerGene(column.name,
                                min = column.lowerBound ?: Short.MIN_VALUE.toInt(),
                                max = column.upperBound ?: Short.MAX_VALUE.toInt())
                    }
                }
                /**
                 * CHAR(255) is assumed to be a char/Character field.
                 * A StringGene of length 1 is used to represent the data.
                 * TODO How to discover if it is a char or a char[] of 255 elements?
                 */
                ColumnDataType.CHAR -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings)
                    } else {
                        StringGene(name = column.name, value = "f", minLength = 0, maxLength = 1)
                    }
                }
                /**
                 * INT4/INTEGER(10) is a int/Integer field
                 */
                ColumnDataType.INT4, ColumnDataType.INTEGER ->
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
                    } else {
                        IntegerGene(column.name,
                                min = column.lowerBound ?: Int.MIN_VALUE,
                                max = column.upperBound ?: Int.MAX_VALUE)
                    }
                /**
                 * BIGINT(19) is a long/Long field
                 */
                ColumnDataType.INT8, ColumnDataType.BIGINT -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(column.name, column.enumValuesAsStrings.map { it.toLong() })
                    } else {

                        LongGene(column.name)
                    }
                }
                /**
                 * DOUBLE(17) is assumed to be a double/Double field
                 * TODO How to discover if the source field is a float/Float field?
                 */

                ColumnDataType.DOUBLE -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toDouble() })
                    } else {
                        DoubleGene(column.name)
                    }
                }
                /**
                 * VARCHAR(N) is assumed to be a String with a maximum length of N.
                 * N could be as large as Integer.MAX_VALUE
                 */
                ColumnDataType.TEXT, ColumnDataType.VARCHAR ->
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings)
                    } else {
                        StringGene(name = column.name, minLength = 0, maxLength = column.size)
                    }


                /**
                 * TIMESTAMP is assumed to be a Date field
                 */
                ColumnDataType.TIMESTAMP -> {
                    if (column.enumValuesAsStrings != null) {
                        throw RuntimeException("Unsupported enum in TIMESTAMP. Please implement")
                    } else {
                        SqlTimestampGene(column.name)
                    }
                }

                /**
                 * DATE is a date without time of day
                 */
                ColumnDataType.DATE -> DateGene(column.name)
                /**
                 * CLOB(N) stores a UNICODE document of length N
                 */
                ColumnDataType.CLOB -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings)
                    } else {
                        StringGene(name = column.name, minLength = 0, maxLength = column.size)
                    }
                }
                //column.type.equals("VARBINARY", ignoreCase = true) ->
                //handleVarBinary(it)

                /**
                 * Could be any kind of binary data... so let's just use a string,
                 * which also simplifies when needing generate the test files
                 */
                ColumnDataType.BLOB -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings)
                    } else {
                        StringGene(name = column.name, minLength = 0, maxLength = 8)
                    }
                }

                /**
                 * REAL is identical to the floating point statement float(24).
                 * TODO How to discover if the source field is a float/Float field?
                 */
                ColumnDataType.REAL -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toDouble() })
                    } else {
                        DoubleGene(column.name)
                    }
                }
                /**
                 * TODO: DECIMAL precision is lower than a float gene
                 */
                ColumnDataType.DECIMAL -> {
                    if (column.enumValuesAsStrings != null) {
                        EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toFloat() })
                    } else {
                        FloatGene(column.name)
                    }
                }

                /**
                 * Postgres UUID column type
                 */
                ColumnDataType.UUID -> SqlUUIDGene(column.name)

                /**
                 * Postgres JSONB column type
                 */
                ColumnDataType.JSON, ColumnDataType.JSONB -> SqlJSONGene(column.name)

                /**
                 * Postgres XML column type
                 */
                ColumnDataType.XML -> SqlXMLGene(column.name)

                else -> throw IllegalArgumentException("Cannot handle: $column.")
            }

        }

        if (column.primaryKey) {
            return SqlPrimaryKeyGene(column.name, table.name, gene, id)
        } else {
            return gene
        }

    }
}
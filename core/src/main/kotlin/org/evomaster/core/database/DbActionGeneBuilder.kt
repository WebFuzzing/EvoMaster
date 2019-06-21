package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.parser.RegexHandler.createGeneForPostgresLike
import org.evomaster.core.parser.RegexHandler.createGeneForPostgresSimilarTo
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.gene.regex.RegexGene

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
                ColumnDataType.BOOLEAN, ColumnDataType.BOOL ->
                    handleBooleanColumn(column)

                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 */
                ColumnDataType.TINYINT ->
                    handleTinyIntColumn(column)

                /**
                 * SMALLINT(5) is assumed as a short/Short field
                 */
                ColumnDataType.INT2, ColumnDataType.SMALLINT ->
                    handleSmallIntColumn(column)

                /**
                 * CHAR(255) is assumed to be a char/Character field.
                 * A StringGene of length 1 is used to represent the data.
                 *
                 */
                ColumnDataType.CHAR ->
                    handleCharColumn(column)

                /**
                 * INT4/INTEGER(10) is a int/Integer field
                 */
                ColumnDataType.INT4, ColumnDataType.INTEGER ->
                    handleIntegerColumn(column)

                /**
                 * BIGINT(19) is a long/Long field
                 */
                ColumnDataType.INT8, ColumnDataType.BIGINT ->
                    handleBigIntColumn(column)

                /**
                 * DOUBLE(17) is assumed to be a double/Double field
                 *
                 */

                ColumnDataType.DOUBLE ->
                    handleDoubleColumn(column)

                /**
                 * VARCHAR(N) is assumed to be a String with a maximum length of N.
                 * N could be as large as Integer.MAX_VALUE
                 */
                ColumnDataType.TEXT, ColumnDataType.VARCHAR ->
                    handleTextColumn(column)


                /**
                 * TIMESTAMP is assumed to be a Date field
                 */
                ColumnDataType.TIMESTAMP ->
                    handleTimestampColumn(column)

                /**
                 * DATE is a date without time of day
                 */
                ColumnDataType.DATE ->
                    DateGene(column.name)
                /**
                 * CLOB(N) stores a UNICODE document of length N
                 */
                ColumnDataType.CLOB ->
                    handleCLOBColumn(column)

                //column.type.equals("VARBINARY", ignoreCase = true) ->
                //handleVarBinary(it)

                /**
                 * Could be any kind of binary data... so let's just use a string,
                 * which also simplifies when needing generate the test files
                 */
                ColumnDataType.BLOB ->
                    handleBLOBColumn(column)


                ColumnDataType.REAL ->
                    handleRealColumn(column)


                ColumnDataType.DECIMAL ->
                    handleDecimalColumn(column)

                /**
                 * Postgres UUID column type
                 */
                ColumnDataType.UUID ->
                    SqlUUIDGene(column.name)

                /**
                 * Postgres JSONB column type
                 */
                ColumnDataType.JSON, ColumnDataType.JSONB ->
                    SqlJSONGene(column.name)

                /**
                 * Postgres XML column type
                 */
                ColumnDataType.XML ->
                    SqlXMLGene(column.name)

                else -> throw IllegalArgumentException("Cannot handle: $column.")
            }

        }

        if (column.primaryKey) {
            return SqlPrimaryKeyGene(column.name, table.name, gene, id)
        } else {
            return gene
        }

    }

    private fun handleBigIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toLong() })
        } else {

            LongGene(column.name)
        }
    }

    private fun handleIntegerColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
        } else {
            IntegerGene(column.name,
                    min = column.lowerBound ?: Int.MIN_VALUE,
                    max = column.upperBound ?: Int.MAX_VALUE)
        }
    }

    private fun handleCharColumn(column: Column): Gene {
        //  TODO How to discover if it is a char or a char[] of 255 elements?
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings)
        } else {
            StringGene(name = column.name, value = "f", minLength = 0, maxLength = 1)
        }
    }

    private fun handleDoubleColumn(column: Column): Gene {
        // TODO How to discover if the source field is a float/Float field?
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toDouble() })
        } else {
            DoubleGene(column.name)
        }
    }

    private fun handleSmallIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
        } else {
            IntegerGene(column.name,
                    min = column.lowerBound ?: Short.MIN_VALUE.toInt(),
                    max = column.upperBound ?: Short.MAX_VALUE.toInt())
        }
    }

    private fun handleTinyIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
        } else {
            IntegerGene(column.name,
                    min = column.lowerBound ?: Byte.MIN_VALUE.toInt(),
                    max = column.upperBound ?: Byte.MAX_VALUE.toInt())
        }
    }

    private fun handleTextColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings)
        } else {
            if (column.similarToPatterns != null && column.similarToPatterns.isNotEmpty()) {
                val columnName = column.name
                val similarToPatterns: List<String> = column.similarToPatterns
                buildSimilarToRegexGene(columnName, similarToPatterns, databaseType = column.databaseType)
            } else if (column.likePatterns != null && column.likePatterns.isNotEmpty()) {
                val columnName = column.name
                val likePatterns = column.likePatterns
                buildLikeRegexGene(columnName, likePatterns, databaseType = column.databaseType)
            } else {
                StringGene(name = column.name, minLength = 0, maxLength = column.size)
            }
        }
    }

    /**
     * Builds a RegexGene using a name and a list of LIKE patterns.
     * The resulting gene is a disjunction of the given patterns
     */
    fun buildLikeRegexGene(geneName: String, likePatterns: List<String>, databaseType: DatabaseType): RegexGene {
        return when {
            databaseType == DatabaseType.POSTGRES -> buildPostgresLikeRegexGene(geneName, likePatterns)
            //TODO: support other database SIMILAR_TO check expressions
            else -> throw UnsupportedOperationException("Must implement similarTo expressions for database %s".format(databaseType))
        }
    }

    private fun buildPostgresLikeRegexGene(geneName: String, likePatterns: List<String>): RegexGene {
        val disjunctionRxGenes = likePatterns
                .map { createGeneForPostgresLike(it) }
                .map { it.disjunctions }
                .map { it.disjunctions }
                .flatten()
        return RegexGene(geneName, disjunctions = DisjunctionListRxGene(disjunctions = disjunctionRxGenes))
    }


    /**
     * Builds a RegexGene using a name and a list of SIMILAR_TO patterns.
     * The resulting gene is a disjunction of the given patterns
     * according to the database we are using
     */
    fun buildSimilarToRegexGene(geneName: String, similarToPatterns: List<String>, databaseType: DatabaseType): RegexGene {
        return when {
            databaseType == DatabaseType.POSTGRES -> buildPostgresSimilarToRegexGene(geneName, similarToPatterns)
            //TODO: support other database SIMILAR_TO check expressions
            else -> throw UnsupportedOperationException("Must implement similarTo expressions for database %s".format(databaseType))
        }
    }

    private fun buildPostgresSimilarToRegexGene(geneName: String, similarToPatterns: List<String>): RegexGene {
        val disjunctionRxGenes = similarToPatterns
                .map { createGeneForPostgresSimilarTo(it) }
                .map { it.disjunctions }
                .map { it.disjunctions }
                .flatten()
        return RegexGene(geneName, disjunctions = DisjunctionListRxGene(disjunctions = disjunctionRxGenes))
    }

    private fun handleTimestampColumn(column: Column): SqlTimestampGene {
        return if (column.enumValuesAsStrings != null) {
            throw RuntimeException("Unsupported enum in TIMESTAMP. Please implement")
        } else {
            SqlTimestampGene(column.name)
        }
    }

    private fun handleCLOBColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings)
        } else {
            StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    private fun handleBLOBColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings)
        } else {
            StringGene(name = column.name, minLength = 0, maxLength = 8)
        }
    }

    private fun handleRealColumn(column: Column): Gene {
        /**
         * REAL is identical to the floating point statement float(24).
         * TODO How to discover if the source field is a float/Float field?
         */
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toDouble() })
        } else {
            DoubleGene(column.name)
        }
    }

    private fun handleDecimalColumn(column: Column): Gene {
        /**
         * TODO: DECIMAL precision is lower than a float gene
         */
        return if (column.enumValuesAsStrings != null) {
            EnumGene(name = column.name, values = column.enumValuesAsStrings.map { it.toFloat() })
        } else {
            FloatGene(column.name)
        }
    }

    private fun handleBooleanColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toBoolean() })
        } else {
            BooleanGene(column.name)
        }
    }
}
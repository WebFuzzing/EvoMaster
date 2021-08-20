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
import org.evomaster.core.search.gene.sql.*
import kotlin.math.pow

class DbActionGeneBuilder {


    private fun getForeignKey(table: Table, column: Column): ForeignKey? {

        //TODO: what if a column is part of more than 1 FK? is that even possible?

        return table.foreignKeys.find { it.sourceColumns.contains(column) }
    }

    fun buildGene(id: Long, table: Table, column: Column): Gene {

        val fk = getForeignKey(table, column)

        var gene = when {
            //TODO handle all constraints and cases
            column.autoIncrement ->
                SqlAutoIncrementGene(column.name)
            fk != null ->
                SqlForeignKeyGene(column.name, id, fk.targetTable, column.nullable)

            else -> when (column.type) {
                // Man: TODO need to check
                ColumnDataType.BIT->
                    handleBitColumn(column)

                /**
                 * BOOLEAN(1) is assumed to be a boolean/Boolean field
                 */
                ColumnDataType.BOOLEAN, ColumnDataType.BOOL ->
                    handleBooleanColumn(column)

                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 */
//                ColumnDataType.TINYINT ->
//                    handleTinyIntColumn(column)

                /**
                 * SMALLINT(5) is assumed as a short/Short field
                 */
//                ColumnDataType.INT2, ColumnDataType.SMALLINT ->
//                    handleSmallIntColumn(column)

                /**
                 * CHAR(255) is assumed to be a char/Character field.
                 * A StringGene of length 1 is used to represent the data.
                 *
                 */
                ColumnDataType.CHAR ->
                    handleCharColumn(column)

                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 * INT2/SMALLINT(5) is assumed as a short/Short field
                 * INT4/INTEGER(10) is a int/Integer field
                 */
                ColumnDataType.TINYINT, ColumnDataType.INT2, ColumnDataType.SMALLINT, ColumnDataType.INT, ColumnDataType.INT4, ColumnDataType.INTEGER, ColumnDataType.SERIAL, ColumnDataType.MEDIUMINT ->
                    handleIntegerColumn(column)

                /**
                 * BIGINT(19) is a long/Long field
                 */
                ColumnDataType.INT8, ColumnDataType.BIGINT, ColumnDataType.BIGSERIAL ->
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
                ColumnDataType.ARRAY_VARCHAR, //FIXME need general solution for arrays
                ColumnDataType.TEXT, ColumnDataType.VARCHAR, ColumnDataType.CLOB ->
                    handleTextColumn(column)

                //TODO normal TIME, and add tests for it. this is just a quick workaround for patio-api
                ColumnDataType.TIMETZ, ColumnDataType.TIME ->
                    TimeGene(column.name)


                /**
                 * TIMESTAMP is assumed to be a Date field
                 */
                ColumnDataType.TIMESTAMP, ColumnDataType.TIMESTAMPTZ ->
                    handleTimestampColumn(column)

                /**
                 * DATE is a date without time of day
                 */
                ColumnDataType.DATE ->
                    DateGene(column.name)

                /**
                 * TODO need to check with Andrea regarding fsp which is the fractional seconds precision
                 *
                 * see https://dev.mysql.com/doc/refman/8.0/en/date-and-time-type-syntax.html
                 */
                ColumnDataType.DATETIME ->
                    DateTimeGene(column.name)

                ColumnDataType.YEAR ->
                    handleYearColumn(column)

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


                ColumnDataType.DECIMAL, ColumnDataType.DEC, ColumnDataType.NUMERIC ->
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

                ColumnDataType.ENUM ->
                    handleEnumColumn(column)

                else -> throw IllegalArgumentException("Cannot handle: $column.")
            }

        }

        if (column.primaryKey) {
            gene = SqlPrimaryKeyGene(column.name, table.name, gene, id)
        }

        if (column.nullable && fk == null) {
            //FKs handle nullability in their own custom way
            gene = SqlNullable(column.name, gene)
        }

        return gene
    }

    /*
        https://dev.mysql.com/doc/refman/8.0/en/year.html
     */
    private fun handleYearColumn(column: Column): Gene{
        // Year(2) is not supported by mysql 8.0
        if (column.size == 2)
            return IntegerGene(column.name,16 ,min =0, max = 99)

        return IntegerGene(column.name, 2016, min = 1901, max = 2155)
    }

    private fun handleEnumColumn(column: Column): Gene{
        return EnumGene(name = column.name, data = column.enumValuesAsStrings?: listOf())
    }

    private fun handleBigIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toLong() })
        } else {

            LongGene(column.name)
        }
    }

    private fun handleIntegerColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
        } else {

            if ((column.type == ColumnDataType.INT4
                        || column.type == ColumnDataType.INT
                        || column.type == ColumnDataType.INTEGER) && column.isUnsigned){
                LongGene(column.name, min = 0L, max = 4294967295L)
            }else{
                val min = when{
                    column.isUnsigned -> 0
                    column.type == ColumnDataType.TINYINT -> Byte.MIN_VALUE.toInt()
                    column.type == ColumnDataType.SMALLINT || column.type == ColumnDataType.INT2 -> Short.MIN_VALUE.toInt()
                    column.type == ColumnDataType.MEDIUMINT -> -8388608
                    else -> Int.MIN_VALUE
                }

                val max = when (column.type){
                    ColumnDataType.TINYINT -> if (column.isUnsigned) 255 else Byte.MAX_VALUE.toInt()
                    ColumnDataType.SMALLINT, ColumnDataType.INT2 -> if (column.isUnsigned) 65535 else Short.MAX_VALUE.toInt()
                    ColumnDataType.MEDIUMINT -> if (column.isUnsigned) 16777215 else 8388607
                    else -> Int.MAX_VALUE
                }

                IntegerGene(column.name,
                    min = column.lowerBound ?: min,
                    max = column.upperBound ?: max)
            }
        }
    }

    private fun handleCharColumn(column: Column): Gene {
        //  TODO How to discover if it is a char or a char[] of 255 elements?
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings)
        } else {
            StringGene(name = column.name, value = "f", minLength = 0, maxLength = 1)
        }
    }

    private fun handleDoubleColumn(column: Column): Gene {
        // TODO How to discover if the source field is a float/Float field?
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings.map { it.toDouble() })
        } else {
            DoubleGene(column.name)
        }
    }

    @Deprecated("replaced by handleIntegerColumn, now all numeric types resulting in IntegerGene would by handled in handleIntegerColumn")
    private fun handleSmallIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            if (column.enumValuesAsStrings.isEmpty()) {
                throw IllegalArgumentException("the list of enumerated values cannot be empty")
            } else {
                EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
            }
        } else {
            IntegerGene(column.name,
                    min = column.lowerBound ?: Short.MIN_VALUE.toInt(),
                    max = column.upperBound ?: Short.MAX_VALUE.toInt())
        }
    }

    @Deprecated("replaced by handleIntegerColumn, now all numeric types resulting in IntegerGene would by handled in handleIntegerColumn")
    private fun handleTinyIntColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            if (column.enumValuesAsStrings.isEmpty()) {
                throw IllegalArgumentException("the list of enumerated values cannot be empty")
            } else {
                EnumGene(column.name, column.enumValuesAsStrings.map { it.toInt() })
            }
        } else {
            IntegerGene(column.name,
                    min = column.lowerBound ?: Byte.MIN_VALUE.toInt(),
                    max = column.upperBound ?: Byte.MAX_VALUE.toInt())
        }
    }

    private fun handleTextColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            if (column.enumValuesAsStrings.isEmpty()) {
                throw IllegalArgumentException("the list of enumerated values cannot be empty")
            } else {
                EnumGene(name = column.name, data = column.enumValuesAsStrings)
            }
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
        return when(databaseType) {
            DatabaseType.POSTGRES, DatabaseType.MYSQL -> buildPostgresMySQLLikeRegexGene(geneName, likePatterns)
            //TODO: support other database SIMILAR_TO check expressions
            else -> throw UnsupportedOperationException("Must implement LIKE expressions for database %s".format(databaseType))
        }
    }

    private fun buildPostgresMySQLLikeRegexGene(geneName: String, likePatterns: List<String>): RegexGene {
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

    fun buildSqlTimestampGene(name: String): DateTimeGene {
        return DateTimeGene(
                name = name,
                date = DateGene("date",
                        year = IntegerGene("year", 2016, 1900, 2100),
                        month = IntegerGene("month", 3, 1, 12),
                        day = IntegerGene("day", 12, 1, 31),
                        onlyValidDates = true),
                time = TimeGene("time",
                        hour = IntegerGene("hour", 0, 0, 23),
                        minute = IntegerGene("minute", 0, 0, 59),
                        second = IntegerGene("second", 0, 0, 59)
                        ),
                dateTimeGeneFormat =  DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME
        )

    }

    private fun handleTimestampColumn(column: Column): DateTimeGene {
        return if (column.enumValuesAsStrings != null) {
            throw RuntimeException("Unsupported enum in TIMESTAMP. Please implement")
        } else {
            return buildSqlTimestampGene(column.name)
        }
    }

    private fun handleCLOBColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings)

        } else {
            StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    private fun handleBLOBColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings)
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
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings.map { it.toDouble() })

        } else {
            DoubleGene(column.name)
        }
    }

    private fun handleDecimalColumn(column: Column): Gene {
        /**
         * TODO: DECIMAL precision is lower than a float gene
         */
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(name = column.name, data = column.enumValuesAsStrings.map { it.toFloat() })
        } else {
            FloatGene(column.name)
        }
    }

    private fun handleBooleanColumn(column: Column): Gene {
        return if (column.enumValuesAsStrings != null) {
            checkNotEmpty(column.enumValuesAsStrings)
            EnumGene(column.name, column.enumValuesAsStrings.map { it.toBoolean() })

        } else {
            BooleanGene(column.name)
        }
    }

    /**
     * handle bit for mysql
     * https://dev.mysql.com/doc/refman/8.0/en/bit-value-literals.html
     */
    private fun handleBitColumn(column: Column): Gene{

        return IntegerGene(column.name,  min= 0, max = (2.0).pow(column.size).toInt() -1 )
    }

    companion object {
        /**
         * Throws an exception if the enum values is non-null and empty
         */
        private fun checkNotEmpty(enumValuesAsStrings: List<String>) {
            if (enumValuesAsStrings != null && enumValuesAsStrings.isEmpty()) {
                throw IllegalArgumentException("the list of enumerated values cannot be empty")
            }
        }
    }
}

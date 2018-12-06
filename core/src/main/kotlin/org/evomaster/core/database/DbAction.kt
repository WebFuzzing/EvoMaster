package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType.*
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*

/**
 *  An action executed on the database.
 *  Typically, a SQL Insertion
 */
class DbAction(
        /**
         * The involved table
         */
        val table: Table,
        /**
         * Which columns we are inserting data into
         */
        val selectedColumns: Set<Column>,
        private val id: Long,
        computedGenes: List<Gene>? = null,
        /**
         * Instead of a new INSERT action, we might have "fake" actions representing
         * data already existing in the database.
         * This is very helpful when dealing with Foreign Keys.
         */
        val representExistingData: Boolean = false
) : Action {

    init {
        /*
            Existing data actions are very special, and can only contain PKs
            with immutable data.
         */
        if(representExistingData){
            if(computedGenes == null){
                throw IllegalArgumentException("No defined genes")
            }

            for(pk in computedGenes){
                if(pk !is SqlPrimaryKeyGene || pk.gene !is ImmutableDataHolderGene){
                    throw IllegalArgumentException("Invalid gene: ${pk.name}")
                }
            }
        }
    }


    private
    val genes: List<Gene> = computedGenes ?: selectedColumns.map {

        val fk = getForeignKey(table, it)

        /*
            TODO should nullable columns be wrapped in a OptionalGene?
            Maybe not, as need special gene to represent NULL even for
            numeric values
         */

        val gene = when {
            //TODO handle all constraints and cases
            it.autoIncrement ->
                SqlAutoIncrementGene(it.name)
            fk != null ->
                SqlForeignKeyGene(it.name, id, fk.targetTable, it.nullable)

            else -> when (it.type) {
                /**
                 * BOOLEAN(1) is assumed to be a boolean/Boolean field
                 */
                BOOLEAN -> BooleanGene(it.name)
                /**
                 * TINYINT(3) is assumed to be representing a byte/Byte field
                 */
                TINYINT -> IntegerGene(it.name,
                        min = it.lowerBound ?: Byte.MIN_VALUE.toInt(),
                        max = it.upperBound ?: Byte.MAX_VALUE.toInt())
                /**
                 * SMALLINT(5) is assumed as a short/Short field
                 */
                SMALLINT -> IntegerGene(it.name,
                        min = it.lowerBound ?: Short.MIN_VALUE.toInt(),
                        max = it.upperBound ?: Short.MAX_VALUE.toInt())
                /**
                 * CHAR(255) is assumed to be a char/Character field.
                 * A StringGene of length 1 is used to represent the data.
                 * TODO How to discover if it is a char or a char[] of 255 elements?
                 */
                CHAR -> StringGene(name = it.name, value = "f", minLength = 0, maxLength = 1)
                /**
                 * INTEGER(10) is a int/Integer field
                 */
                INTEGER -> IntegerGene(it.name,
                        min = it.lowerBound ?: Int.MIN_VALUE,
                        max = it.upperBound ?: Int.MAX_VALUE)
                /**
                 * BIGINT(19) is a long/Long field
                 */
                BIGINT -> LongGene(it.name)
                /**
                 * DOUBLE(17) is assumed to be a double/Double field
                 * TODO How to discover if the source field is a float/Float field?
                 */

                DOUBLE -> DoubleGene(it.name)
                /**
                 * VARCHAR(N) is assumed to be a String with a maximum length of N.
                 * N could be as large as Integer.MAX_VALUE
                 */
                VARCHAR -> StringGene(name = it.name, minLength = 0, maxLength = it.size)
                /**
                 * TIMESTAMP is assumed to be a Date field
                 */
                TIMESTAMP -> SqlTimestampGene(it.name)
                /**
                 * CLOB(N) stores a UNICODE document of length N
                 */
                CLOB -> StringGene(name = it.name, minLength = 0, maxLength = it.size)
                //it.type.equals("VARBINARY", ignoreCase = true) ->
                //handleVarBinary(it)

                /**
                 * Could be any kind of binary data... so let's just use a string,
                 * which also simplifies when needing generate the test files
                 */
                BLOB -> StringGene(name = it.name, minLength = 0, maxLength = 8)

                /**
                 * REAL is identical to the floating point statement float(24).
                 * TODO How to discover if the source field is a float/Float field?
                 */
                REAL -> DoubleGene(it.name)

                /**
                 * TODO: DECIMAL precision is lower than a float gene
                 */
                DECIMAL -> FloatGene(it.name)

                else -> throw IllegalArgumentException("Cannot handle: $it")
            }

        }

        if (it.primaryKey) {
            SqlPrimaryKeyGene(it.name, table.name, gene, id)
        } else {
            gene
        }
    }


    private fun handleVarBinary(column: Column): Gene {
        /*
            TODO: this is more complicated than expected, as we need
            new gene type to handle transformation to hex format
         */
        /*
            This is a nasty case, as it is a blob of binary data.
            Could be any format, and likely no constraint in the DB schema,
            where the actual constraints are in the SUT code.
            This is also what for example can be used by Hibernate to represent
            a ZoneDataTime before Java 8 support.
            A workaround for the moment is to guess a possible type/constraints
            based on the column name
         */
        if (column.name.contains("time", ignoreCase = true)) {
            return SqlTimestampGene(column.name)
        } else {
            //go for a default string
            return StringGene(name = column.name, minLength = 0, maxLength = column.size)
        }
    }

    private fun getForeignKey(table: Table, column: Column): ForeignKey? {

        //TODO: what if a column is part of more than 1 FK? is that even possible?

        return table.foreignKeys.find { it.sourceColumns.contains(column) }
    }


    override fun getName(): String {
        return "SQL_Insert_${table.name}_${selectedColumns.map { it.name }.joinToString("_")}"
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun copy(): Action {
        return DbAction(table, selectedColumns, id, genes.map(Gene::copy), representExistingData)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    fun geInsertionId(): Long {
        return this.id
    }
}
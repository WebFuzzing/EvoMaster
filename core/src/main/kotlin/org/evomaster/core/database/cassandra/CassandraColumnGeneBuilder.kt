package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene

/**
 * Builds the gene used to generate the value of a Cassandra column, based on its CQL type.
 * Only the scalar CQL types that can be inserted with a plain literal are handled: collections,
 * user defined types, and the types that cannot be given an arbitrary value in an INSERT (eg a
 * counter, which is only writable with an UPDATE) have no representation here.
 */
object CassandraColumnGeneBuilder {

    /**
     * @return whether a gene can be built for [column], ie whether its CQL type is one of the
     * scalar types handled here
     */
    fun isSupported(column: CassandraColumn) = normalize(column.cqlType) in SUPPORTED_CQL_TYPES

    /**
     * @throws IllegalArgumentException if the CQL type of [column] is not handled, as verifiable
     * beforehand with [isSupported]
     */
    fun buildGene(column: CassandraColumn): Gene {

        val name = column.name

        return when (normalize(column.cqlType)) {
            "ascii", "text", "varchar" -> StringGene(name)
            "tinyint" -> IntegerGene(name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt())
            "smallint" -> IntegerGene(name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt())
            "int" -> IntegerGene(name)
            "bigint" -> LongGene(name)
            "varint" -> BigIntegerGene(name)
            "decimal" -> BigDecimalGene(name)
            "float" -> FloatGene(name)
            "double" -> DoubleGene(name)
            "boolean" -> BooleanGene(name)
            "uuid" -> UUIDGene(name)
            /*
                Only valid values are generated, as these genes are used to set up the state of the
                database, and Cassandra would just reject an insertion carrying an invalid one.
             */
            "timestamp" -> DateTimeGene(name, onlyValid = true)
            "date" -> DateGene(name, onlyValidDates = true)
            "time" -> TimeGene(name, onlyValidTimes = true)
            else -> throw IllegalArgumentException("Cannot handle the CQL type of column $column")
        }
    }

    private fun normalize(cqlType: String) = cqlType.trim().lowercase()

    private val SUPPORTED_CQL_TYPES = setOf(
        "ascii", "text", "varchar",
        "tinyint", "smallint", "int", "bigint", "varint", "decimal", "float", "double",
        "boolean",
        "uuid",
        "timestamp", "date", "time"
    )
}

package org.evomaster.core.database.schema

/**
 *
 * Should be immutable
 */
data class Table(
        val name: String,

        val columns: Set<Column>,

        val foreignKeys: Set<ForeignKey>
){

    fun primaryKeys() = columns.filter { it.primaryKey }
}
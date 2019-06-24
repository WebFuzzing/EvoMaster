package org.evomaster.core.database.schema

import org.evomaster.dbconstraint.TableConstraint

/**
 *
 * Should be immutable
 */
data class Table(
        val name: String,

        val columns: Set<Column>,

        val foreignKeys: Set<ForeignKey>,

        /**
         * a constraint on the rows stored in
         * the table
         */
        val tableConstraints: Set<TableConstraint> = setOf()
){

    fun primaryKeys() = columns.filter { it.primaryKey }
}
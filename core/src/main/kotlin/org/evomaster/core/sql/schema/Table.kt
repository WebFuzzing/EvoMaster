package org.evomaster.core.sql.schema

import org.evomaster.dbconstraint.TableConstraint

/**
 *
 * Should be immutable
 */
data class Table(
        /**
         * This usually would be fully qualified, ie, including schema
         */
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
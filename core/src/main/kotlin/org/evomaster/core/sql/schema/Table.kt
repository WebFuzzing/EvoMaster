package org.evomaster.core.sql.schema

import org.evomaster.dbconstraint.TableConstraint

/**
 *
 * Should be immutable
 */
data class Table(

        val id: TableId,

        val columns: Set<Column>,

        val foreignKeys: Set<ForeignKey>,

        /**
         * a constraint on the rows stored in
         * the table
         */
        val tableConstraints: Set<TableConstraint> = setOf()
){
    constructor(name: String, columns: Set<Column>, foreignKeys: Set<ForeignKey>, tableConstraints: Set<TableConstraint> = setOf())
        : this(TableId(name), columns, foreignKeys, tableConstraints)

    @Deprecated("Use id directly instead")
    val name: String
            get() = id.name

    fun primaryKeys() = columns.filter { it.primaryKey }
}
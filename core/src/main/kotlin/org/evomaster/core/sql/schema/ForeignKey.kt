package org.evomaster.core.sql.schema

/**
 *
 * Should be immutable
 */

data class ForeignKey(

        val sourceColumns: Set<Column>,

        val targetTable: String
)
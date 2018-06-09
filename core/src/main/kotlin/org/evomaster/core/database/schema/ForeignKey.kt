package org.evomaster.core.database.schema

/**
 *
 * Should be immutable
 */

data class ForeignKey(

        val sourceColumns: Set<Column>,

        val targetTable: String
)
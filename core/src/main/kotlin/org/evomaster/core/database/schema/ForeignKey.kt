package org.evomaster.core.database.schema

/**
 *
 * Should be immutable
 */

data class ForeignKey(

    val columns: Set<Column>,

    val targetTable: String
)
package org.evomaster.core.database.sql.schema

/**
 *
 * Should be immutable
 */
data class CompositeType(
        val name: String,

        val columns: List<Column>) {
}
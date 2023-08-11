package org.evomaster.core.sql.schema

/**
 *
 * Should be immutable
 */
data class CompositeType(
        val name: String,

        val columns: List<Column>) {
}
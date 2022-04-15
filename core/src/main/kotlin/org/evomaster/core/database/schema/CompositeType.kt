package org.evomaster.core.database.schema

/**
 *
 * Should be immutable
 */
data class CompositeType(
        val name: String,

        val columns: List<Column>) {
}
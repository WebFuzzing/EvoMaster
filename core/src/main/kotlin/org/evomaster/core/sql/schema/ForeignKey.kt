package org.evomaster.core.sql.schema

/**
 *
 * Should be immutable
 */

data class ForeignKey(

        /**
         * The columns in the source table that are referenced by this foreign key
         */
        val sourceColumns: List<Column>,

        /**
         * The target table that is referenced by this foreign key
         */
        val targetTableId: TableId,

        /**
         * The columns in the target table that are referenced by this foreign key.
         * The order should match the order of the source columns.
         */
        val targetColumns: List<Column>
)

package org.evomaster.core.database.schema

/**
 *
 * Should be immutable
 */
data class Column(

        val name: String,

        val type: ColumnDataType,

        val size: Int = 0,

        val primaryKey: Boolean = false,

        val nullable: Boolean = true,

        val unique: Boolean = false,

        val autoIncrement: Boolean = false,

        val foreignKeyToAutoIncrement: Boolean = false,

        val lowerBound: Int? = null,

        val upperBound: Int? = null,

        val enumValuesAsStrings: List<String>? = null,

        val similarToPatterns: List<String>? = null,

        val likePatterns: List<String>? = null

        // public boolean identity;

        //TODO something for other constraints
)
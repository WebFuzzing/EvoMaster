package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table

/**
 * this is a utility to handle Sql command generation
 */
class SQLGenerator{

    companion object {


        /**
         * select all rows of all columns, i.e., SELECT * FROM TABLE
         */
        fun genSelectAll(table : Table, condition: String? = null) : String{
            return genSelect(SQLKey.ALL.key, table, condition)
        }

        /**
         * select rows of specified columns, i.e., SELECT COL1, COL2 FROM TABLE
         */
        fun genSelect(cols : Array<String>?, table: Table, condition: String? = null) : String{
            val selected = if(cols == null) "*" else table.columns.filter { cols.contains(it.name) }.map { it.name }.joinToString(",")
            return genSelect(selected, table, condition)
        }

        /**
         * assemble all filter conditions
         */
        fun composeAndConditions(conditions : List<String>) : String{
            return conditions.joinToString(SQLKey.AND.key)
        }


        private fun composeConditions(conditions : Array<String>, operator : String) : String{
            return conditions.joinToString(operator)
        }

        /**
         * @return a list of filter condition regarding columns [cols] and its values [values] for table [table].
         */
        fun genConditions(cols: Array<String>, values: List<String>, table: Table) : List<String>{
            if(cols.size != values.size)
                IllegalArgumentException("sizes of values ${values.size} and columns ${cols.size} are not matched")

            val array = mutableListOf<String>()

            for(i in 0 until cols.size){
                val col = table.columns.find { it.name == cols[i] }?:throw IllegalArgumentException("column ${cols[i]} can not be found in the table ${table.name}")
                val condition = genCondition(col, values[i])
                if(condition.isNotBlank()) array.add(condition)

            }
            return array
        }

        private fun genCondition(col : Column, value : String) : String{
            return when(col.type){
                ColumnDataType.BOOLEAN,
                ColumnDataType.TINYINT,
                ColumnDataType.INTEGER,
                ColumnDataType.BIGINT,
                ColumnDataType.DOUBLE,
                ColumnDataType.SMALLINT,
                ColumnDataType.REAL,
                ColumnDataType.DECIMAL-> equalCondition(col.name, value)
                ColumnDataType.CHAR,
                ColumnDataType.VARCHAR -> equalCondition(col.name, "\'$value\'")
                else -> {
                    ""
                    //TODO("not sure whether to handle the types, i.e., TIMESTAMP, VARBINARY, CLOB, BLOB")
                }
            }
        }

        private fun equalCondition(left : String, right :String) :String = "$left = $right"

        /**
         * select rows of specified columns with constraints, i.e., SELECT * FROM TABLE WHERE CONDITION
         */
        private  fun genSelect(col : String, table: Table, condition: String? = null):String {
            val sql = "${SQLKey.SELECT.key} $col ${SQLKey.FROM} ${table.name}"
            if(condition == null || condition.isBlank()) return sql
            return sql +" " + SQLKey.WHERE.key+ " "+ condition
        }

    }
}

enum class SQLKey(val key : String){
    SELECT("SELECT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    INSERT("INSERT"),
    WHERE("WHERE"),
    FROM("FROM"),
    ALL("*"),
    AND(" AND ")
}





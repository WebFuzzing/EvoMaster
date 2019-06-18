package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.junit.jupiter.api.Test

class DbActionRepairTest {

    @Test
    fun testRepairEnum() {

        val statusColumn = Column("status", ColumnDataType.TEXT, 10,
                primaryKey = false,
                autoIncrement = false,
                unique = false,
                enumValuesAsStrings = listOf("A", "B"),
                nullable = false)

        val pAtColumn = Column("p_at", ColumnDataType.TIMESTAMP, 10,
                primaryKey = false,
                autoIncrement = false,
                unique = false,
                nullable = true)

        val xTableName = "x"
        val table = Table(xTableName, setOf(statusColumn, pAtColumn), setOf())


    }

}
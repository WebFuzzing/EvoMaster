package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.dbconstraint.EnumConstraint
import org.evomaster.dbconstraint.IffConstraint
import org.evomaster.dbconstraint.IsNotNullConstraint
import org.junit.jupiter.api.Test

class DbActionRepairTest {

    @Test
    fun testRepairEnum() {

        val statusColumn = Column("status", ColumnDataType.TEXT, 10,
                primaryKey = false,
                autoIncrement = false,
                unique = false,
                enumValuesAsStrings = listOf("A", "B"),
                nullable = false,
                databaseType = DatabaseType.H2)

        val pAtColumn = Column("p_at", ColumnDataType.TIMESTAMP, 10,
                primaryKey = false,
                autoIncrement = false,
                unique = false,
                nullable = true,
                databaseType = DatabaseType.H2)

        val xTableName = "x"

        val left = EnumConstraint(xTableName, statusColumn.name, listOf("b"))
        val right = IsNotNullConstraint(xTableName, pAtColumn.name)
        val tableConstraint = IffConstraint(xTableName, left, right)

        val table = Table(xTableName, setOf(statusColumn, pAtColumn), setOf())


    }

}
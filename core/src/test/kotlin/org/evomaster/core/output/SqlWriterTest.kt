package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.action.EvaluatedDbAction
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SqlWriterTest {

    @Test
    fun testHandleFKWithMultiColumnForeignKey() {
        val targetTableId = TableId("target")
        val pkCol1 = Column("pk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val pkCol2 = Column("pk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val targetTable = Table(targetTableId, setOf(pkCol1, pkCol2), emptySet())

        val sourceTableId = TableId("source")
        val fkCol1 = Column("fk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fkCol2 = Column("fk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val foreignKey = ForeignKey(listOf(fkCol1, fkCol2), targetTableId, listOf(pkCol1, pkCol2))
        val sourceTable = Table(sourceTableId, setOf(fkCol1, fkCol2), setOf(foreignKey))

        val pkUniqueId = 1L
        val pkGene1 = SqlPrimaryKeyGene("pk1", targetTableId, IntegerGene("pk1", 10), pkUniqueId)
        val pkGene2 = SqlPrimaryKeyGene("pk2", targetTableId, IntegerGene("pk2", 20), pkUniqueId)
        val targetAction = SqlAction(targetTable, setOf(pkCol1, pkCol2), 100L, listOf(pkGene1, pkGene2))
        targetAction.setLocalId("0")

        val fkUniqueId = 2L
        val fkGene1 = SqlForeignKeyGene("fk1", fkUniqueId, targetTableId, "pk1", false,
            otherSourceColumnsInCompositeFK = listOf("fk2"), uniqueIdOfPrimaryKey = pkUniqueId)
        val fkGene2 = SqlForeignKeyGene("fk2", fkUniqueId, targetTableId, "pk2", false,
            otherSourceColumnsInCompositeFK = listOf("fk1"), uniqueIdOfPrimaryKey = pkUniqueId)
        val sourceAction = SqlAction(sourceTable, setOf(fkCol1, fkCol2), 101L, listOf(fkGene1, fkGene2))
        sourceAction.setLocalId("1")

        val format = OutputFormat.JAVA_JUNIT_5
        val lines = Lines(format)
        val dbInitialization = listOf(
            EvaluatedDbAction(targetAction, SqlActionResult("0").apply { setInsertExecutionResult(true) }),
            EvaluatedDbAction(sourceAction, SqlActionResult("1").apply { setInsertExecutionResult(true) })
        )

        SqlWriter.handleDbInitialization(
            format = format,
            dbInitialization = dbInitialization,
            lines = lines,
            insertionVars = mutableListOf(),
            skipFailure = false
        )

        val output = lines.toString()
        assertTrue(output.contains(".insertInto(\"source\", 101L)"))
        assertTrue(output.contains(".d(\"fk1\", \"10\")"))
        assertTrue(output.contains(".d(\"fk2\", \"20\")"))
    }
}

package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.sql.schema.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlActionUtilsTest {

    @Test
    fun testValidSingleColumnForeignKey() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val targetTable = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(sourceColumns = listOf(fkColumn), targetTableId = targetTable.id, targetColumns = listOf(idColumn))

        val sourceTable = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val integerGene = IntegerGene("Id", 42, 0, 100)


        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id", targetTable.id, integerGene, insertId0)
        val action0 = SqlAction(targetTable, setOf(idColumn), insertId0, listOf(pkGeneTable0))

        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Id", insertId1, targetTable.id, "Id", false, insertId0)
        val pkGeneTable1 = SqlPrimaryKeyGene("Id", sourceTable.id, fkGene, insertId1)

        val action1 = SqlAction(sourceTable, setOf(fkColumn), insertId1, listOf(pkGeneTable1))


        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.isValidForeignKeys(actions))
    }

    @Test
    fun testValidMultiColumnForeignKeys() {
        val idColumn1 = Column("Id1", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val idColumn2 = Column("Id2", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val targetTable = Table("Table0", setOf(idColumn1, idColumn2), setOf())

        val fkColumn1 = Column("Fk1", ColumnDataType.INTEGER, 10,
            primaryKey = false,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val fkColumn2 = Column("Fk2", ColumnDataType.INTEGER, 10,
            primaryKey = false,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(
            sourceColumns = listOf(fkColumn1, fkColumn2),
            targetTableId = targetTable.id,
            targetColumns = listOf(idColumn1, idColumn2)
        )

        val sourceTable = Table("Table1", setOf(fkColumn1, fkColumn2), setOf(foreignKey))

        val integerGene1 = IntegerGene("Id1", 42, 0, 100)
        val integerGene2 = IntegerGene("Id2", 84, 0, 100)

        val insertId0 = 1001L
        val pkGene1Table0 = SqlPrimaryKeyGene("Id1", targetTable.id, integerGene1, insertId0)
        val pkGene2Table0 = SqlPrimaryKeyGene("Id2", targetTable.id, integerGene2, insertId0)
        val action0 = SqlAction(targetTable, setOf(idColumn1, idColumn2), insertId0, listOf(pkGene1Table0, pkGene2Table0))

        val insertId1 = 1002L
        val fkGene1 = SqlForeignKeyGene("Fk1", insertId1, targetTable.id, "Id1", false, insertId0)
        val fkGene2 = SqlForeignKeyGene("Fk2", insertId1, targetTable.id, "Id2", false, insertId0)

        val action1 = SqlAction(sourceTable, setOf(fkColumn1, fkColumn2), insertId1, listOf(fkGene1, fkGene2))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.isValidForeignKeys(actions))
    }

    @Test
    fun testInvalidMultiColumnForeignKeys() {
        val idColumn1 = Column("Id1", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)

        val idColumn2 = Column("Id2", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)

        val targetTable = Table("Table0", setOf(idColumn1, idColumn2), setOf())

        val fkColumn1 = Column("Fk1", ColumnDataType.INTEGER, 10,
            databaseType = DatabaseType.H2)

        val fkColumn2 = Column("Fk2", ColumnDataType.INTEGER, 10,
            databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(
            sourceColumns = listOf(fkColumn1, fkColumn2),
            targetTableId = targetTable.id,
            targetColumns = listOf(idColumn1, idColumn2)
        )

        val sourceTable = Table("Table1", setOf(fkColumn1, fkColumn2), setOf(foreignKey))

        val integerGene0 = IntegerGene("Id1", 21, 0, 100)
        val integerGene1 = IntegerGene("Id1", 42, 0, 100)
        val integerGene2 = IntegerGene("Id2", 84, 0, 100)

        // Inserts (21,84) to Table0(Id1,Id2)
        val insertId0 = 1001L
        val pkGene1Table0_0 = SqlPrimaryKeyGene("Id1", targetTable.id, integerGene0, insertId0)
        val pkGene2Table0_0 = SqlPrimaryKeyGene("Id2", targetTable.id, integerGene2, insertId0)
        val action0 = SqlAction(targetTable, setOf(idColumn1, idColumn2), insertId0, listOf(pkGene1Table0_0, pkGene2Table0_0))

        // Inserts (42,84) to Table0(Id1,Id2)
        val insertId1 = 1002L
        val pkGene1Table0_1 = SqlPrimaryKeyGene("Id1", targetTable.id, integerGene1, insertId1)
        val pkGene2Table0_1 = SqlPrimaryKeyGene("Id2", targetTable.id, integerGene2, insertId1)
        val action1 = SqlAction(targetTable, setOf(idColumn1, idColumn2), insertId1, listOf(pkGene1Table0_1, pkGene2Table0_1))

        val insertId2 = 1003L
        // Points to DIFFERENT but VALID PK IDs (1001L and 1002L)
        val fkGene1 = SqlForeignKeyGene("Fk1", insertId2, targetTable.id, "Id1", false, insertId0)
        val fkGene2 = SqlForeignKeyGene("Fk2", insertId2, targetTable.id, "Id2", false, insertId1)

        val action2 = SqlAction(sourceTable, setOf(fkColumn1, fkColumn2), insertId2, listOf(fkGene1, fkGene2))

        val actions = mutableListOf(action0, action1, action2)

        assertFalse(SqlActionUtils.isValidForeignKeys(actions))
    }

    @Test
    fun testInvalidForeignKeyReferringToWrongTable() {
        val idColumnTable0 = Column("Id0", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)

        val table0 = Table("Table0", setOf(idColumnTable0), setOf())

        val idColumnTable1 = Column("Id1", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            databaseType = DatabaseType.H2)

        val table1 = Table("Table1", setOf(idColumnTable1), setOf())

        val fkColumn = Column("Fk", ColumnDataType.INTEGER, 10,
            databaseType = DatabaseType.H2)

        // FK in Table2 points to Table1
        val foreignKey = ForeignKey(
            sourceColumns = listOf(fkColumn),
            targetTableId = table1.id,
            targetColumns = listOf(idColumnTable1)
        )

        val table2 = Table("Table2", setOf(fkColumn), setOf(foreignKey))

        val integerGene0 = IntegerGene("Id0", 1, 0, 100)

        // Action 0: Insert into Table0 (ID = 1, uniqueId = 1001L)
        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id0", table0.id, integerGene0, insertId0)
        val action0 = SqlAction(table0, setOf(idColumnTable0), insertId0, listOf(pkGeneTable0))

        // Action 1: Insert into Table2, but FK points to uniqueId = 1001L (which belongs to Table0, NOT Table1)
        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Fk", insertId1, table1.id, "Id1", false, insertId0)

        val action1 = SqlAction(table2, setOf(fkColumn), insertId1, listOf(fkGene))

        val actions = mutableListOf(action0, action1)

        // This SHOULD be false, because the FK points to an insertion in Table0, but the constraint says it should point to Table1
        assertFalse(SqlActionUtils.isValidForeignKeys(actions))
    }
    
}

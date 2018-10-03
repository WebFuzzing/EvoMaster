package org.evomaster.core.database

import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DbActionRepairTest {


    @Test
    fun testNotRepeatedStringValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(uniqueColumn.name, "stringValue1", 0, 10)
        val action1 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)


        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))

        assertTrue(DbAction.verifyActions(actions))

    }

    @Test
    fun testNotUniqueColumn() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = false)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action1 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)


        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))

        assertTrue(DbAction.verifyActions(actions))

    }


    @Test
    fun testRepeatedStringValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(DbAction.verifyActions(actions))
    }


    @Test
    fun testRepeatedStringValuesNoAttempts() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness, maxNumberOfAttemptsToRepairAnAction = 0)

        assertEquals(false, listWasNotTruncated)
    }


    @Test
    fun testRepeatedBooleanValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.BOOLEAN, 1, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = BooleanGene(uniqueColumn.name, true)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(DbAction.verifyActions(actions))
    }

    @Test
    fun testRepeatedIntegerValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.INTEGER, 11, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = IntegerGene(uniqueColumn.name, 42)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(DbAction.verifyActions(actions))
    }


    @Test
    fun testIllegalMaxNumberOfAttempts() {
        val randomness = Randomness()
        try {
            DbAction.repairBrokenDbActionsList(mutableListOf(), randomness, -1)
            fail<Exception>("Expected IllegalArgumentException when maxNumberOfAttemptsToRepairAction argument is negative")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testUnrecoverableActionList() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.BOOLEAN, 1, unique = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = BooleanGene(uniqueColumn.name, true)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val gene2 = gene0.copy()
        val action2 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene2))

        val actions = mutableListOf<DbAction>(action0, action1, action2)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))
        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(false, listWasNotTruncated)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))
        assertTrue(DbAction.verifyActions(actions))

        assertEquals(2,actions.size)
    }


    @Test
    fun testPrimaryKey() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.INTEGER, 11,
                unique = false,
                primaryKey = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val innerGene = IntegerGene(uniqueColumn.name, 42)
        val uniqueId = 1001L
        val gene0 = SqlPrimaryKeyGene(uniqueColumn.name, aTable.name, innerGene, uniqueId)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))

        val randomness = Randomness()
        val repairWasSuccessful = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, repairWasSuccessful)
        assertTrue(DbAction.verifyActions(actions))
    }

    @Test
    fun testUniqueAutoIncrementPrimaryKey() {

        val uniqueColumn = Column("Id", ColumnDataType.INTEGER, 11,
                unique = true,
                autoIncrement = true)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = SqlAutoIncrementGene(uniqueColumn.name)
        val action0 = DbAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = SqlAutoIncrementGene(uniqueColumn.name)
        val action1 = DbAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))

        assertTrue(DbAction.verifyActions(actions))

    }

    @Test
    fun testForeignKeyColumn() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val integerGene = IntegerGene("Id", 42, 0, 10)


        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id", "Table0", integerGene, insertId0)
        val action0 = DbAction(table0, setOf(idColumn), insertId0, listOf(pkGeneTable0))

        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Id", insertId1, "Table0", false, insertId0)
        val pkGeneTable1 = SqlPrimaryKeyGene("Id", "Table1", fkGene, insertId1)

        val action1 = DbAction(table1, setOf(fkColumn), insertId1, listOf(pkGeneTable1))


        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))

        assertTrue(DbAction.verifyActions(actions))

    }


    @Test
    fun testForeignKeyToAutoIncrement() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = true,
                unique = false)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val autoIncrementGene = SqlAutoIncrementGene("Id")

        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene, insertId0)
        val action0 = DbAction(table0, setOf(idColumn), insertId0, listOf(pkGeneTable0))

        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Id", insertId1, "Table0", false, insertId0)
        val pkGeneTable1 = SqlPrimaryKeyGene("Id", "Table1", fkGene, insertId1)

        val action1 = DbAction(table1, setOf(fkColumn), insertId1, listOf(pkGeneTable1))


        val actions = mutableListOf<DbAction>(action0, action1)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertTrue(DbAction.verifyUniqueColumns(actions))

        assertTrue(DbAction.verifyActions(actions))

    }

    @Test
    fun testDuplicatedForeignKeyToAutoIncrement() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = true,
                unique = false)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))


        val insertId0 = 1001L
        val autoIncrementGene0 = SqlAutoIncrementGene("Id")
        val pkGene0 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene0, insertId0)
        val action0 = DbAction(table0, setOf(idColumn), insertId0, listOf(pkGene0))

        val insertId1 = 1002L
        val autoIncrementGene1 = SqlAutoIncrementGene("Id")
        val pkGene1 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene1, insertId1)
        val action1 = DbAction(table0, setOf(idColumn), insertId0, listOf(pkGene1))


        val insertId2 = 1003L
        val fkGene0 = SqlForeignKeyGene("Id", insertId2, "Table0", false, insertId0)
        val pkGene2 = SqlPrimaryKeyGene("Id", "Table1", fkGene0, insertId2)
        val action2 = DbAction(table1, setOf(fkColumn), insertId2, listOf(pkGene2))

        val insertId3 = 1003L
        val fkGene1 = SqlForeignKeyGene("Id", insertId3, "Table0", false, insertId0)
        val pkGene3 = SqlPrimaryKeyGene("Id", "Table1", fkGene1, insertId3)
        val action3 = DbAction(table1, setOf(fkColumn), insertId3, listOf(pkGene3))


        val actions = mutableListOf<DbAction>(action0, action1, action2, action3)

        assertTrue(DbAction.verifyForeignKeys(actions))
        assertFalse(DbAction.verifyUniqueColumns(actions))

        assertFalse(DbAction.verifyActions(actions))


        val randomness = Randomness()
        val listWasNotTruncated = DbAction.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(DbAction.verifyActions(actions))
    }

}
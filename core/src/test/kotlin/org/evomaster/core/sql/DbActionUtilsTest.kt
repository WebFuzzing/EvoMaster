package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DbActionUtilsTest {


    private val randomness = Randomness()

    @BeforeEach
    fun init(){
        randomness.updateSeed(42)
    }

    @Test
    fun testMultiColumnPrimaryKey() {

        val x = Column("x", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val y = Column("y", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val tableName = "ATable"
        val table = Table(tableName, setOf(x, y), setOf())


        val gx0 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 1)
        val gy0 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 66), 2)
        val action0 = SqlAction(table, setOf(x, y), 0L, listOf(gx0, gy0))

        assertTrue(SqlActionUtils.verifyUniqueColumns(listOf(action0)))

        //second action with exact same PK
        val gx1 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 3)
        val gy1 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 66), 4)
        val action1 = SqlAction(table, setOf(x, y), 1L, listOf(gx1, gy1))

        //validation should fail
        assertFalse(SqlActionUtils.verifyUniqueColumns(listOf(action0, action1)))


        //third action with inverted values
        val gx2 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 66), 5)
        val gy2 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 42), 6)
        val action2 = SqlAction(table, setOf(x, y), 2L, listOf(gx2, gy2))

        //should be fine
        assertTrue(SqlActionUtils.verifyUniqueColumns(listOf(action0, action2)))


        //fourth action with one column as same value
        val gx3 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 7)
        val gy3 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 1234), 8)
        val action3 = SqlAction(table, setOf(x, y), 3L, listOf(gx3, gy3))

        //should still be fine, as PK is composed of 2 columns
        assertTrue(SqlActionUtils.verifyUniqueColumns(listOf(action0, action3)))
    }

    @Test
    fun testMultiColumnPrimaryKeyWithUnique() {

        /*
            2 columns x and y
            They both define the PK, but x is also unique.
            Likely this would not make much sense, as then could just have
            x as PK. but as it is easy to support, we just handle it
         */
        val x = Column("x", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = true,
                databaseType = DatabaseType.H2)

        val y = Column("y", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val tableName = "ATable"
        val table = Table(tableName, setOf(x, y), setOf())


        val gx0 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 1)
        val gy0 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 66), 2)
        val action0 = SqlAction(table, setOf(x, y), 0L, listOf(gx0, gy0))

        assertTrue(SqlActionUtils.verifyUniqueColumns(listOf(action0)))

        //second action with exact same PK
        val gx1 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 3)
        val gy1 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 66), 4)
        val action1 = SqlAction(table, setOf(x, y), 1L, listOf(gx1, gy1))

        //validation should fail
        assertFalse(SqlActionUtils.verifyUniqueColumns(listOf(action0, action1)))


        //third action with different y
        val gx2 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 42), 5)
        val gy2 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 77), 6)
        val action2 = SqlAction(table, setOf(x, y), 2L, listOf(gx2, gy2))

        //should still fail, due to unique x
        assertFalse(SqlActionUtils.verifyUniqueColumns(listOf(action0, action2)))


        //fourth action with same y, and different x
        val gx3 = SqlPrimaryKeyGene(x.name, tableName, IntegerGene(x.name, 1234), 7)
        val gy3 = SqlPrimaryKeyGene(y.name, tableName, IntegerGene(y.name, 66), 8)
        val action3 = SqlAction(table, setOf(x, y), 3L, listOf(gx3, gy3))

        //should be fine, as PK is composed of 2 columns, and y does not need to be unique
        assertTrue(SqlActionUtils.verifyUniqueColumns(listOf(action0, action3)))
    }


    @Test
    fun testNotRepeatedStringValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(uniqueColumn.name, "stringValue1", 0, 10)
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)


        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))

        assertTrue(SqlActionUtils.verifyActions(actions))

    }

    @Test
    fun testNotUniqueColumn() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = false,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)


        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))

        assertTrue(SqlActionUtils.verifyActions(actions))

    }


    @Test
    fun testRepeatedStringValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(SqlActionUtils.verifyActions(actions))
    }


    @Test
    fun testRepeatedStringValuesNoAttempts() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.VARCHAR, 10, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = StringGene(uniqueColumn.name, "stringValue0", 0, 10)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness, maxNumberOfAttemptsToRepairAnAction = 0)

        assertEquals(false, listWasNotTruncated)
    }


    @Test
    fun testRepeatedBooleanValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.BOOLEAN, 1, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = BooleanGene(uniqueColumn.name, true)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(SqlActionUtils.verifyActions(actions))
    }

    @Test
    fun testRepeatedIntegerValues() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.INTEGER, 11, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = IntegerGene(uniqueColumn.name, 42)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(SqlActionUtils.verifyActions(actions))
    }


    @Test
    fun testIllegalMaxNumberOfAttempts() {
        try {
            SqlActionUtils.repairBrokenDbActionsList(mutableListOf(), randomness, -1)
            fail<Exception>("Expected IllegalArgumentException when maxNumberOfAttemptsToRepairAction argument is negative")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testUnrecoverableActionList() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.BOOLEAN, 1, unique = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = BooleanGene(uniqueColumn.name, true)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val gene2 = gene0.copy()
        val action2 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene2))

        val actions = mutableListOf(action0, action1, action2)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))
        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(false, listWasNotTruncated)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))
        assertTrue(SqlActionUtils.verifyActions(actions))

        assertEquals(2, actions.size)
    }


    @Test
    fun testPrimaryKey() {

        val uniqueColumn = Column("uniqueColumn", ColumnDataType.INTEGER, 11,
                unique = false,
                primaryKey = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val innerGene = IntegerGene(uniqueColumn.name, 42)
        val uniqueId = 1001L
        val gene0 = SqlPrimaryKeyGene(uniqueColumn.name, aTable.name, innerGene, uniqueId)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = gene0.copy()
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val repairWasSuccessful = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, repairWasSuccessful)
        assertTrue(SqlActionUtils.verifyActions(actions))
    }

    @Test
    fun testUniqueAutoIncrementPrimaryKey() {

        val uniqueColumn = Column("Id", ColumnDataType.INTEGER, 11,
                unique = true,
                autoIncrement = true,
                databaseType = DatabaseType.H2)

        val aTable = Table("myTable", setOf(uniqueColumn), HashSet<ForeignKey>())

        val gene0 = SqlAutoIncrementGene(uniqueColumn.name)
        val action0 = SqlAction(aTable, setOf(uniqueColumn), 0L, mutableListOf(gene0))

        val gene1 = SqlAutoIncrementGene(uniqueColumn.name)
        val action1 = SqlAction(aTable, setOf(uniqueColumn), 1L, mutableListOf(gene1))

        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))

        assertTrue(SqlActionUtils.verifyActions(actions))

    }

    @Test
    fun testForeignKeyColumn() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val integerGene = IntegerGene("Id", 42, 0, 10)


        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id", "Table0", integerGene, insertId0)
        val action0 = SqlAction(table0, setOf(idColumn), insertId0, listOf(pkGeneTable0))

        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Id", insertId1, "Table0", false, insertId0)
        val pkGeneTable1 = SqlPrimaryKeyGene("Id", "Table1", fkGene, insertId1)

        val action1 = SqlAction(table1, setOf(fkColumn), insertId1, listOf(pkGeneTable1))


        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))

        assertTrue(SqlActionUtils.verifyActions(actions))

    }


    @Test
    fun testForeignKeyToAutoIncrement() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = true,
                unique = false,
                databaseType = DatabaseType.H2)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val autoIncrementGene = SqlAutoIncrementGene("Id")

        val insertId0 = 1001L
        val pkGeneTable0 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene, insertId0)
        val action0 = SqlAction(table0, setOf(idColumn), insertId0, listOf(pkGeneTable0))

        val insertId1 = 1002L
        val fkGene = SqlForeignKeyGene("Id", insertId1, "Table0", false, insertId0)
        val pkGeneTable1 = SqlPrimaryKeyGene("Id", "Table1", fkGene, insertId1)

        val action1 = SqlAction(table1, setOf(fkColumn), insertId1, listOf(pkGeneTable1))


        val actions = mutableListOf(action0, action1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertTrue(SqlActionUtils.verifyUniqueColumns(actions))

        assertTrue(SqlActionUtils.verifyActions(actions))

    }

    @Test
    fun testDuplicatedForeignKeyToAutoIncrement() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = true,
                unique = false,
                databaseType = DatabaseType.H2)

        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                autoIncrement = false,
                unique = false,
                databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)

        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))


        val insertId0 = 1001L
        val autoIncrementGene0 = SqlAutoIncrementGene("Id")
        val pkGene0 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene0, insertId0)
        val action0 = SqlAction(table0, setOf(idColumn), insertId0, listOf(pkGene0))

        val insertId1 = 1002L
        val autoIncrementGene1 = SqlAutoIncrementGene("Id")
        val pkGene1 = SqlPrimaryKeyGene("Id", "Table0", autoIncrementGene1, insertId1)
        val action1 = SqlAction(table0, setOf(idColumn), insertId0, listOf(pkGene1))


        val insertId2 = 1003L
        val fkGene0 = SqlForeignKeyGene("Id", insertId2, "Table0", false, insertId0)
        val pkGene2 = SqlPrimaryKeyGene("Id", "Table1", fkGene0, insertId2)
        val action2 = SqlAction(table1, setOf(fkColumn), insertId2, listOf(pkGene2))

        val insertId3 = 1003L
        val fkGene1 = SqlForeignKeyGene("Id", insertId3, "Table0", false, insertId0)
        val pkGene3 = SqlPrimaryKeyGene("Id", "Table1", fkGene1, insertId3)
        val action3 = SqlAction(table1, setOf(fkColumn), insertId3, listOf(pkGene3))


        val actions = mutableListOf(action0, action1, action2, action3)

        val ind = RestIndividual(mutableListOf(), SampleType.RANDOM,null,actions,null,-1)

        assertTrue(SqlActionUtils.verifyForeignKeys(actions))
        assertFalse(SqlActionUtils.verifyUniqueColumns(actions))

        assertFalse(SqlActionUtils.verifyActions(actions))

        val listWasNotTruncated = SqlActionUtils.repairBrokenDbActionsList(actions, randomness)

        assertEquals(true, listWasNotTruncated)
        assertTrue(SqlActionUtils.verifyActions(actions))
    }

    @Test
    fun testSortTableBasedFk(){
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = true,
            unique = false,
            databaseType = DatabaseType.H2)
        val table0 = Table("Table0", setOf(idColumn), setOf())

        val fkColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)
        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = table0.name)
        val table1 = Table("Table1", setOf(fkColumn), setOf(foreignKey))

        val set = listOf(table0, table0, table1)
        val sorted = SqlActionUtils.sortTable(set)
        assertEquals(listOf(table1, table0, table0), sorted)

        val rsorted = SqlActionUtils.sortTable(set.distinct(), true)
        assertEquals(listOf(table0, table1), rsorted)
    }
}

package org.evomaster.core.search.gene.sql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlForeignKeyGeneTest {

    @Test
    fun testRandomizeSingleColumnForeignKey() {
        val targetTableId = TableId("targetTable")
        val pkColumn = Column("col_pk", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val targetTable = Table(targetTableId, setOf(pkColumn), emptySet())

        val uniqueId0 = 1L
        val pkGene1 = SqlPrimaryKeyGene("col_pk", targetTableId, IntegerGene("col_pk", 1), uniqueId0)
        val action1 = SqlAction(targetTable, setOf(pkColumn), uniqueId0, listOf(pkGene1))

        val uniqueId1 = 2L
        val pkGene2 = SqlPrimaryKeyGene("col_pk", targetTableId, IntegerGene("col_pk", 2), uniqueId1)
        val action2 = SqlAction(targetTable, setOf(pkColumn), uniqueId1, listOf(pkGene2))

        val sourceTableId = TableId("sourceTable")
        val fkColumn = Column("col_fk", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val foreignKey = ForeignKey(listOf(fkColumn), targetTableId, listOf(pkColumn))
        val sourceTable = Table(sourceTableId, setOf(fkColumn), setOf(foreignKey))
        val uniqueId2 = 3L
        val fkGene = SqlForeignKeyGene("col_fk", uniqueId2, targetTableId, "col_pk", nullable = false)
        val action3 = SqlAction(sourceTable, setOf(fkColumn), uniqueId2, listOf(fkGene))

        RestIndividual(
            resourceCalls = mutableListOf(),
            sampleType = SampleType.RANDOM,
            dbInitialization = mutableListOf(action1, action2, action3)
        )

        val randomness = Randomness()
        randomness.updateSeed(42)

        fkGene.randomize(randomness, tryToForceNewValue = false)
        assertTrue(fkGene.isBound())
        assertTrue(fkGene.uniqueIdOfPrimaryKey == uniqueId0 || fkGene.uniqueIdOfPrimaryKey == uniqueId1)
        assertTrue(SqlActionUtils.isValidActions(listOf(action1, action2, action3)))

    }

    @Test
    fun testRandomizeMultiColumnForeignKey() {
        val targetTableId = TableId("targetTable")
        val pkColumn1 = Column("col_pk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val pkColumn2 = Column("col_pk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val targetTable = Table(targetTableId, setOf(pkColumn1, pkColumn2), emptySet())

        // First row in target table
        val uniqueId0 = 1L
        val pkGene1_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 1), uniqueId0)
        val pkGene1_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 10), uniqueId0)
        val action1 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueId0, listOf(pkGene1_1, pkGene1_2))

        // Second row in target table
        val uniqueId1 = 2L
        val pkGene2_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 2), uniqueId1)
        val pkGene2_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 20), uniqueId1)
        val action2 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueId1, listOf(pkGene2_1, pkGene2_2))

        val sourceTableId = TableId("sourceTable")
        val fkColumn1 = Column("col_fk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fkColumn2 = Column("col_fk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(listOf(fkColumn1, fkColumn2), targetTableId, listOf(pkColumn1, pkColumn2))
        val sourceTable = Table(sourceTableId, setOf(fkColumn1, fkColumn2), setOf(foreignKey))

        // First row in source table
        val uniqueId2 = 3L
        val fkGene1 = SqlForeignKeyGene("col_fk1", uniqueId2, targetTableId, "col_pk1", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk2"))
        val fkGene2 = SqlForeignKeyGene("col_fk2", uniqueId2, targetTableId, "col_pk2", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk1"))
        val action3 = SqlAction(sourceTable, setOf(fkColumn1, fkColumn2), uniqueId2, listOf(fkGene1, fkGene2))

        RestIndividual(
            resourceCalls = mutableListOf(),
            sampleType = SampleType.RANDOM,
            dbInitialization = mutableListOf(action1, action2, action3)
        )

        // Randomize FK genes
        val randomness = Randomness()
        randomness.updateSeed(42)
        fkGene1.randomize(randomness, tryToForceNewValue = false)
        fkGene2.randomize(randomness, tryToForceNewValue = false)

        assertTrue(SqlActionUtils.isValidActions(listOf(action1, action2, action3)))
    }



    @Test
    fun testRandomizeMultiColumnForeignKeyWithDifferentInsertions() {
        val targetTableId = TableId("targetTable")
        val pkColumn1 = Column("col_pk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val pkColumn2 = Column("col_pk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val targetTable = Table(targetTableId, setOf(pkColumn1, pkColumn2), emptySet())

        // First row in target table
        val uniqueId0 = 1L
        val pkGene1_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 1), uniqueId0)
        val pkGene1_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 10), uniqueId0)
        val action1 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueId0, listOf(pkGene1_1, pkGene1_2))

        // Second row in target table
        val uniqueId1 = 2L
        val pkGene2_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 2), uniqueId1)
        val pkGene2_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 20), uniqueId1)
        val action2 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueId1, listOf(pkGene2_1, pkGene2_2))

        val sourceTableId = TableId("sourceTable")
        val fkColumn1 = Column("col_fk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fkColumn2 = Column("col_fk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(listOf(fkColumn1, fkColumn2), targetTableId, listOf(pkColumn1, pkColumn2))
        val sourceTable = Table(sourceTableId, setOf(fkColumn1, fkColumn2), setOf(foreignKey))

        // First row in source table
        val uniqueId2_multi = 3L
        val fkGene1 = SqlForeignKeyGene("col_fk1", uniqueId2_multi, targetTableId, "col_pk1", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk2"))
        val fkGene2 = SqlForeignKeyGene("col_fk2", uniqueId2_multi, targetTableId, "col_pk2", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk1"))
        val action3 = SqlAction(sourceTable, setOf(fkColumn1, fkColumn2), uniqueId2_multi, listOf(fkGene1, fkGene2))

        // Second row in source table
        val uniqueId3_multi = 4L
        val fkGene3 = SqlForeignKeyGene("col_fk1", uniqueId3_multi, targetTableId, "col_pk1", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk2"))
        val fkGene4 = SqlForeignKeyGene("col_fk2", uniqueId3_multi, targetTableId, "col_pk2", nullable = false, otherSourceColumnsInCompositeFK = listOf("col_fk1"))
        val action4 = SqlAction(sourceTable, setOf(fkColumn1, fkColumn2), uniqueId3_multi, listOf(fkGene3, fkGene4))

        RestIndividual(
            resourceCalls = mutableListOf(),
            sampleType = SampleType.RANDOM,
            dbInitialization = mutableListOf(action1, action2, action3, action4)
        )

        // Randomize FK genes
        val randomness = Randomness()
        randomness.updateSeed(42)
        fkGene1.randomize(randomness, tryToForceNewValue = false)
        fkGene2.randomize(randomness, tryToForceNewValue = false)
        fkGene3.randomize(randomness, tryToForceNewValue = false)
        fkGene4.randomize(randomness, tryToForceNewValue = false)

        assertTrue(SqlActionUtils.isValidActions(listOf(action1, action2, action3, action4)))
    }

    @Test
    fun testTwoMultiColumnForeignKeysToSameTable() {
        val targetTableId = TableId("targetTable")
        val pkColumn1 = Column("col_pk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val pkColumn2 = Column("col_pk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val targetTable = Table(targetTableId, setOf(pkColumn1, pkColumn2), emptySet())

        // Row 1 in target table
        val uniqueIdTarget1 = 1L
        val pkGene1_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 1), uniqueIdTarget1)
        val pkGene1_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 10), uniqueIdTarget1)
        val actionTarget1 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueIdTarget1, listOf(pkGene1_1, pkGene1_2))

        // Row 2 in target table
        val uniqueIdTarget2 = 2L
        val pkGene2_1 = SqlPrimaryKeyGene("col_pk1", targetTableId, IntegerGene("col_pk1", 2), uniqueIdTarget2)
        val pkGene2_2 = SqlPrimaryKeyGene("col_pk2", targetTableId, IntegerGene("col_pk2", 20), uniqueIdTarget2)
        val actionTarget2 = SqlAction(targetTable, setOf(pkColumn1, pkColumn2), uniqueIdTarget2, listOf(pkGene2_1, pkGene2_2))

        val sourceTableId = TableId("sourceTable")
        val fk1_1 = Column("fk1_1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fk1_2 = Column("fk1_2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fk2_1 = Column("fk2_1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fk2_2 = Column("fk2_2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val foreignKey1 = ForeignKey(listOf(fk1_1, fk1_2), targetTableId, listOf(pkColumn1, pkColumn2))
        val foreignKey2 = ForeignKey(listOf(fk2_1, fk2_2), targetTableId, listOf(pkColumn1, pkColumn2))

        val sourceTable = Table(sourceTableId, setOf(fk1_1, fk1_2, fk2_1, fk2_2), setOf(foreignKey1, foreignKey2))

        // A single row in source table that points to TWO different rows in target table
        val uniqueIdSource = 3L
        val fkGene1_1 = SqlForeignKeyGene("fk1_1", uniqueIdSource, targetTableId, "col_pk1", nullable = false, otherSourceColumnsInCompositeFK = listOf("fk1_2"))
        val fkGene1_2 = SqlForeignKeyGene("fk1_2", uniqueIdSource, targetTableId, "col_pk2", nullable = false, otherSourceColumnsInCompositeFK = listOf("fk1_1"))
        val fkGene2_1 = SqlForeignKeyGene("fk2_1", uniqueIdSource, targetTableId, "col_pk1", nullable = false, otherSourceColumnsInCompositeFK = listOf("fk2_2"))
        val fkGene2_2 = SqlForeignKeyGene("fk2_2", uniqueIdSource, targetTableId, "col_pk2", nullable = false, otherSourceColumnsInCompositeFK = listOf("fk2_1"))

        val actionSource = SqlAction(sourceTable, setOf(fk1_1, fk1_2, fk2_1, fk2_2), uniqueIdSource,
            listOf(fkGene1_1, fkGene1_2, fkGene2_1, fkGene2_2))

        RestIndividual(
            resourceCalls = mutableListOf(),
            sampleType = SampleType.RANDOM,
            dbInitialization = mutableListOf(actionTarget1, actionTarget2, actionSource)
        )

        // For a multi-column FK, both genes in the same FK must point to the same row in the target table.
        // But the two different FKs (FK1 and FK2) can point to different rows.

        // We manually bind them to different rows to test the specific scenario
        fkGene1_1.uniqueIdOfPrimaryKey = uniqueIdTarget1
        fkGene1_2.uniqueIdOfPrimaryKey = uniqueIdTarget1

        fkGene2_1.uniqueIdOfPrimaryKey = uniqueIdTarget2
        fkGene2_2.uniqueIdOfPrimaryKey = uniqueIdTarget2

        assertTrue(SqlActionUtils.isValidActions(listOf(actionTarget1, actionTarget2, actionSource)))

        // Now test that randomization also works and keeps it valid
        val randomness = Randomness()
        randomness.updateSeed(42)
        fkGene1_1.randomize(randomness, tryToForceNewValue = false)
        fkGene1_2.randomize(randomness, tryToForceNewValue = false)
        fkGene2_1.randomize(randomness, tryToForceNewValue = false)
        fkGene2_2.randomize(randomness, tryToForceNewValue = false)

        assertTrue(SqlActionUtils.isValidActions(listOf(actionTarget1, actionTarget2, actionSource)))
    }

    @Test
    fun testTwoDifferentForeignKeysToSameTableInSameAction() {
        val targetTableId = TableId("targetTable")
        val pkColumn = Column("col_pk", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true)
        val targetTable = Table(targetTableId, setOf(pkColumn), emptySet())

        // Row 1 in target table
        val uniqueIdTarget1 = 1L
        val pkGene1 = SqlPrimaryKeyGene("col_pk", targetTableId, IntegerGene("col_pk", 1), uniqueIdTarget1)
        val actionTarget1 = SqlAction(targetTable, setOf(pkColumn), uniqueIdTarget1, listOf(pkGene1))

        // Row 2 in target table
        val uniqueIdTarget2 = 2L
        val pkGene2 = SqlPrimaryKeyGene("col_pk", targetTableId, IntegerGene("col_pk", 2), uniqueIdTarget2)
        val actionTarget2 = SqlAction(targetTable, setOf(pkColumn), uniqueIdTarget2, listOf(pkGene2))

        val sourceTableId = TableId("sourceTable")
        val fk1 = Column("fk1", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val fk2 = Column("fk2", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)

        val foreignKey1 = ForeignKey(listOf(fk1), targetTableId, listOf(pkColumn))
        val foreignKey2 = ForeignKey(listOf(fk2), targetTableId, listOf(pkColumn))

        val sourceTable = Table(sourceTableId, setOf(fk1, fk2), setOf(foreignKey1, foreignKey2))

        val uniqueIdSource = 3L
        val fkGene1 = SqlForeignKeyGene("fk1", uniqueIdSource, targetTableId, "col_pk", nullable = false)
        val fkGene2 = SqlForeignKeyGene("fk2", uniqueIdSource, targetTableId, "col_pk", nullable = false)

        val actionSource = SqlAction(sourceTable, setOf(fk1, fk2), uniqueIdSource, listOf(fkGene1, fkGene2))

        RestIndividual(
            resourceCalls = mutableListOf(),
            sampleType = SampleType.RANDOM,
            dbInitialization = mutableListOf(actionTarget1, actionTarget2, actionSource)
        )

        // Bind them to DIFFERENT rows
        fkGene1.uniqueIdOfPrimaryKey = uniqueIdTarget1
        fkGene2.uniqueIdOfPrimaryKey = uniqueIdTarget2

        // Verify initial state is valid
        assertTrue(SqlActionUtils.isValidActions(listOf(actionTarget1, actionTarget2, actionSource)))

        val randomness = Randomness()
        randomness.updateSeed(42)

        // Randomize fkGene1. It should NOT force fkGene2 to change (if fkGene2 was already bound)
        // OR more importantly, if we randomize fkGene2, it should NOT be forced to point to whatever fkGene1 points to.
        
        // Let's randomize fkGene2. Because it's "bound" (to uniqueIdTarget2), and fkGene1 is also bound (to uniqueIdTarget1).
        // In the current BUGGY implementation, fkGene2.randomize() will find fkGene1 in 'otherFksInSameAction', 
        // see it's bound to uniqueIdTarget1, and FORCE fkGene2 to also point to uniqueIdTarget1.
        
        fkGene2.randomize(randomness, tryToForceNewValue = false)
        
        // If the bug exists, fkGene2.uniqueIdOfPrimaryKey will now be equal to fkGene1.uniqueIdOfPrimaryKey (uniqueIdTarget1)
        // But they are independent FKs, so they SHOULD be allowed to point to different PKs.
        // Actually, randomize() might choose a new value if it wasn't forced, but here it's forced by 'alreadyBoundId'
        
        assertNotEquals(fkGene1.uniqueIdOfPrimaryKey, fkGene2.uniqueIdOfPrimaryKey, 
            "Independent FKs in the same action should NOT be forced to point to the same PK")
    }
}

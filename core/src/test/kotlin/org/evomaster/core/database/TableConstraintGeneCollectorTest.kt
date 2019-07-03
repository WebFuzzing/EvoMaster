package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.gene.Gene
import org.evomaster.dbconstraint.*
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class TableConstraintGeneCollectorTest {

    @Test
    fun testLowerBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = LowerBoundConstraint("table0", "column0", -10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testUpperBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = UpperBoundConstraint("table0", "column0", 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testRange() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = RangeConstraint("table0", "column0", -10L, 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)

    }


    @Test
    fun testAndFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = AndConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }


    @Test
    fun testTrueOrFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = OrConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testBothTrueIffFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = IffConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val expectedGenes = action.seeGenes().toSet()

        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testDifferentTableUpperBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = UpperBoundConstraint("table1", "column0", 10L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)

    }


    @Test
    fun testDifferentTableLowerBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = LowerBoundConstraint("table1", "column0", 10L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testDifferentTableRange() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = RangeConstraint("table1", "column0", -10L, +10L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testIsNotNullConstraint() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = IsNotNullConstraint("table0", "column0")

        val expectedGenes = action.seeGenes().toSet()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testDifferentTableIsNotNullConstraint() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = IsNotNullConstraint("table1", "column0")

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }


    @Test
    fun testEnumConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2, enumValuesAsStrings = listOf("value0", "value1", "value2"))
        val constraint = EnumConstraint("table0", "column0", listOf("value0", "value1", "value2"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)

    }

    @Test
    fun testDifferentTableEnumConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2, enumValuesAsStrings = listOf("value0", "value1", "value2"))
        val constraint = EnumConstraint("table1", "column0", listOf("value0", "value1", "value2"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testUniqueConstraintOneRow() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val constraint = UniqueConstraint("table0", listOf("column0"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testUniqueConstrainDifferentTable() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val constraint = UniqueConstraint("table1", listOf("column0"))

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    // (status = 'B') = (p_at IS NOT NULL)
    @Test
    fun testIffEnumAndIsNotConstraint() {
        val statusColumn = Column("status", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val pAtColumn = Column("p_at", ColumnDataType.TIMESTAMP, databaseType = DatabaseType.H2)

        val equalsConstraint = EnumConstraint("table0", "status", listOf("B"))
        val isNotNullConstraint = IsNotNullConstraint("table0", "p_at")
        val constraint = IffConstraint("table0", equalsConstraint, isNotNullConstraint)

        val table = Table("table0", setOf(statusColumn, pAtColumn), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(statusColumn, pAtColumn), id = 0L)

        val expectedGenes = action.seeGenes().toSet()
        val collector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(collector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testNotSupportedConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = UnsupportedTableConstraint("table0", "this query was not parsed")

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testLikeConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = LikeConstraint("table0", "column0", "%hi_", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }


    @Test
    fun testLikeConstraintDifferentTable() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = LikeConstraint("table1", "column0", "%hi_", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }

    @Test
    fun testSimilarToConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = SimilarToConstraint("table0", "column0", "/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val expectedGenes = action.seeGenes().toSet()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)

    }

    @Test
    fun testSimilarToConstraintDiffTable() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = SimilarToConstraint("table1", "column0", "/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val expectedGenes = setOf<Gene>()
        val geneCollector = TableConstraintGeneCollector()
        val collectedGenes = constraint.accept(geneCollector, action)
        assertEquals(expectedGenes, collectedGenes)
    }
}
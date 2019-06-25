package org.evomaster.core.database

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.SqlTimestampGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.dbconstraint.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test

class TableConstraintEvaluatorTest {

    @Test
    fun testTrueLowerBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = LowerBoundConstraint("table0", "column0", -10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseLowerBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = LowerBoundConstraint("table0", "column0", 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testTrueUpperBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = UpperBoundConstraint("table0", "column0", 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseUpperBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = UpperBoundConstraint("table0", "column0", -10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testTrueRange() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = RangeConstraint("table0", "column0", -10L, 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseRange() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val constraint = RangeConstraint("table0", "column0", -10L, 10L)
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 100L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 1000))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testTrueAndFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = AndConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseAndFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = AndConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = -15))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testTrueOrFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = OrConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = -15))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseOrFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", 10L)
        val upperBound = UpperBoundConstraint("table0", "column0", -10L)

        val constraint = OrConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testBothTrueIffFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", -10L)
        val upperBound = UpperBoundConstraint("table0", "column0", 10L)

        val constraint = IffConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testBothFalseIffFormula() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val lowerBound = LowerBoundConstraint("table0", "column0", 10L)
        val upperBound = UpperBoundConstraint("table0", "column0", -10L)

        val constraint = IffConstraint("table0", lowerBound, upperBound)
        val table = Table("table0", setOf(column), setOf(), setOf(lowerBound, upperBound))

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))


        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testDifferentTableUpperBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()

        val constraint = UpperBoundConstraint("table1", "column0", 10L)
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }


    @Test
    fun testDifferentTableLowerBound() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()

        val constraint = LowerBoundConstraint("table1", "column0", 10L)
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testDifferentTableRange() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()

        val constraint = RangeConstraint("table1", "column0", -10L, +10L)
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testTrueIsNotNullConstraint() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()

        val constraint = IsNotNullConstraint("table0", "column0")
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testDifferentTableIsNotNullConstraint() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as IntegerGene).copyValueFrom(IntegerGene("column0", value = 0))
        val evaluator = TableConstraintEvaluator()

        val constraint = IsNotNullConstraint("table1", "column0")
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testFalseIsNotNullConstraint() {
        val column = Column("column0", ColumnDataType.INTEGER, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action = DbAction(table = table, selectedColumns = setOf(), id = 0L)

        val constraint = IsNotNullConstraint("table0", "column0")
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }


    @Test
    fun testTrueEnumConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2, enumValuesAsStrings = listOf("value0", "value1", "value2"))
        val constraint = EnumConstraint("table0", "column0", listOf("value0", "value1", "value2"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testDifferentTableEnumConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2, enumValuesAsStrings = listOf("value0", "value1", "value2"))
        val constraint = EnumConstraint("table1", "column0", listOf("value0", "value1", "value2"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testEnumConstraintNullValue() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2, enumValuesAsStrings = listOf("value0", "value1", "value2"))
        val constraint = EnumConstraint("table0", "column0", listOf("value0", "value1", "value2"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(), id = 0L)
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertFalse(value)
    }

    @Test
    fun testUniqueConstraintOneRow() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val constraint = UniqueConstraint("table0", listOf("column0"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("foo"))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action)
        assertTrue(value)
    }

    @Test
    fun testUniqueConstraintMultiRowFalse() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val constraint = UniqueConstraint("table0", listOf("column0"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action0 = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action0.seeGenes()[0] as StringGene).copyValueFrom(StringGene("foo"))

        val action1 = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action1.seeGenes()[0] as StringGene).copyValueFrom(StringGene("foo"))


        val evaluator = TableConstraintEvaluator(listOf(action0))
        val value = constraint.accept(evaluator, action1)
        assertFalse(value)
    }

    @Test
    fun testUniqueConstraintMultiRowTrue() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val constraint = UniqueConstraint("table0", listOf("column0"))
        val table = Table("table0", setOf(column), setOf(), setOf(constraint))
        val action0 = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action0.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "foo"))

        val action1 = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action1.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "bar"))

        val evaluator = TableConstraintEvaluator(listOf(action0))
        val value = constraint.accept(evaluator, action1)
        assertTrue(value)
    }

    @Test
    fun testUniqueConstrainDifferentTable() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action0 = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action0.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "foo"))

        val constraint = UniqueConstraint("table1", listOf("column0"))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action0)
        assertTrue(value)
    }

    @Test
    fun testUniqueConstrainNullValues() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val action0 = DbAction(table = table, selectedColumns = setOf(), id = 0L)

        val constraint = UniqueConstraint("table0", listOf("column0"))
        val evaluator = TableConstraintEvaluator()
        val value = constraint.accept(evaluator, action0)
        assertTrue(value)
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
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("status", value = "B"))
        (action.seeGenes()[1] as SqlTimestampGene).copyValueFrom(SqlTimestampGene("p_at"))

        val evaluator = TableConstraintEvaluator()

        val equalsConstraintValue = equalsConstraint.accept(evaluator, action)
        assertTrue(equalsConstraintValue)

        val isNotNullConstraintValue = isNotNullConstraint.accept(evaluator, action)
        assertTrue(isNotNullConstraintValue)

        val constraintValue = constraint.accept(evaluator, action)
        assertTrue(constraintValue)
    }

    @Test
    fun testNotSupportedConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.H2)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = UnsupportedTableConstraint("table0", "this query was not parsed")

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertTrue(constraintValue)
    }

    @Test
    fun testLikeConstraint() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = LikeConstraint("table0", "column0", "%hi_", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("status", value = "hiX"))

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertTrue(constraintValue)
    }

    @Test
    fun testLikeConstraintFalse() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = LikeConstraint("table0", "column0", "%hi_", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("status", value = "not matches"))

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertFalse(constraintValue)
    }

    @Test
    fun testSimilarToConstraintTrue() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = SimilarToConstraint("table0", "column0", "/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "/foo/XX/bar/left/0000-00-000"))

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertTrue(constraintValue)
    }

    @Test
    fun testSimilarToConstraintFalse() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = SimilarToConstraint("table0", "column0", "/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "/foo/XXXX/bar/left/0000-00-000"))

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertFalse(constraintValue)
    }

    @Test
    fun testSimilarToConstraintDiffTable() {
        val column = Column("column0", ColumnDataType.TEXT, databaseType = DatabaseType.POSTGRES)
        val table = Table("table0", setOf(column), setOf(), setOf())
        val constraint = SimilarToConstraint("table1", "column0", "/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?", ConstraintDatabaseType.POSTGRES)

        val action = DbAction(table = table, selectedColumns = setOf(column), id = 0L)
        (action.seeGenes()[0] as StringGene).copyValueFrom(StringGene("column0", value = "/foo/XXXX/bar/left/0000-00-000"))

        val evaluator = TableConstraintEvaluator()
        val constraintValue = constraint.accept(evaluator, action)
        assertTrue(constraintValue)
    }
}
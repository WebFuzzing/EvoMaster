package org.evomaster.core.output

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType.*
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestCaseWriterTest {

    @Test
    fun testEmptyDbInitialization() {

        val dbInitialization = ArrayList<DbAction>()

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testOneAction() {
        val aColumn = Column("aColumn", VARCHAR, 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val id = 0L

        val gene = StringGene(aColumn.name, "stringValue", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene))

        val dbInitialization = ArrayList<DbAction>()
        dbInitialization.add(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"aColumn\", \"\\\"stringValue\\\"\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    private fun buildEvaluatedIndividual(dbInitialization: MutableList<DbAction>): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {
        val format = OutputFormat.JAVA_JUNIT_4

        val baseUrlOfSut = "baseUrlOfSut"

        val sampleType = SampleType.RANDOM

        val restActions = ArrayList<RestAction>()


        val individual = RestIndividual(restActions, sampleType, dbInitialization)

        val fitnessVal = FitnessValue(0.0)

        val results = ArrayList<ActionResult>()

        val ei = EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
        return Triple(format, baseUrlOfSut, ei)
    }

    @Test
    fun testTwoAction() {
        val aColumn = Column("aColumn", VARCHAR, 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val gene0 = StringGene(aColumn.name, "stringValue0", 0, 10)

        val insertIntoTableAction0 = DbAction(aTable, setOf(aColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(aColumn.name, "stringValue1", 0, 10)

        val insertIntoTableAction1 = DbAction(aTable, setOf(aColumn), 1L, mutableListOf(gene1))


        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction0, insertIntoTableAction1))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"aColumn\", \"\\\"stringValue0\\\"\")")
            indent()
            add(".and().insertInto(\"myTable\", 1L).d(\"aColumn\", \"\\\"stringValue1\\\"\").dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testTwoColumns() {
        val column0 = Column("Column0", VARCHAR, 10)
        val column1 = Column("Column1", VARCHAR, 10)

        val aTable = Table("myTable", setOf(column0, column1), HashSet<ForeignKey>())

        val id = 0L

        val gene0 = StringGene(column0.name, "stringValue0", 0, 10)
        val gene1 = StringGene(column1.name, "stringValue1", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(column0, column1), id, mutableListOf(gene0, gene1))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"Column0\", \"\\\"stringValue0\\\"\").d(\"Column1\", \"\\\"stringValue1\\\"\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testIntegerColumn() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = false)
        val nameColumn = Column("Name", VARCHAR, 10, primaryKey = false)

        val aTable = Table("myTable", setOf(idColumn, nameColumn), HashSet<ForeignKey>())

        val id = 0L

        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val stringGene = StringGene(nameColumn.name, "nameValue", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(idColumn, nameColumn), id, listOf(integerGene, stringGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"Id\", \"42\").d(\"Name\", \"\\\"nameValue\\\"\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testPrimaryKeyColumn() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = true)
        val nameColumn = Column("Name", VARCHAR, 10, primaryKey = false)

        val aTable = Table("myTable", setOf(idColumn, nameColumn), HashSet<ForeignKey>())


        val id = 0L

        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val primaryKeyGene = SqlPrimaryKeyGene(idColumn.name, "myTable", integerGene, 10)
        val stringGene = StringGene(nameColumn.name, "nameValue", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(idColumn, nameColumn), id, listOf(primaryKeyGene, stringGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"Id\", \"42\").d(\"Name\", \"\\\"nameValue\\\"\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testForeignKeyColumn() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = true)
        val table0 = Table("Table0", setOf(idColumn), HashSet<ForeignKey>())

        val fkColumn = Column("fkId", INTEGER, 10, primaryKey = false)
        val table1 = Table("Table1", setOf(idColumn, fkColumn), HashSet<ForeignKey>())


        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val primaryKeyTable0Gene = SqlPrimaryKeyGene(idColumn.name, "Table0", integerGene, 10)
        val primaryKeyTable1Gene = SqlPrimaryKeyGene(idColumn.name, "Table1", integerGene, 10)


        val firstInsertionId = 1001L
        val insertIntoTable0 = DbAction(table0, setOf(idColumn), firstInsertionId, listOf(primaryKeyTable0Gene))
        val secondInsertionId = 1002L
        val foreignKeyGene = SqlForeignKeyGene(fkColumn.name, secondInsertionId, "Table0", false, firstInsertionId)

        val insertIntoTable1 = DbAction(table1, setOf(idColumn, fkColumn), secondInsertionId, listOf(primaryKeyTable1Gene, foreignKeyGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTable0, insertIntoTable1))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L).d(\"Id\", \"42\")")
            indent()
            add(".and().insertInto(\"Table1\", 1002L).d(\"Id\", \"42\").r(\"fkId\", 1001L).dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testBooleanColumn() {
        val aColumn = Column("aColumn", BOOLEAN, 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val id = 0L

        val gene = BooleanGene(aColumn.name, false)

        val insertIntoTableAction = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene))

        val dbInitialization = ArrayList<DbAction>()
        dbInitialization.add(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"aColumn\", \"false\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testNullableForeignKeyColumn() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = true)
        val table0 = Table("Table0", setOf(idColumn), HashSet<ForeignKey>())

        val fkColumn = Column("fkId", INTEGER, 10, primaryKey = false, nullable = true)
        val table1 = Table("Table1", setOf(idColumn, fkColumn), HashSet<ForeignKey>())


        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val primaryKeyTable0Gene = SqlPrimaryKeyGene(idColumn.name, "Table0", integerGene, 10)
        val primaryKeyTable1Gene = SqlPrimaryKeyGene(idColumn.name, "Table1", integerGene, 10)


        val firstInsertionId = 1001L
        val insertIntoTable0 = DbAction(table0, setOf(idColumn), firstInsertionId, listOf(primaryKeyTable0Gene))
        val secondInsertionId = 1002L
        val foreignKeyGene = SqlForeignKeyGene(fkColumn.name, secondInsertionId, "Table0", true, -1L)

        val insertIntoTable1 = DbAction(table1, setOf(idColumn, fkColumn), secondInsertionId, listOf(primaryKeyTable1Gene, foreignKeyGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTable0, insertIntoTable1))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L).d(\"Id\", \"42\")")
            indent()
            add(".and().insertInto(\"Table1\", 1002L).d(\"Id\", \"42\").d(\"fkId\", \"NULL\").dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testTimeStampColumn() {
        val aColumn = Column("aColumn", TIMESTAMP, 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val id = 0L

        val gene = DateTimeGene(aColumn.name)

        val insertIntoTableAction = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene))

        val dbInitialization = ArrayList<DbAction>()
        dbInitialization.add(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L).d(\"aColumn\", \"\\\"2016-03-12 00:00:00\\\"\").dtos();")
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

}
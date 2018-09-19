package org.evomaster.core.output

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.StringGene
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

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testOneAction() {
        val aColumn = Column("aColumn", "VARCHAR", 10)

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
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\").d(\"aColumn\", \"stringValue\").dtos();")
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
        val aColumn = Column("aColumn", "VARCHAR", 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val id = 0L

        val gene0 = StringGene(aColumn.name, "stringValue0", 0, 10)

        val insertIntoTableAction0 = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene0))

        val gene1 = StringGene(aColumn.name, "stringValue1", 0, 10)

        val insertIntoTableAction1 = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene1))


        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction0, insertIntoTableAction1))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.indent()
        expectedLines.add("List<InsertionDto> insertions = sql().insertInto(\"myTable\").d(\"aColumn\", \"stringValue0\").and().insertInto(\"myTable\").d(\"aColumn\", \"stringValue1\").dtos();")
        expectedLines.add("controller.execInsertionsIntoDatabase(insertions);")
        expectedLines.deindent()
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testTwoColumns() {
        val column0 = Column("Column0", "VARCHAR", 10)
        val column1 = Column("Column1", "VARCHAR", 10)

        val aTable = Table("myTable", setOf(column0, column1), HashSet<ForeignKey>())

        val id = 0L

        val gene0 = StringGene(column0.name, "stringValue0", 0, 10)
        val gene1 = StringGene(column1.name, "stringValue1", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(column0, column1), id, mutableListOf(gene0, gene1))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.indent()
        expectedLines.add("List<InsertionDto> insertions = sql().insertInto(\"myTable\").d(\"Column0\", \"stringValue0\").d(\"Column1\", \"stringValue1\").dtos();")
        expectedLines.add("controller.execInsertionsIntoDatabase(insertions);")
        expectedLines.deindent()
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testIntegerColumn() {
        val idColumn = Column("Id", "INTEGER", 10, primaryKey = false)
        val nameColumn = Column("Name", "VARCHAR", 10, primaryKey = false)

        val aTable = Table("myTable", setOf(idColumn, nameColumn), HashSet<ForeignKey>())

        val id = 0L

        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val stringGene = StringGene(nameColumn.name, "nameValue", 0, 10)

        val insertIntoTableAction = DbAction(aTable, setOf(idColumn, nameColumn), id, listOf(integerGene, stringGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.indent()
        expectedLines.add("List<InsertionDto> insertions = sql().insertInto(\"myTable\").d(\"Id\", 42).d(\"Name\", \"nameValue\").dtos();")
        expectedLines.add("controller.execInsertionsIntoDatabase(insertions);")
        expectedLines.deindent()
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testPrimaryKeyColumn() {
        val idColumn = Column("Id", "INTEGER", 10, primaryKey = true)
        val nameColumn = Column("Name", "VARCHAR", 10, primaryKey = false)

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

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.indent()
        expectedLines.add("List<InsertionDto> insertions = sql().insertInto(\"myTable\").d(\"Id\", 42).d(\"Name\", \"nameValue\").dtos();")
        expectedLines.add("controller.execInsertionsIntoDatabase(insertions);")
        expectedLines.deindent()
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testForeignKeyColumn() {
        val idColumn = Column("Id", "INTEGER", 10, primaryKey = true)
        val table0 = Table("Table0", setOf(idColumn), HashSet<ForeignKey>())

        val fkColumn = Column("fkId", "INTEGER", 10, primaryKey = false)
        val table1 = Table("Table1", setOf(idColumn, fkColumn), HashSet<ForeignKey>())


        val insertionId = 1001L

        val integerGene = IntegerGene(idColumn.name, 42, 0, 10)
        val primaryKeyTable0Gene = SqlPrimaryKeyGene(idColumn.name, "Table0", integerGene, 10)
        val primaryKeyTable1Gene = SqlPrimaryKeyGene(idColumn.name, "Table1", integerGene, 10)
        val foreignKeyGene = SqlForeignKeyGene(fkColumn.name, insertionId, "Table0", nullable = false)


        val insertIntoTable0 = DbAction(table0, setOf(idColumn), insertionId, listOf(primaryKeyTable0Gene))
        val insertIntoTable1 = DbAction(table1, setOf(idColumn, fkColumn), insertionId, listOf(primaryKeyTable1Gene, foreignKeyGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTable0, insertIntoTable1))

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(format, test, baseUrlOfSut)

        val expectedLines = Lines()
        expectedLines.add("@Test")
        expectedLines.add("public void test() throws Exception {")
        expectedLines.indent()
        expectedLines.add("List<InsertionDto> insertions = sql().insertInto(\"Table0\").d(\"Id\", 42).and().insertInto(\"Table1\").d(\"Id\", 42).r(\"fkId\", 1001).dtos();")
        expectedLines.add("controller.execInsertionsIntoDatabase(insertions);")
        expectedLines.deindent()
        expectedLines.add("}")

        assertEquals(expectedLines.toString(), lines.toString())
    }



}
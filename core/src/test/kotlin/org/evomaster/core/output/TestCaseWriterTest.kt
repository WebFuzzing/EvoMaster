package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType.*
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.buildEvaluatedIndividual
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestCaseWriterTest {

    @Test
    fun testEmptyDbInitialization() {


        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(emptyList<DbAction>().toMutableList())

        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

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

        val dbInitialization = mutableListOf(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue0\\\"\")")
            deindent()
            add(".and().insertInto(\"myTable\", 1L)")
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue1\\\"\")")
            deindent()
            add(".dtos();")
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"Column0\", \"\\\"stringValue0\\\"\")")
            add(".d(\"Column1\", \"\\\"stringValue1\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"Id\", \"42\")")
            add(".d(\"Name\", \"\\\"nameValue\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"Id\", \"42\")")
            add(".d(\"Name\", \"\\\"nameValue\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L)")
            indent()
            indent()
            add(".d(\"Id\", \"42\")")
            deindent()
            add(".and().insertInto(\"Table1\", 1002L)")
            indent()
            add(".d(\"Id\", \"42\")")
            add(".r(\"fkId\", 1001L)")
            deindent()
            add(".dtos();")
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

        val dbInitialization = mutableListOf(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"aColumn\", \"false\")")
            deindent()
            add(".dtos();")
            deindent()
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
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L)")
            indent()
            indent()
            add(".d(\"Id\", \"42\")")
            deindent()
            deindent()
            indent()
            add(".and().insertInto(\"Table1\", 1002L)")
            indent()
            add(".d(\"Id\", \"42\")")
            add(".d(\"fkId\", \"NULL\")")
            deindent()
            add(".dtos();")
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

        val gene = SqlTimestampGene(aColumn.name)

        val insertIntoTableAction = DbAction(aTable, setOf(aColumn), id, mutableListOf(gene))

        val dbInitialization = mutableListOf(insertIntoTableAction)

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(dbInitialization)
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"aColumn\", \"\\\"2016-03-12 00:00:00\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testThreeAction() {
        val aColumn = Column("aColumn", VARCHAR, 10)

        val aTable = Table("myTable", setOf(aColumn), HashSet<ForeignKey>())


        val gene0 = StringGene(aColumn.name, "stringValue0", 0, 10)

        val insertIntoTableAction0 = DbAction(aTable, setOf(aColumn), 0L, mutableListOf(gene0))

        val gene1 = StringGene(aColumn.name, "stringValue1", 0, 10)

        val insertIntoTableAction1 = DbAction(aTable, setOf(aColumn), 1L, mutableListOf(gene1))

        val gene2 = StringGene(aColumn.name, "stringValue2", 0, 10)

        val insertIntoTableAction2 = DbAction(aTable, setOf(aColumn), 2L, mutableListOf(gene2))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTableAction0, insertIntoTableAction1, insertIntoTableAction2))
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"myTable\", 0L)")
            indent()
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue0\\\"\")")
            deindent()
            add(".and().insertInto(\"myTable\", 1L)")
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue1\\\"\")")
            deindent()
            add(".and().insertInto(\"myTable\", 2L)")
            indent()
            add(".d(\"aColumn\", \"\\\"stringValue2\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testTimeStampForeignKeyColumn() {
        val idColumn = Column("Id", TIMESTAMP, 10, primaryKey = true)
        val table0 = Table("Table0", setOf(idColumn), HashSet<ForeignKey>())

        val fkColumn = Column("fkId", TIMESTAMP, 10, primaryKey = false)
        val table1 = Table("Table1", setOf(idColumn, fkColumn), HashSet<ForeignKey>())


        val timeStampGene = SqlTimestampGene(idColumn.name)
        val primaryKeyTable0Gene = SqlPrimaryKeyGene(idColumn.name, "Table0", timeStampGene, 10)
        val primaryKeyTable1Gene = SqlPrimaryKeyGene(idColumn.name, "Table1", timeStampGene, 10)


        val firstInsertionId = 1001L
        val insertIntoTable0 = DbAction(table0, setOf(idColumn), firstInsertionId, listOf(primaryKeyTable0Gene))
        val secondInsertionId = 1002L
        val foreignKeyGene = SqlForeignKeyGene(fkColumn.name, secondInsertionId, "Table0", false, firstInsertionId)

        val insertIntoTable1 = DbAction(table1, setOf(idColumn, fkColumn), secondInsertionId, listOf(primaryKeyTable1Gene, foreignKeyGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insertIntoTable0, insertIntoTable1))
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L)")
            indent()
            indent()
            add(".d(\"Id\", \"\\\"2016-03-12 00:00:00\\\"\")")
            deindent()
            add(".and().insertInto(\"Table1\", 1002L)")
            indent()
            add(".d(\"Id\", \"\\\"2016-03-12 00:00:00\\\"\")")
            add(".r(\"fkId\", 1001L)")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testIndirectForeignKeyColumn() {
        val table0_Id = Column("Id", INTEGER, 10, primaryKey = true, autoIncrement = true)
        val table0 = Table("Table0", setOf(table0_Id), HashSet<ForeignKey>())

        val table1_Id = Column("Id", INTEGER, 10, primaryKey = true)
        val table1 = Table("Table1", setOf(table1_Id), HashSet<ForeignKey>())

        val table2_Id = Column("Id", INTEGER, 10, primaryKey = true, foreignKeyToAutoIncrement = true)
        val table2 = Table("Table2", setOf(table2_Id), HashSet<ForeignKey>())


        val integerGene = IntegerGene(table0_Id.name, 42, 0, 10)

        val primaryKeyTable1Gene = SqlPrimaryKeyGene(table0_Id.name, "Table1", integerGene, 10)


        val insertId0 = 1001L
        val autoGene = SqlAutoIncrementGene(table0_Id.name)
        val pkGene0 = SqlPrimaryKeyGene(table0_Id.name, "Table0", autoGene, 10)
        val insert0 = DbAction(table0, setOf(table0_Id), insertId0, listOf(pkGene0))

        val insertId1 = 1002L
        val fkGene0 = SqlForeignKeyGene(table1_Id.name, insertId1, "Table0", false, insertId0)
        val insert1 = DbAction(table1, setOf(table1_Id), insertId1, listOf(fkGene0))

        val insertId2 = 1003L
        val fkGene1 = SqlForeignKeyGene(table2_Id.name, insertId2, "Table1", false, insertId1)
        val insert2 = DbAction(table2, setOf(table2_Id), insertId2, listOf(fkGene1))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert0, insert1, insert2))
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 1001L)")
            indent()
            add(".and().insertInto(\"Table1\", 1002L)")
            indent()
            add(".r(\"Id\", 1001L)")
            deindent()
            add(".and().insertInto(\"Table2\", 1003L)")
            indent()
            add(".r(\"Id\", 1002L, true)")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testInsertDateColumnType() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = true, autoIncrement = true)
        val dateColumn = Column("birthDate", DATE, 10, primaryKey = false, autoIncrement = false)

        val table = Table("Table0", setOf(idColumn, dateColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val dateGene = DateGene(dateColumn.name)
        val insert = DbAction(table, setOf(idColumn, dateColumn), 0L, listOf(pkGene0, dateGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"birthDate\", \"\\\"2016-03-12\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testUUIDColumnType() {
        val idColumn = Column("Id", INTEGER, 10, primaryKey = true, autoIncrement = true)
        val uuidColumn = Column("uuidCode", UUID, 10, primaryKey = false, autoIncrement = false)

        val table = Table("Table0", setOf(idColumn, uuidColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val uuidGene = SqlUUIDGene(uuidColumn.name)
        val insert = DbAction(table, setOf(idColumn, uuidColumn), 0L, listOf(pkGene0, uuidGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = EMConfig()
        config.outputFormat = format

        val test = TestCase(test = ei, name = "test")

        val writer = TestCaseWriter()

        val lines = writer.convertToCompilableTestCode(config, test, baseUrlOfSut)

        val expectedLines = Lines().apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"uuidCode\", \"\\\"{00000000-0000-0000-0000-000000000000}\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        assertEquals(expectedLines.toString(), lines.toString())
    }



}
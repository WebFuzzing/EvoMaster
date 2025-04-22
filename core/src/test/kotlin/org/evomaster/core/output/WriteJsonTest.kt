package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.EMConfig
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.buildEvaluatedIndividual
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WriteJsonTest : WriterTestBase() {


    @Test
    fun testJSONBEmpty() {


        val idColumn = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, autoIncrement = true,
                databaseType = DatabaseType.POSTGRES)
        val jsonbColumn = Column("jsonbColumn", ColumnDataType.JSONB, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(idColumn, jsonbColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val objectGene = ObjectGene(jsonbColumn.name, listOf())
        val insert = SqlAction(table, setOf(idColumn, jsonbColumn), 0L, listOf(pkGene0, objectGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode(test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"jsonbColumn\", \"'{}'\")")
            deindent()
            add(".dtos();")
            deindent()
            add("InsertionResultsDto insertionsresult = controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testJSONBColumnType() {


        val idColumn = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, autoIncrement = true,
                databaseType = DatabaseType.POSTGRES)
        val jsonbColumn = Column("jsonbColumn", ColumnDataType.JSONB, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(idColumn, jsonbColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val objectGene = ObjectGene(jsonbColumn.name, listOf(IntegerGene("integerField")))
        val insert = SqlAction(table, setOf(idColumn, jsonbColumn), 0L, listOf(pkGene0, objectGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode(test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"jsonbColumn\", \"'{\\\"integerField\\\":0}'\")")
            deindent()
            add(".dtos();")
            deindent()
            add("InsertionResultsDto insertionsresult = controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testJSONBoolean() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, autoIncrement = true,
                databaseType = DatabaseType.POSTGRES)
        val jsonbColumn = Column("jsonbColumn", ColumnDataType.JSONB, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(idColumn, jsonbColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val objectGene = ObjectGene(jsonbColumn.name, listOf(BooleanGene("booleanField")))
        val insert = SqlAction(table, setOf(idColumn, jsonbColumn), 0L, listOf(pkGene0, objectGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"jsonbColumn\", \"'{\\\"booleanField\\\":true}'\")")
            deindent()
            add(".dtos();")
            deindent()
            add("InsertionResultsDto insertionsresult = controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testJSONString() {
        val idColumn = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, autoIncrement = true,
                databaseType = DatabaseType.POSTGRES)
        val jsonbColumn = Column("jsonbColumn", ColumnDataType.JSONB, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(idColumn, jsonbColumn), HashSet<ForeignKey>())


        val autoGene = SqlAutoIncrementGene(table.name)
        val pkGene0 = SqlPrimaryKeyGene(idColumn.name, "Table0", autoGene, 10)
        val objectGene = ObjectGene(jsonbColumn.name, listOf(StringGene("stringField")))
        val insert = SqlAction(table, setOf(idColumn, jsonbColumn), 0L, listOf(pkGene0, objectGene))

        val (format, baseUrlOfSut, ei) = buildEvaluatedIndividual(mutableListOf(insert))
        val config = getConfig(format)

        val test = TestCase(test = ei, name = "test")

        val writer = RestTestCaseWriter(config, PartialOracles())

        val lines = writer.convertToCompilableTestCode( test, baseUrlOfSut)

        val expectedLines = Lines(format).apply {
            add("@Test")
            add("public void test() throws Exception {")
            indent()
            add("List<InsertionDto> insertions = sql().insertInto(\"Table0\", 0L)")
            indent()
            indent()
            add(".d(\"jsonbColumn\", \"'{\\\"stringField\\\":\\\"foo\\\"}'\")")
            deindent()
            add(".dtos();")
            deindent()
            add("InsertionResultsDto insertionsresult = controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }
}

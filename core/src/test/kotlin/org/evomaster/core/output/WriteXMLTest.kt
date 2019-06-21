package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.buildEvaluatedIndividual
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.SqlXMLGene
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WriteXMLTest {

    @Test
    fun testEmptyXML() {
        val xmlColumn = Column("xmlColumn", ColumnDataType.XML, 10, primaryKey = false, autoIncrement = false, databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(xmlColumn), setOf())


        val objectGene = ObjectGene("anElement", listOf())
        val sqlXMLGene = SqlXMLGene("xmlColumn", objectGene)

        val insert = DbAction(table, setOf(xmlColumn), 0L, listOf(sqlXMLGene))

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
            add(".d(\"xmlColumn\", \"\\\"<anElement></anElement>\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }

    @Test
    fun testChildrenXML() {
        val xmlColumn = Column("xmlColumn", ColumnDataType.XML, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(xmlColumn), setOf())


        val child0 = ObjectGene("child", listOf())
        val child1 = ObjectGene("child", listOf())
        val child2 = ObjectGene("child", listOf())
        val child3 = ObjectGene("child", listOf())
        val objectGene = ObjectGene("anElement", listOf(child0, child1, child2, child3))
        val sqlXMLGene = SqlXMLGene("xmlColumn", objectGene)

        val insert = DbAction(table, setOf(xmlColumn), 0L, listOf(sqlXMLGene))

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
            add(".d(\"xmlColumn\", \"\\\"<anElement><child></child><child></child><child></child><child></child></anElement>\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }


    @Test
    fun testString() {
        val xmlColumn = Column("xmlColumn", ColumnDataType.XML, 10, primaryKey = false, autoIncrement = false,
                databaseType = DatabaseType.POSTGRES)

        val table = Table("Table0", setOf(xmlColumn), setOf())


        val stringGene = StringGene("stringValue", value = "</element>")
        val objectGene = ObjectGene("anElement", listOf(stringGene))
        val sqlXMLGene = SqlXMLGene("xmlColumn", objectGene)

        val insert = DbAction(table, setOf(xmlColumn), 0L, listOf(sqlXMLGene))

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
            add(".d(\"xmlColumn\", \"\\\"<anElement>&lt;/element&gt;</anElement>\\\"\")")
            deindent()
            add(".dtos();")
            deindent()
            add("controller.execInsertionsIntoDatabase(insertions);")
            deindent()
            add("}")
        }

        Assertions.assertEquals(expectedLines.toString(), lines.toString())
    }


}

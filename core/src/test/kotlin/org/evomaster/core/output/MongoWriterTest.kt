package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.database.mongo.MongoDbActionResult
import org.evomaster.core.search.action.EvaluatedMongoDbAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MongoWriterTest {

    /**
     * Creates an `EvaluatedMongoDbAction` object based on the specified parameters.
     *
     * @param database The name of the database to be used in the action. Defaults to "testdb".
     * @param collection The name of the collection to be used in the action. Defaults to "users".
     * @param genes A list of `Gene` objects representing the structure and data for the action. Defaults
     *              to a gene structure that includes an object gene with a "user" containing a string gene
     *              for the name "Alice".
     * @param success A flag indicating the success status to be set in the `MongoDbActionResult`. Defaults
     *                to true.
     * @return An instance of `EvaluatedMongoDbAction` containing the constructed `MongoDbAction` and its
     *         corresponding result.
     */
    private fun makeEvaluatedMongoAction(
        database: String = "testdb",
        collection: String = "users",
        genes: List<Gene> = listOf(ObjectGene("user", listOf(StringGene("name", "Alice")))),
        success: Boolean = true
    ): EvaluatedMongoDbAction {
        val action = MongoDbAction(database, collection, "ignored", genes)
        action.setLocalId("test-mongo-action")
        val result = MongoDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedMongoDbAction(action, result)
    }

    /**
     * Generates Kotlin test code for the specified MongoDB actions.
     *
     * @param actions A list of `EvaluatedMongoDbAction` objects representing
     *                the evaluated MongoDB actions for which the code is to be generated.
     * @param insertionVars A mutable list of variable name and value pairs that can be
     *                      used for insertions in the generated code. Defaults to an empty list.
     * @param skipFailure A flag indicating whether actions that failed should be skipped
     *                    in the generated output. Defaults to false.
     * @param groupIndex A string representing the group index to differentiate or categorize
     *                   the generated actions. Defaults to an empty string.
     * @return A `String` containing the generated Kotlin test code.
     */
    private fun writeKotlin(
        actions: List<EvaluatedMongoDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false,
        groupIndex: String = ""
    ): String {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)
        MongoWriter.handleMongoDbInitialization(OutputFormat.KOTLIN_JUNIT_5, actions, lines, groupIndex, insertionVars, skipFailure)
        return lines.toString()
    }

    /**
     * Generates Java test code for the specified MongoDB actions.
     *
     * @param actions A list of `EvaluatedMongoDbAction` objects representing
     *                the evaluated MongoDB actions for which the code is to be generated.
     * @param insertionVars A mutable list of variable name and value pairs that can be
     *                      used for insertions in the generated code. Defaults to an empty list.
     * @param skipFailure A flag indicating whether actions that failed should be skipped
     *                    in the generated output. Defaults to false.
     * @param groupIndex A string representing the group index to differentiate or categorize
     *                   the generated actions. Defaults to an empty string.
     * @return A `String` containing the generated Java test code.
     */
    private fun writeJava(
        actions: List<EvaluatedMongoDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false,
        groupIndex: String = ""
    ): String {
        val lines = Lines(OutputFormat.JAVA_JUNIT_5)
        MongoWriter.handleMongoDbInitialization(OutputFormat.JAVA_JUNIT_5, actions, lines, groupIndex, insertionVars, skipFailure)
        return lines.toString()
    }

    @Test
    fun `should escape literal backslash u without changing its runtime value`() {
        val cases = mapOf(
            """{"field":"El24e\uJTQGh"}""" to """{\"field\":\"El24e\134uJTQGh\"}""",
            """{"field":"El24e\\uJTQGh"}""" to """{\"field\":\"El24e\134\134uJTQGh\"}""",
            """{"field":"\u0041"}""" to """{\"field\":\"\134u0041\"}"""
        )

        cases.forEach { (ejson, expected) ->
            val escaped = MongoWriter.escapeEjsonForJavaLiteral(ejson)

            assertEquals(expected, escaped)
            assertEquals(ejson, StringEscapeUtils.unescapeJava(escaped))
        }
    }

    @Test
    fun `should not rewrite unicode escapes created by escapeJava`() {
        listOf("\\é", "\\😀", "é", "😀").forEach { ejson ->
            val escaped = MongoWriter.escapeEjsonForJavaLiteral(ejson)

            assertEquals(StringEscapeUtils.escapeJava(ejson), escaped)
            assertEquals(ejson, StringEscapeUtils.unescapeJava(escaped))
        }
    }

    @Test
    fun testEmptyListGeneratesNothing() {
        assertTrue(writeKotlin(emptyList()).isBlank())
        assertTrue(writeJava(emptyList()).isBlank())
    }

    @Test
    fun testAllFailedWithSkipFailureGeneratesNothing() {
        val output = writeKotlin(listOf(makeEvaluatedMongoAction(success = false)), skipFailure = true)
        assertTrue(output.isBlank())
    }

    @Test
    fun testKotlinFormatSingleAction() {
        val output = writeKotlin(listOf(makeEvaluatedMongoAction("appdb", "customers", listOf(ObjectGene("customer", listOf(StringGene("name", "Bob")))))))
        assertTrue(output.contains("val mongoInsertions = mongo().insertInto(\"appdb\", \"customers\")"))
        assertTrue(output.contains(".d("))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val mongoInsertionsresult = controller.execInsertionsIntoMongoDatabase(mongoInsertions)"))
    }

    @Test
    fun testJavaFormatSingleAction() {
        val output = writeJava(listOf(makeEvaluatedMongoAction("appdb", "customers", listOf(ObjectGene("customer", listOf(StringGene("name", "Bob")))))))
        assertTrue(output.contains("List<MongoInsertionDto> mongoInsertions = mongo().insertInto(\"appdb\", \"customers\")"))
        assertTrue(output.contains(".dtos();"))
        assertTrue(output.contains("MongoInsertionResultsDto mongoInsertionsresult = controller.execInsertionsIntoMongoDatabase(mongoInsertions);"))
    }

    @Test
    fun testMultipleActionsUsesAndChaining() {
        val actions = listOf(makeEvaluatedMongoAction("db1", "coll1"), makeEvaluatedMongoAction("db2", "coll2"))
        val output = writeKotlin(actions)
        assertTrue(output.contains(".and().insertInto(\"db2\", \"coll2\")"))
    }

    @Test
    fun testSkipFailureOmitsFailedActions() {
        val actions = listOf(
            makeEvaluatedMongoAction("db1", "coll1", success = true),
            makeEvaluatedMongoAction("db2", "coll2", success = false)
        )
        val output = writeKotlin(actions, skipFailure = true)
        assertTrue(output.contains("coll1"))
        assertFalse(output.contains("coll2"))
    }

    @Test
    fun testGroupIndexAndPreviousInsertionVars() {
        val insertionVars = mutableListOf<Pair<String, String>>()
        val out1 = writeKotlin(listOf(makeEvaluatedMongoAction("db1", "coll1")), insertionVars = insertionVars, groupIndex = "_0")
        val out2 = writeKotlin(listOf(makeEvaluatedMongoAction("db2", "coll2")), insertionVars = insertionVars, groupIndex = "_1")

        assertTrue(out1.contains("val mongoInsertions_0 = mongo()"))
        assertTrue(out2.contains("val mongoInsertions_1 = mongo(mongoInsertions_0)"))
        assertEquals(2, insertionVars.size)
        assertEquals("mongoInsertions_0" to "mongoInsertions_0result", insertionVars[0])
        assertEquals("mongoInsertions_1" to "mongoInsertions_1result", insertionVars[1])
    }

    @Test
    fun testKotlinEscapesDollarSign() {
        val output = writeKotlin(listOf(makeEvaluatedMongoAction(genes = listOf(ObjectGene("obj", listOf(StringGene("price", "\$100")))))))
        assertTrue(output.contains("\\\$100") || output.contains("\\${'$'}100"))
        assertFalse(output.contains("\"$100\""))
    }
}

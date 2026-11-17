package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.*
import org.evomaster.core.database.sql.schema.*
import org.evomaster.core.search.action.EvaluatedDbAction
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID

class SqlForeignKeyWriterRegressionTest {
    @TempDir lateinit var directory: Path

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun explicitGeneratedAndExistingKeysMatchSearchAndExecute(format: OutputFormat) {
        for (kind in listOf("explicit", "generated", "existing")) {
            val parentId = TableId("parent_row")
            val childId = TableId("child_row")
            val pk = Column("id", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, primaryKey = true, autoIncrement = true)
            val fk = Column("parent_id", ColumnDataType.INTEGER, databaseType = DatabaseType.H2, foreignKeyToAutoIncrement = true)
            val parent = Table(parentId, setOf(pk), emptySet())
            val child = Table(childId, setOf(fk), setOf(ForeignKey(listOf(fk), parentId, listOf(pk))))
            val parentGene = SqlPrimaryKeyGene("id", parentId,
                when (kind) {
                    "generated" -> SqlAutoIncrementGene("id")
                    "existing" -> ImmutableDataHolderGene("id", "9", false)
                    else -> IntegerGene("id", 9)
                }, 100L)
            val parentAction = SqlAction(parent, setOf(pk), 100L, listOf(parentGene), representExistingData = kind == "existing")
            val childAction = SqlAction(child, setOf(fk), 101L, listOf(
                SqlForeignKeyGene("parent_id", 101L, parentId, "id", false, uniqueIdOfPrimaryKey = 100L)))
            val actions = listOf(parentAction, childAction)
            actions.forEachIndexed { i, action -> action.setLocalId(i.toString()) }
            val lines = Lines(format)
            SqlWriter.handleDbInitialization(format, actions.map {
                EvaluatedDbAction(it, SqlActionResult(it.getLocalId()).apply { setInsertExecutionResult(true) })
            }, lines, sqlInsertionVars = mutableListOf(), skipFailure = false)
            val text = lines.toString()
            if (kind == "generated") assertTrue(text.contains(".r(\"parent_id\", 100L)"), text)
            else {
                assertTrue(text.contains(".d(\"parent_id\", \"9\")"), text)
                assertFalse(text.contains(".r("), text)
            }
            // Compile the generated DSL. Execution through the real SQL runner is checked below.
            val dsl = text.lineSequence().takeWhile { !it.contains("controller.execInsertionsIntoDatabase") }.joinToString("\n")
            val code = if (format.isJava()) """
                import java.util.List;
                import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
                import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
                public class SqlProbe { public List<InsertionDto> build() { $dsl return insertions; } }
            """.trimIndent() else """
                import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
                import org.evomaster.client.java.sql.dsl.SqlDsl.sql
                class SqlProbe { fun build(): List<InsertionDto> {
                    $dsl
                    return insertions
                } }
            """.trimIndent()
            GeneratedRegressionCompiler.withClass(directory.resolve(kind), format, "SqlProbe", code) { klass ->
                @Suppress("UNCHECKED_CAST")
                val generated = klass.getMethod("build").invoke(klass.getDeclaredConstructor().newInstance()) as List<InsertionDto>
                val searched = SqlActionTransformer.transform(actions).insertions
                assertEquals(searched.size, generated.size)
                for ((expected, actual) in searched.zip(generated)) {
                    if (expected.id != null) assertEquals(expected.id, actual.id)
                    assertEquals(expected.data.map { Triple(it.variableName, it.printableValue, it.foreignKeyToPreviouslyGeneratedRow) },
                        actual.data.map { Triple(it.variableName, it.printableValue, it.foreignKeyToPreviouslyGeneratedRow) })
                }
                DriverManager.getConnection("jdbc:h2:mem:${UUID.randomUUID()}").use { connection ->
                    connection.createStatement().use {
                        it.execute("CREATE TABLE parent_row(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY)")
                        it.execute("CREATE TABLE child_row(parent_id INTEGER REFERENCES parent_row(id))")
                        if (kind == "existing") it.execute("INSERT INTO parent_row(id) VALUES (9)")
                    }
                    val inserted = SqlScriptRunner.execInsert(connection, generated)
                    assertTrue(inserted.executionResults.all { it })
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT c.parent_id FROM child_row c JOIN parent_row p ON c.parent_id = p.id").use {
                            assertTrue(it.next())
                            assertEquals(if (kind == "generated") 1 else 9, it.getInt(1))
                            assertFalse(it.next())
                        }
                    }
                }
            }
        }
    }
}

package org.evomaster.core.database

import org.evomaster.clientJava.controller.db.SqlScriptRunner
import org.evomaster.clientJava.controller.internal.db.SchemaExtractor
import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.SqlAutoIncrementGene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.sql.Connection
import java.sql.DriverManager

class ProxyPrintSqlExtractTest {


    companion object {

        private lateinit var connection: Connection

        private val sqlSchema = this::class.java.getResource("/sql_schema/proxyprint.sql").readText()

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }
    }

    @BeforeEach
    fun initTest() {

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
    }


    @Test
    fun testCreateAndExtract() {

        SqlScriptRunner.execCommand(connection, sqlSchema)

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(15, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "ADMIN" }) },
                Executable { assertTrue(schema.tables.any { it.name == "CONSUMERS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "DOCUMENTS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "DOCUMENTS_SPECS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "EMPLOYEES" }) },
                Executable { assertTrue(schema.tables.any { it.name == "MANAGERS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "NOTIFICATION" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PRICETABLES" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PRINT_REQUESTS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "REVIEWS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "ROLES" }) },
                Executable { assertTrue(schema.tables.any { it.name == "USERS" }) }
        )

    }


    @Test
    fun testIssueWithUsers(){

        SqlScriptRunner.execCommand(connection, sqlSchema)

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("USERS", setOf("ID", "PASSWORD", "USERNAME"))

        assertEquals(1, actions.size)

        val genes = actions[0].seeGenes()

        assertEquals(3, genes.size)

        val pk = genes.find { it.name.equals("ID", ignoreCase = true) }

        assertTrue(pk is SqlPrimaryKeyGene)
        assertTrue((pk as SqlPrimaryKeyGene).gene is SqlAutoIncrementGene)
    }

    @Test
    fun testIssueWithFK(){

        SqlScriptRunner.execCommand(connection, sqlSchema)

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("PRINT_REQUESTS", setOf("CONSUMER_ID"))

        val all = actions.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        //force binding
        val randomness = Randomness()//.apply { updateSeed(1) }
        DbAction.randomizeDbActionGenes(actions, randomness)

        /*
           - PRINT_REQUESTS request has a FK to CONSUMER
           - the PK of CONSUMER is itself a FK to USERS
           - the PK of USERS is autoincrement, so not printable

           This means PRINT_REQUESTS:CONSUMER_ID has to point to a row when printed
         */

        val gene = all.find { it.name.equals("CONSUMER_ID", ignoreCase = true) }
        assertTrue(gene is SqlForeignKeyGene)

        val fk = gene as SqlForeignKeyGene
        assertTrue(fk.isBound())
        assertTrue(fk.isReferenceToNonPrintable(all))

        val dto = DbActionTransformer.transform(actions)
        assertEquals(actions.size, dto.insertions.size)
    }
}
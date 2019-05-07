package org.evomaster.core.database.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.SqlAutoIncrementGene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class ProxyPrintSqlExtractTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/proxyprint.sql"


    @Test
    fun testCreateAndExtract() {

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


        /**
         * The schema includes an alter table command that specifies that
         * table USERS has a unique column USERNAME:
         * alter table users add constraint UK_r43af9ap4edm43mmtq01oddj6 unique (username);
         */
        assertEquals(true, schema.tables.find { it.name == "USERS" }!!.columns.find { it.name == "USERNAME" }!!.unique)

        /**
         * BIGSERIAL are autoincrement fields
         */
        assertEquals(true, schema.tables.filter { it.name == "USERS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "DOCUMENTS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "DOCUMENTS_SPECS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "NOTIFICATION" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "PRINTING_SCHEMAS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "PRINTSHOPS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "PRINT_REQUESTS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "REGISTER_REQUESTS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertEquals(true, schema.tables.filter { it.name == "REVIEWS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)

        assertEquals(false, schema.tables.filter { it.name == "USERS" }.first().columns.filter { it.name == "PASSWORD" }.first().nullable)
        assertEquals(false, schema.tables.filter { it.name == "USERS" }.first().columns.filter { it.name == "USERNAME" }.first().nullable)

        assertEquals(listOf("PRINTSHOP_ID", "ITEM"), schema.tables.filter { it.name == "PRICETABLES" }.first().primaryKeySequence);

        assertEquals(true, schema.tables.filter { it.name == "CONSUMERS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement);
        assertEquals(true, schema.tables.filter { it.name == "ADMIN" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement);
        assertEquals(true, schema.tables.filter { it.name == "EMPLOYEES" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement); assertEquals(true, schema.tables.filter { it.name == "CONSUMERS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement);
        assertEquals(true, schema.tables.filter { it.name == "MANAGERS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement);
        assertEquals(true, schema.tables.filter { it.name == "PRICETABLES" }.first().columns.filter { it.name == "PRINTSHOP_ID" }.first().foreignKeyToAutoIncrement);

        assertEquals(false, schema.tables.filter { it.name == "DOCUMENTS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)
        assertEquals(false, schema.tables.filter { it.name == "DOCUMENTS_SPECS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)
        assertEquals(false, schema.tables.filter { it.name == "NOTIFICATION" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)
        assertEquals(false, schema.tables.filter { it.name == "DOCUMENTS_SPECS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)
        assertEquals(false, schema.tables.filter { it.name == "PRINTING_SCHEMAS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)
        assertEquals(false, schema.tables.filter { it.name == "REGISTER_REQUESTS" }.first().columns.filter { it.name == "ID" }.first().foreignKeyToAutoIncrement)


    }


    @Test
    fun testIssueWithUsers() {

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
    fun testIssueWithFK() {

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("PRINT_REQUESTS", setOf("CONSUMER_ID"))

        val all = actions.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        //force binding
        val randomness = Randomness()//.apply { updateSeed(1) }
        DbActionUtils.randomizeDbActionGenes(actions, randomness)

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
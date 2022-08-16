package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.sql.network.SqlCidrGene
import org.evomaster.core.search.gene.sql.network.SqlInetGene
import org.evomaster.core.search.gene.sql.network.SqlMacAddrGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject

/**
 * Created by jgaleotti on 07-May-19.
 */
class NetworkTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_network_types.sql"


    @Test
    fun testNetworkTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "NetworkTypes", setOf(
                "cidrColumn",
                "inetColumn",
                "macaddrColumn",
                "macaddr8Column"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(4, genes.size)
        assertTrue(genes[0] is SqlCidrGene)
        assertTrue(genes[1] is SqlInetGene)
        assertTrue(genes[2] is SqlMacAddrGene)
        assertTrue(genes[3] is SqlMacAddrGene)

        assertEquals(SqlMacAddrGene.MACADDR6_SIZE, (genes[2] as SqlMacAddrGene).size())
        assertEquals(SqlMacAddrGene.MACADDR8_SIZE, (genes[3] as SqlMacAddrGene).size())

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val resultSet = SqlScriptRunner.execCommand(connection, "SELECT * FROM NetworkTypes;")

        assertTrue(resultSet.seeRows().isNotEmpty())

        val dataRow = resultSet.seeRows().first()

        assertTrue(dataRow.getValueByName("cidrColumn") is PGobject)
        assertTrue(dataRow.getValueByName("inetColumn") is PGobject)
        assertTrue(dataRow.getValueByName("macaddrColumn") is PGobject)
        assertTrue(dataRow.getValueByName("macaddr8Column") is PGobject)

    }
}
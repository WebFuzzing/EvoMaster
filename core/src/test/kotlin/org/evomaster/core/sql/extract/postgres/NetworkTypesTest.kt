package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.network.CidrGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.network.MacAddrGene
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
        assertTrue(genes[0] is CidrGene)
        assertTrue(genes[1] is InetGene)
        assertTrue(genes[2] is MacAddrGene)
        assertTrue(genes[3] is MacAddrGene)

        assertEquals(MacAddrGene.MACADDR6_SIZE, (genes[2] as MacAddrGene).size())
        assertEquals(MacAddrGene.MACADDR8_SIZE, (genes[3] as MacAddrGene).size())

        val dbCommandDto = SqlActionTransformer.transform(actions)
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
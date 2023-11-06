package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.sql.geometric.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.postgresql.geometric.*

/**
 * Created by jgaleotti on 07-May-19.
 */
class GeometricTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_geometric_types.sql"


    @Test
    fun testGeometricTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "GeometricTypes", setOf(
                "pointColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(7, genes.size)
        assertTrue(genes[0] is SqlPointGene)
        assertTrue(genes[1] is SqlLineGene)
        assertTrue(genes[2] is SqlLineSegmentGene)
        assertTrue(genes[3] is SqlBoxGene)
        assertTrue(genes[4] is SqlPathGene)
        assertTrue(genes[5] is SqlPolygonGene)
        assertTrue(genes[6] is SqlCircleGene)

        val lineGene = genes[1] as SqlLineGene
        val p = lineGene.getViewOfChildren()[0] as SqlPointGene
        val q = lineGene.getViewOfChildren()[1] as SqlPointGene
        p.x.value = 0.0f
        p.y.value = 0.0f
        q.x.value = 1.0f
        q.y.value = 1.0f

        val pathGene = genes[4] as SqlPathGene
        (pathGene.getViewOfChildren().first() as ArrayGene<SqlPointGene>).addElement(SqlPointGene("point1"))
        (pathGene.getViewOfChildren().first() as ArrayGene<SqlPointGene>).addElement(SqlPointGene("point2"))

        val polygonGene = genes[5] as SqlPolygonGene
        (polygonGene.getViewOfChildren().first() as ArrayGene<SqlPointGene>).addElement(SqlPointGene("point1"))
        (polygonGene.getViewOfChildren().first() as ArrayGene<SqlPointGene>).addElement(SqlPointGene("point2"))

        val circleGene = genes[6] as SqlCircleGene

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val resultSet = SqlScriptRunner.execCommand(connection, "SELECT * FROM GeometricTypes;")

        assertTrue(resultSet.seeRows().isNotEmpty())

        val dataRow = resultSet.seeRows().first()
        val point = dataRow.getValueByName("pointColumn") as PGpoint
        assertEquals(0.0, point.x)
        assertEquals(0.0, point.y)

        assertTrue(dataRow.getValueByName("lineColumn") is PGline)
        assertTrue(dataRow.getValueByName("lineSegmentColumn") is PGlseg)
        assertTrue(dataRow.getValueByName("boxColumn") is PGbox)
        assertTrue(dataRow.getValueByName("pathColumn") is PGpath)
        assertTrue(dataRow.getValueByName("polygonColumn") is PGpolygon)
        assertTrue(dataRow.getValueByName("circleColumn") is PGcircle)

    }
}
package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlMultiPathGeneTest {

    val rand = Randomness()

    @BeforeEach
    fun resetSeed() {
        rand.updateSeed(42)
    }

    @Test
    fun testEmptyGetValueAsPrintable() {
        val gene = SqlMultiPathGene(name = "multilinestring", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.paths.killAllChildren()
        assertEquals("\"MULTILINESTRING EMPTY\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testNonEmptyGetValueAsPrintable() {
        val sqlLinestringGene0 = SqlPathGene(name = "linestring", databaseType = DatabaseType.H2)
        sqlLinestringGene0.randomize(rand,true)
        sqlLinestringGene0.points.killAllChildren()
        sqlLinestringGene0.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p2",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val gene = SqlMultiPathGene(name = "multilinestring", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.paths.killAllChildren()
        gene.paths.addElement(sqlLinestringGene0)
        assertEquals("\"MULTILINESTRING((0.0 1.0, 1.0 1.0, 0.0 0.0))\"",gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintableH2() {
        val sqlLinestringGene0 = SqlPathGene(name = "linestring", databaseType = DatabaseType.H2)
        sqlLinestringGene0.randomize(rand,true)
        sqlLinestringGene0.points.killAllChildren()
        sqlLinestringGene0.points.addElement(SqlPointGene("p0", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p1", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p2", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val sqlLinestringGene1 = SqlPathGene(name = "linestring", databaseType = DatabaseType.H2)
        sqlLinestringGene1.randomize(rand,true)
        sqlLinestringGene1.points.killAllChildren()
        sqlLinestringGene1.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene1.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene1.points.addElement(SqlPointGene("p2",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
        ))

        val sqlLinestringGene2 = SqlPathGene(name = "linestring", databaseType = DatabaseType.H2)
        sqlLinestringGene2.randomize(rand,true)
        sqlLinestringGene2.points.killAllChildren()


        val gene = SqlMultiPathGene(name = "multilinestring", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.paths.killAllChildren()
        gene.paths.addElement(sqlLinestringGene0)
        gene.paths.addElement(sqlLinestringGene1)
        gene.paths.addElement(sqlLinestringGene2)

        assertEquals("\"MULTILINESTRING((0.0 1.0, 1.0 1.0, 0.0 0.0), (1.0 1.0, 0.0 1.0, 1.0 0.0), EMPTY)\"",gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintableMySQL() {
        val sqlLinestringGene0 = SqlPathGene(name = "linestring", databaseType = DatabaseType.MYSQL)
        sqlLinestringGene0.randomize(rand,true)
        sqlLinestringGene0.points.killAllChildren()
        sqlLinestringGene0.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene0.points.addElement(SqlPointGene("p2",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val sqlLinestringGene1 = SqlPathGene(name = "linestring", databaseType = DatabaseType.MYSQL)
        sqlLinestringGene1.randomize(rand,true)
        sqlLinestringGene1.points.killAllChildren()
        sqlLinestringGene1.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene1.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        sqlLinestringGene1.points.addElement(SqlPointGene("p2",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
        ))

        val gene = SqlMultiPathGene(name = "multilinestring", databaseType = DatabaseType.MYSQL)
        gene.randomize(rand,true)
        gene.paths.killAllChildren()
        gene.paths.addElement(sqlLinestringGene0)
        gene.paths.addElement(sqlLinestringGene1)

        assertEquals("MULTILINESTRING(LINESTRING(POINT(0.0, 1.0), POINT(1.0, 1.0), POINT(0.0, 0.0)), LINESTRING(POINT(1.0, 1.0), POINT(0.0, 1.0), POINT(1.0, 0.0)))",gene.getValueAsPrintableString())
    }


}
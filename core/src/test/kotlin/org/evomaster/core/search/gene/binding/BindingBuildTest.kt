package org.evomaster.core.search.gene.binding

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BindingBuildTest {

    @Test
    fun testBuilderForRest(){
        val ancestorPath = RestPath("/api/foo")
        val f1 = LongGene("id", 1L)
        val f2 = DoubleGene("doubleValue", 2.0)
        val f3 = IntegerGene("intValue", 3)
        val f4 = FloatGene("floatValue", 4f)
        val bodyGene = ObjectGene("foo", fields = listOf(f1,f2,f3,f4))
        val bodyParam = BodyParam(bodyGene, EnumGene("contentType", listOf("application/json")))
        val post = RestCallAction(id="post",verb = HttpVerb.POST,path = ancestorPath.copy(), parameters = mutableListOf(bodyParam))

        val path = RestPath("/api/foo/{id}")
        val disruptiveGene = CustomMutationRateGene("id", f1.copy(), 1.0)
        (disruptiveGene.gene as LongGene).value = 5L
        val pathParam = PathParam("id", disruptiveGene)
        val queryPathParam = QueryParam("doubleValue", f2.copy())
        (queryPathParam.gene as DoubleGene).value = 6.0
        val get = RestCallAction("get", HttpVerb.GET, path = path.copy(), parameters = mutableListOf(pathParam.copy(), queryPathParam.copy()))

        val map = BindingBuilder.buildBindBetweenParams(bodyParam, post.path, get.path, get.parameters, randomness = null)
        assertEquals(2, map.size)
        assert(map.map { "${it.first.name}${it.second.name}" }.contains("${f1.name}${f1.name}"))
        assert(map.map { "${it.first.name}${it.second.name}" }.contains("${f2.name}${f2.name}"))

        post.bindBasedOn(get.path, get.parameters, randomness = null)
        val idfield = ((post.parameters.first() as? BodyParam)?.gene as? ObjectGene)?.fields?.find { it.name == f1.name }
        assertNotNull(idfield)
        assertEquals(5L, (idfield as LongGene).value)
        val doubleValuefield = ((post.parameters.first() as? BodyParam)?.gene as? ObjectGene)?.fields?.find { it.name == f2.name }
        assertNotNull(doubleValuefield)
        assertEquals(6.0, (doubleValuefield as DoubleGene).value)
    }

    @Test
    fun testBuilderForRestAndDb(){
        val c1 = Column("id", ColumnDataType.BIGSERIAL, primaryKey = true, databaseType = DatabaseType.H2)
        val c2 = Column("doubleValue", ColumnDataType.DOUBLE, databaseType = DatabaseType.H2)
        val c3 = Column("intValue", ColumnDataType.INT4, databaseType = DatabaseType.H2)
        val c4 = Column("floatValue", ColumnDataType.REAL, databaseType = DatabaseType.H2)
        val table = Table("foo", columns = setOf(c1,c2,c3,c4), foreignKeys = setOf())
        val sqlAction = SqlAction(table, setOf(c1,c2,c3,c4), 0L, representExistingData = false)

        val ancestorPath = RestPath("/api/foo")
        val f1 = LongGene("id", 1L)
        val f2 = DoubleGene("doubleValue", 2.0)
        val f3 = IntegerGene("intValue", 3)
        val f4 = FloatGene("floatValue", 4f)
        val bodyGene = ObjectGene("foo", fields = listOf(f1,f2,f3,f4))
        val bodyParam = BodyParam(bodyGene, EnumGene("contentType", listOf("application/json")))
        val post = RestCallAction(id="post",verb = HttpVerb.POST,path = ancestorPath.copy(), parameters = mutableListOf(bodyParam))


    }


}
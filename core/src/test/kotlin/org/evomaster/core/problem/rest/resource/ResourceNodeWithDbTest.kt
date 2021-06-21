package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.db.DbCleaner
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.SqlInsertBuilderTest
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.resource.dependency.BodyParamRelatedToTable
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class ResourceNodeWithDbTest {


    companion object {

        private lateinit var connection: Connection
        private lateinit var sqlInsertBuilder: SqlInsertBuilder
        private val schema = OpenAPIParser().readLocation("/swagger/artificial/resource_test.json", null, null).openAPI
        private val actionCluster: MutableMap<String, Action> = mutableMapOf()
        private val cluster = ResourceCluster()
        private val randomness = Randomness()

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")

            SqlScriptRunner.execCommand(connection, """
                CREATE TABLE RFOO (id  bigint not null, doubleValue double, intValue int, floatValue float, primary key (id));
                CREATE TABLE RBAR (id  bigint not null, name varchar(256), fooId bigint not null, primary key(id));
                alter table RBAR add constraint RBAR_FK foreign key (fooId) references RFOO(id);
                CREATE TABLE RXYZ (id  bigint not null, name varchar(256), barId bigint not null, primary key(id));
                alter table RXYZ add constraint RXYZ_FK foreign key (barId) references RBAR(id);
            """)

            SqlScriptRunner.execCommand(connection, "INSERT INTO RFOO (id, doubleValue, intValue, floatValue) VALUES (0, 1.0, 2, 3.0)")
            SqlScriptRunner.execCommand(connection, "INSERT INTO RBAR (id, name, fooId) VALUES (1, 'bar', 0)")
            SqlScriptRunner.execCommand(connection, "INSERT INTO RXYZ (id, name, barId) VALUES (2, 'xyz', 1)")
            SqlScriptRunner.execCommand(connection, "INSERT INTO RFOO (id, doubleValue, intValue, floatValue) VALUES (3, 4.0, 5, 6.0)")

            val dbschema = SchemaExtractor.extract(connection)
            sqlInsertBuilder = SqlInsertBuilder(dbschema, DbExecutor())

            RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster)
            val config = EMConfig()
            config.doesApplyNameMatching = true
            config.probOfEnablingResourceDependencyHeuristics = 1.0
            cluster.initResourceCluster(actionCluster, sqlInsertBuilder = sqlInsertBuilder, config = config)
            cluster.initRelatedTables()
        }
    }

    @BeforeEach
    fun initTest() {
        DbCleaner.clearDatabase_H2(connection)
    }

    @Test
    fun testClusterWithDbInit(){
        //rfoo, rfoo/{id}, rbar, rbar/{id}, rxyz
        assertEquals(6, cluster.getCluster().size)

        // tabe in db
        assertEquals(setOf("RFOO", "RBAR", "RXYZ"), cluster.getTableInfo().keys)

        // data in db
        assertEquals(2, cluster.getDataInDb("RFOO")?.size)
        assertEquals(1, cluster.getDataInDb("RBAR")?.size)
        assertEquals(1, cluster.getDataInDb("RXYZ")?.size)

        val rfooNode = cluster.getResourceNode("/v3/api/rfoo")
        assertNotNull(rfooNode)
        rfooNode!!.resourceToTable.apply {
            assertTrue(derivedMap.keys.contains("RFOO"))
            assertEquals(1, paramToTable.size)
            assertTrue(paramToTable.values.first() is BodyParamRelatedToTable)
            (paramToTable.values.first() as BodyParamRelatedToTable).apply {
                assertEquals(4, fieldsMap.size)
                fieldsMap.forEach { t, u ->
                    assertEquals(1, u.derivedMap.size)
                    u.derivedMap.forEach { ut, uu ->
                        assertEquals("RFOO", ut)
                        assertEquals(uu.input.toLowerCase(), uu.targetMatched.toLowerCase())
                    }
                }
            }
        }
    }


    @Test
    fun testBindingWithDbInOneCall(){
        // /v3/api/rfoo
        val dbactions = sqlInsertBuilder.createSqlInsertionAction("RFOO")
        val dbFooId = getGenePredict(dbactions.first(), "id") { g: Gene -> g is LongGene }
        (dbFooId as LongGene).value = 42L
        assertEquals(1, dbactions.size)

        val postFoo = cluster.getResourceNode("/v3/api/rfoo")!!.sampleRestResourceCalls("POST", randomness, 10)
        postFoo.is2POST = true
        postFoo.initDbActions(dbactions, cluster, false, false)
        val fooBodyId = getGenePredict(postFoo.seeActions(ActionFilter.NO_SQL).first(), "id") { g: Gene -> g is LongGene }
        val fooDbId = getGenePredict(postFoo.seeActions(ActionFilter.ONLY_SQL).first(), "id") { g: Gene -> g is LongGene }

        // because id here is primary key, param is updated based on db gene
        assertEquals((fooBodyId as LongGene).value, (fooDbId as LongGene).value)
        assertEquals(42, fooBodyId.value)

        // /v3/api/rfoo/{rfooId}
        val getFoo = cluster.getResourceNode("/v3/api/rfoo/{rfooId}")!!.sampleRestResourceCalls("GET", randomness, maxTestSize = 10)
        val previousGetFooId = getGenePredict(getFoo.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){g: Gene-> g is LongGene}
        (previousGetFooId as LongGene).value = 40
        getFoo.is2POST = true
        val createFoo = sqlInsertBuilder.createSqlInsertionAction("RFOO")
        val createGetFooId = getGenePredict(createFoo.first(), "id") { g: Gene -> g is LongGene }
        assertNotNull(createGetFooId)
        (createGetFooId as LongGene).value = 42

        getFoo.initDbActions(createFoo, cluster, false, false)
        val fooGetId = getGenePredict(getFoo.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){g: Gene-> g is LongGene}
        val fooCreateGetId = getGenePredict(getFoo.seeActions(ActionFilter.ONLY_SQL).first(), "id") { g: Gene -> g is LongGene }
        assertEquals((fooGetId as LongGene).value, (fooCreateGetId as LongGene).value)
        assertEquals(42, fooGetId.value)

        // /v3/api/rfoo/{rfooId}/rbar/{rbarId}
        val getBarNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}")!!
        val getBar = getBarNode.sampleRestResourceCalls("GET", randomness, maxTestSize = 10)
        val paramInfo = getBarNode.getPossiblyBoundParams("GET", true)
        val bindingMap = SimpleDeriveResourceBinding.generateRelatedTables(paramInfo, getBar, listOf())

        val relatedTables = bindingMap.values.flatMap { it.map { g->g.tableName } }.toSet()
        assertEquals(setOf("RFOO","RBAR"), relatedTables)

        val sorted = DbActionUtils.sortTable(relatedTables.mapNotNull { cluster.getTableByName(it) }.toSet()).map { t-> t.name }
        assertEquals(listOf("RBAR", "RFOO"), sorted)

        val dbActionsToCreate = cluster.createSqlAction(setOf("RFOO","RBAR"), sqlInsertBuilder, mutableListOf(), true, randomness= randomness)
        assertEquals(2, dbActionsToCreate.size)

        val barFooId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){g: Gene-> g is LongGene}
        val barId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rbarId"){g: Gene-> g is LongGene}


    }

    @Test
    fun testDbActionCreation(){

        val fooAndBar = cluster.createSqlAction(setOf("RFOO","RBAR"), sqlInsertBuilder, mutableListOf(), true, randomness= randomness)
        assertEquals(2, fooAndBar.size)


        val fooAndBar2 =  cluster.createSqlAction(setOf("RFOO","RBAR"), sqlInsertBuilder, fooAndBar, true, randomness= randomness)
        assertEquals(0, fooAndBar2.size)

        val xyz = cluster.createSqlAction(setOf("RXYZ"), sqlInsertBuilder, mutableListOf(), true, randomness= randomness)
        assertEquals(3, xyz.size)

        val xyz2 = cluster.createSqlAction(setOf("RFOO","RBAR"), sqlInsertBuilder, xyz, true, randomness= randomness)
        assertEquals(0, xyz2.size)
    }


    private fun getGenePredict(action: Action, name: String, predict: (Gene) -> Boolean) : Gene?{
        return action.seeGenes().flatMap { it.flatView() }.find { g-> predict(g) && g.name.equals(name, ignoreCase = true) }
    }

    private class DbExecutor : DatabaseExecutor {

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): Map<Long, Long>? {
            return null
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return SqlScriptRunner.execCommand(connection, dto.command).toDto()
        }

        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return false
        }
    }
}
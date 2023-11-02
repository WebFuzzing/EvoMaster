package org.evomaster.core.problem.rest.resource

import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.EMConfig
import org.evomaster.core.sql.DatabaseExecutor
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.resource.dependency.BodyParamRelatedToTable
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
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

            val config = EMConfig()
            config.doesApplyNameMatching = true
            config.probOfEnablingResourceDependencyHeuristics = 1.0

            RestActionBuilderV3.addActionsFromSwagger(schema, actionCluster, enableConstraintHandling = config.enableSchemaConstraintHandling)
            cluster.initResourceCluster(actionCluster, sqlInsertBuilder = sqlInsertBuilder, config = config)
            cluster.initRelatedTables()
        }
    }

    @Test
    fun testClusterWithDbInit(){
        //rfoo, rfoo/{id}, rbar, rbar/{id}, rxyz
        assertEquals(6, cluster.getCluster().size)

        // table in db
        assertTrue(cluster.getTableInfo().keys.containsAll(setOf("RFOO", "RBAR", "RXYZ")))

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

        val rbarNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}")
        assertNotNull(rbarNode)
        rbarNode!!.resourceToTable.apply {
            assertTrue(derivedMap.keys.contains("RFOO"))
            assertTrue(derivedMap.keys.contains("RBAR"))
        }

        val rxyzNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}")
        assertNotNull(rxyzNode)
        rxyzNode!!.resourceToTable.apply {
            assertTrue(derivedMap.keys.contains("RFOO"))
            assertTrue(derivedMap.keys.contains("RBAR"))
            assertTrue(derivedMap.keys.contains("RXYZ"))
        }
    }

    @Test
    fun testDbActionCreation(){

        val fooAndBar = cluster.createSqlAction(listOf("RFOO","RBAR"), sqlInsertBuilder, mutableListOf(), true, randomness= randomness)
        assertEquals(2, fooAndBar.size)

        val fooAndBar2 =  cluster.createSqlAction(listOf("RFOO","RBAR"), sqlInsertBuilder, fooAndBar, true, randomness= randomness)
        assertEquals(0, fooAndBar2.size)

        val xyz = cluster.createSqlAction(listOf("RXYZ"), sqlInsertBuilder, mutableListOf(), true, randomness= randomness)
        assertEquals(3, xyz.size)

        val xyz2 = cluster.createSqlAction(listOf("RFOO","RBAR"), sqlInsertBuilder, xyz, true, randomness= randomness)
        assertEquals(0, xyz2.size)

        val xyz3 = cluster.createSqlAction(listOf("RFOO","RFOO","RBAR","RBAR"), sqlInsertBuilder, mutableListOf(), false, randomness= randomness)
        assertEquals(2 + 2*2, xyz3.size)

        val xyzSelect = cluster.createSqlAction(listOf("RXYZ"), sqlInsertBuilder, mutableListOf(), true, isInsertion = false, randomness = randomness)
        assertEquals(1, xyzSelect.size)
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
        val previousGetFooId = getGenePredict(getFoo.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){ g: Gene-> g is LongGene }
        (previousGetFooId as LongGene).value = 40
        getFoo.is2POST = true
        val createFoo = sqlInsertBuilder.createSqlInsertionAction("RFOO")
        val createGetFooId = getGenePredict(createFoo.first(), "id") { g: Gene -> g is LongGene }
        assertNotNull(createGetFooId)
        (createGetFooId as LongGene).value = 42

        getFoo.initDbActions(createFoo, cluster, false, false)
        val fooGetId = getGenePredict(getFoo.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){ g: Gene-> g is LongGene }
        val fooCreateGetId = getGenePredict(getFoo.seeActions(ActionFilter.ONLY_SQL).first(), "id") { g: Gene -> g is LongGene }
        assertEquals((fooGetId as LongGene).value, (fooCreateGetId as LongGene).value)
        assertEquals(42, fooGetId.value)

        // /v3/api/rfoo/{rfooId}/rbar/{rbarId}
        val getBarNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}")!!
        val getBar = getBarNode.sampleRestResourceCalls("GET", randomness, maxTestSize = 10)
        val fooBarDbActionToCreate = cluster.createSqlAction(listOf("RFOO", "RBAR"), sqlInsertBuilder, mutableListOf(), true, randomness = randomness)
        assertEquals(2, fooBarDbActionToCreate.size)
        getBar.initDbActions(fooBarDbActionToCreate, cluster, false, false)
        val barFooId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){ g: Gene-> g is LongGene }
        val barId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rbarId"){ g: Gene-> g is LongGene }
        val dbFooIdInGetBar = getGenePredict(getBar.seeActions(ActionFilter.ONLY_SQL).first(), "id"){ g: Gene -> g is LongGene }
        val dbBarIdInGetBar = getGenePredict(getBar.seeActions(ActionFilter.ONLY_SQL)[1], "id"){ g: Gene -> g is LongGene }
        assertEquals((barFooId as LongGene).value, (dbFooIdInGetBar as LongGene).value)
        assertTrue(dbFooIdInGetBar.isDirectBoundWith(barFooId))
        assertEquals((barId as LongGene).value, (dbBarIdInGetBar as LongGene).value)
        assertTrue(barId.isDirectBoundWith(dbBarIdInGetBar))

        // /v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}
        val xYZNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}")!!
        val getXYZ = xYZNode.sampleRestResourceCalls("GET", randomness, 10)
        val xyzDbActions = cluster.createSqlAction(listOf("RXYZ", "RBAR", "RFOO"), sqlInsertBuilder, mutableListOf(), true, randomness = randomness)
        getGenePredict(xyzDbActions[0], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 42
        }
        getGenePredict(xyzDbActions[1], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 43
        }
        getGenePredict(xyzDbActions[2], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 44
        }
        assertEquals(3, xyzDbActions.size)
        getXYZ.initDbActions(xyzDbActions, cluster, false, false)
        val xyzFooId = getGenePredict(getXYZ.seeActions(ActionFilter.NO_SQL)[0], "rfooId"){ g: Gene-> g is LongGene }
        val xyzBarId = getGenePredict(getXYZ.seeActions(ActionFilter.NO_SQL)[0], "rbarId"){ g: Gene-> g is LongGene }
        val xyzId = getGenePredict(getXYZ.seeActions(ActionFilter.NO_SQL)[0], "rxyzId"){ g: Gene-> g is LongGene }
        val dbXYZFooId = getGenePredict(getXYZ.seeActions(ActionFilter.ONLY_SQL)[0], "id"){ g: Gene-> g is LongGene }
        val dbXYZBarId = getGenePredict(getXYZ.seeActions(ActionFilter.ONLY_SQL)[1], "id"){ g: Gene-> g is LongGene }
        val dbXYZId = getGenePredict(getXYZ.seeActions(ActionFilter.ONLY_SQL)[2], "id"){ g: Gene-> g is LongGene }

        assertEquals((xyzFooId as LongGene).value, (dbXYZFooId as LongGene).value)
        assertEquals(42, xyzFooId.value)
        assertTrue(xyzFooId.isDirectBoundWith(dbXYZFooId))
        assertEquals((xyzBarId as LongGene).value, (dbXYZBarId as LongGene).value)
        assertEquals(43, xyzBarId.value)
        assertTrue(xyzBarId.isDirectBoundWith(dbXYZBarId))
        assertEquals((xyzId as LongGene).value, (dbXYZId as LongGene).value)
        assertEquals(44, xyzId.value)
        assertTrue(xyzId.isDirectBoundWith(dbXYZId))
    }

    @Test
    fun testBindingWithOtherCall(){

        val xYZNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}/rxyz/{rxyzId}")!!
        val getXYZ = xYZNode.sampleRestResourceCalls("GET", randomness, 10)
        val dbXYZ = cluster.createSqlAction(listOf("RFOO", "RBAR", "RXYZ"), sqlInsertBuilder, mutableListOf(), true, randomness = randomness)
        getGenePredict(dbXYZ[0], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 42
        }
        getGenePredict(dbXYZ[1], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 43
        }
        getGenePredict(dbXYZ[2], "id"){g: Gene-> g is LongGene }.apply {
            (this as? LongGene)?.value = 44
        }
        getXYZ.initDbActions(dbXYZ, cluster, false, false)

        val xyzFooId = getGenePredict(getXYZ.seeActions(ActionFilter.NO_SQL)[0], "rfooId"){ g: Gene-> g is LongGene }
        val xyzBarId = getGenePredict(getXYZ.seeActions(ActionFilter.NO_SQL)[0], "rbarId"){ g: Gene-> g is LongGene }
        val dbXYZFooId = getGenePredict(getXYZ.seeActions(ActionFilter.ONLY_SQL)[0], "id"){ g: Gene-> g is LongGene }
        val dbXYZBarId = getGenePredict(getXYZ.seeActions(ActionFilter.ONLY_SQL)[1], "id"){ g: Gene-> g is LongGene }


        val getBarNode = cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}")!!
        val getBar = getBarNode.sampleRestResourceCalls("GET", randomness, maxTestSize = 10)
        val dbBar = cluster.createSqlAction(listOf("RFOO", "RBAR"), sqlInsertBuilder, mutableListOf(), true, randomness = randomness)
        getBar.initDbActions(dbBar, cluster, false, false)
        assertEquals(2, getBar.seeActionSize(ActionFilter.ONLY_SQL))

        getBar.bindWithOtherRestResourceCalls(mutableListOf(getXYZ), cluster,true, randomness = null)
        assertEquals(0, getBar.seeActionSize(ActionFilter.ONLY_SQL))

        assertFalse(getXYZ.isDeletable)
        assertTrue(getXYZ.shouldBefore.contains(getBar.getResourceNodeKey()))

        val barFooId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rfooId"){ g: Gene-> g is LongGene }
        val barId = getGenePredict(getBar.seeActions(ActionFilter.NO_SQL).first(), "rbarId"){ g: Gene-> g is LongGene }

        assertEquals((xyzBarId as LongGene).value, (barId as LongGene).value)
        assertEquals((dbXYZBarId as LongGene).value, barId.value)
        assertEquals(43, barId.value)
        assertEquals((xyzFooId as LongGene).value, (barFooId as LongGene).value)
        assertEquals(42, barFooId.value)
        assertEquals((dbXYZFooId as LongGene).value, barFooId.value)

    }

    private fun getGenePredict(action: Action, name: String, predict: (Gene) -> Boolean) : Gene?{
        return action.seeTopGenes().flatMap { it.flatView() }.find { g-> predict(g) && g.name.equals(name, ignoreCase = true) }
    }

    private class DbExecutor : DatabaseExecutor {

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
            return null
        }

        override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? {
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
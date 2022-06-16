package org.evomaster.core.problem.rest.resource

import com.google.inject.*
import com.netflix.governator.guice.LifecycleInjector
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.matchproblem.PrimitiveTypeMatchIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.junit.ClassRule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import java.sql.DriverManager
import java.util.stream.Stream
import kotlin.math.max
import kotlin.math.min


class RestIndividualResourceTest {

    private lateinit var sampler : ResourceSampler
    private lateinit var mutator : ResourceRestMutator
    private lateinit var rm : ResourceManageService
    private lateinit var ff : RestResourceFitness
    private lateinit var archive : Archive<RestIndividual>
    private lateinit var searchTimeController: SearchTimeController

    private lateinit var config : EMConfig
    private lateinit var cluster : ResourceCluster


    companion object {
        private lateinit var connection: Connection
        var randomness : Randomness = Randomness()
        var sqlInsertBuilder : SqlInsertBuilder? = null

        private val MOCKSERVER_IMAGE = DockerImageName.parse(
            "jamesdbloom/mockserver:mockserver-5.5.4"
        )
        private val mockServer: MockServerContainer = MockServerContainer(MOCKSERVER_IMAGE)
        lateinit var sutAddress: String

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
            mockServer.start()
            sutAddress = "http://${mockServer.host}:${mockServer.serverPort}"
        }


        @JvmStatic
        fun getBudgetAndNumOfResource(): Stream<Arguments> {
            val budget = arrayOf(10, 100, 1000)
            val range = (1..30)
            return range.map {r-> budget.map { Arguments.of(it, r) } }.flatten().stream()
        }

        @AfterAll
        fun clean(){
            mockServer.close()
        }
    }

    @BeforeEach
    fun initTest() {
        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
    }

    private fun sampleDbAction(table : Table) : List<DbAction>{

        val actions = sqlInsertBuilder!!.createSqlInsertionAction(table.name)
        return actions.map { it.copy() as DbAction}
    }

    private fun registerTable(tableName: String, columns: List<Pair<String, Boolean>>, columnTypes: List<ColumnDataType>){
        SqlScriptRunner.execCommand(connection,
            "CREATE TABLE $tableName(${columns.mapIndexed { index, pair -> "${pair.first} ${columnTypes[index].name} ${if (pair.second) "not null" else ""}" }.joinToString(",")});")

    }

    private fun initResourceNode(numResource: Int, maxPropertySize: Int){
        // sample actioncluster and dbactions
        val spec = sampleResourceSpec(numResource, maxPropertySize)
        spec.forEach {
            registerTable(it.resourceName, it.properties, it.columnTypes)
        }
        val openAPI = openApiSchema(spec)

        val schema = SchemaExtractor.extract(connection)

        val injector: Injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(
                BaseModule(emptyArray(), true),
                ResourceRestModule(false),
                FakeModule(
                    SutInfoDto().apply {
                        restProblem = RestProblemDto().apply {
                            openApiSchema = openAPI
                            defaultOutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_4
                            baseUrlOfSUT = sutAddress
                        }
                        sqlSchemaDto = schema
                    },
                    ControllerInfoDto()
                )))
            .build().createInjector()

        randomness = injector.getInstance(Randomness::class.java)

        config = injector.getInstance(EMConfig::class.java)
        sampler = injector.getInstance(ResourceSampler::class.java)
        mutator = injector.getInstance(ResourceRestMutator::class.java)
        sqlInsertBuilder = sampler.sqlInsertBuilder
        rm = injector.getInstance(ResourceManageService::class.java)
        ff = injector.getInstance(RestResourceFitness::class.java)
        archive = injector.getInstance(Key.get(
            object : TypeLiteral<Archive<RestIndividual>>() {}))
        cluster = rm.cluster
        searchTimeController = injector.getInstance(SearchTimeController::class.java)

        config.useTimeInFeedbackSampling = false
        config.seed = 42

    }


    data class ResourceSpec(
        val resourceName: String,
        val properties: List<Pair<String, Boolean>>,
        val propertyTypes: List<String>,
        val propertyFormats: List<String?>,
        val columnTypes: List<ColumnDataType>)

    private fun generatePropertyName(size: Int) = "${randomness.nextLetter()}${(0 until size).map { randomness.nextLetter() }.joinToString("")}"

    private fun generateResourceName() = "${randomness.nextLetter().uppercaseChar()}${generatePropertyName(5)}"

    private fun sampleResourceSpec(numResource: Int, maxPropertySize: Int) : List<ResourceSpec>{
        return (0 until numResource).map {
            val size = randomness.nextInt(1, max(2, maxPropertySize))
            val columnTypes = (0 until size).map { randomness.choose(typeCluster()) }
            ResourceSpec(
                resourceName = generateResourceName(),
                properties = (0 until size).map { generatePropertyName(4) to randomness.nextBoolean()},
                columnTypes = columnTypes,
                propertyTypes = columnTypes.map {
                    when(it){
                        ColumnDataType.INT -> "integer"
                        ColumnDataType.VARCHAR -> "string"
                        ColumnDataType.DATE -> "string"
                        else -> throw IllegalArgumentException("not support $it in current test")
                    }
                },
                propertyFormats = columnTypes.map {
                    when(it){
                        ColumnDataType.INT -> "int32"
                        ColumnDataType.DATE -> "date"
                        else -> null
                    }
                }
            )
        }
    }

    // now only involve these types in tests, might expand it when needed
    private fun typeCluster() = listOf(ColumnDataType.INT, ColumnDataType.VARCHAR, ColumnDataType.DATE)

    private fun sampleResourceCall(resNode: RestResourceNode? = null): RestResourceCalls{
        val node = resNode?: randomness.choose(cluster.getCluster().values)
        val sampleOption = randomness.nextInt(0, 3)
        return when (sampleOption) {
            0 -> node.sampleOneAction(verb = null, randomness)
            1 -> node.sampleIndResourceCall(randomness, config.maxTestSize)
            2 -> node.sampleAnyRestResourceCalls(randomness, config.maxTestSize)
            3 -> node.sampleRestResourceCalls(randomness.choose(node.getTemplates().values).template, randomness, config.maxTestSize)
            else -> throw IllegalStateException("not support")
        }
    }

    private fun sampleRestIndividual(dbSize : Int, resourceSize: Int): RestIndividual{
        val dbActions = mutableListOf<DbAction>()
        val resoureCalls = mutableListOf<RestResourceCalls>()
        do {
            val table = randomness.choose(cluster.getTableInfo().values)
            dbActions.addAll(sampleDbAction(table))
        }while (dbActions.size < dbSize)


        do {
            val node = randomness.choose(cluster.getCluster().values)
            resoureCalls.add(sampleResourceCall(node))
        }while (resoureCalls.size < resourceSize)
        return RestIndividual(dbInitialization = dbActions, resourceCalls = resoureCalls, sampleType = SampleType.RANDOM)
    }

    @ParameterizedTest
    @MethodSource("getBudgetAndNumOfResource")
    fun testIndividualResourceManipulation(iteration: Int, numResource: Int){
        initResourceNode(numResource, 5)

        config.maxActionEvaluations = iteration
        config.maxTestSize = 20
        (0 until iteration).forEach { _ ->
            val dbSize = randomness.nextInt(1, 15)
            val resourceSize = randomness.nextInt(2, 4)

            val ind = sampleRestIndividual(dbSize, resourceSize)
            assertEquals(dbSize + resourceSize, ind.getViewOfChildren().size)

            val call = sampleResourceCall()

            // add
            val addedIndex = randomness.nextInt(0, resourceSize-1)
            ind.addResourceCall(addedIndex, call)
            assertEquals(dbSize+addedIndex, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize+1, ind.getViewOfChildren().size)

            // remove
            ind.removeResourceCall(addedIndex)
            assertEquals(-1, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize, ind.getViewOfChildren().size)

            // replace
            val original = ind.getIndexedResourceCalls()
            val old = randomness.choose(original.keys)
            val oldRes = original[old]!!
            ind.replaceResourceCall(old, call)
            assertEquals(-1, ind.getViewOfChildren().indexOf(oldRes))
            assertEquals(dbSize + old, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize+resourceSize, ind.getViewOfChildren().size)

            // swap
            val setToSwap = ind.getIndexedResourceCalls()
            val candidates = randomness.choose(setToSwap.keys, 2)
            assertEquals(2, candidates.size)
            val first = setToSwap[candidates.first()]!!
            val second = setToSwap[candidates.last()]!!
            ind.swapResourceCall(candidates.first(), candidates.last())
            assertEquals(dbSize + candidates.first(), ind.getViewOfChildren().indexOf(second))
            assertEquals(dbSize + candidates.last(), ind.getViewOfChildren().indexOf(first))

        }

    }

    @ParameterizedTest
    @MethodSource("getBudgetAndNumOfResource")
    fun testSampledIndividual(iteration: Int, numResource: Int){
        initResourceNode(numResource, 5)
        config.maxActionEvaluations = iteration

        (0 until iteration).forEach { _ ->
            val ind = sampler.sample()

            assertEquals(0, ind.seeInitializingActions().size)
            if (ind.seeDbActions().isNotEmpty()){
                // all db actions should be before rest actions
                ind.getResourceCalls().forEach { r->
                    val dbIndexes = r.getIndexedChildren(DbAction::class.java).keys
                    val restIndexes = r.getIndexedChildren(RestCallAction::class.java).keys
                    assertTrue(restIndexes.all {
                        it > (dbIndexes.maxOrNull() ?: -1)
                    })
                }
            }
        }
    }

    @Disabled // not finish
    @ParameterizedTest
    @MethodSource("getBudgetAndNumOfResource")
    fun testMutatedIndividual(iteration: Int, numResource: Int){
        initResourceNode(numResource, 5)
        config.maxActionEvaluations = iteration
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val ind = sampler.sample()
        var eval = ff.calculateCoverage(ind)
        assertNotNull(eval)

        var evalution = 1
        do{
            evalution ++
            val mutated = mutator.mutateAndSave(1, eval!!, archive)
            mutated.tracking.apply {
                assertNotNull(this)
                assertEquals(min(evalution, config.maxLengthOfTraces), this!!.history.size)
                assertNotNull(this.history.last().evaluatedResult)
                assertEquals(EvaluatedMutation.BETTER_THAN,this.history.last().evaluatedResult)
            }


            eval = mutated
        }while (searchTimeController.shouldContinueSearch())
    }

    private fun openApiSchema(spec: List<ResourceSpec>) : String {

        val swagger = OpenAPI()
            .info(Info()
                .version("1.0.0")
                .title("Test")
                .apply {
                    contact = Contact()
                        .name("Test")
                        .email("foo@test")
                        .url("http://test")
                })

        val errorSchema = Schema<Any>().apply {
            type = "object"
            setProperties(mapOf("errorMessage" to StringSchema()))
        }

        swagger.schema("Error", errorSchema)

        val errorResponse = ApiResponse()
            .description("error response")
            .content(
                Content()
                    .addMediaType(
                        "application/json", MediaType()
                            .schema(Schema<Any?>().`$ref`("Error"))
                    )
            )

        val okResponse = ApiResponse()
            .description("ok response")
            .content(
                Content()
                    .addMediaType(
                        "*/*", MediaType()
                            .schema(StringSchema())
                    )
            )

        val schemas = spec.associate {
            it.resourceName to Schema<Any>().apply {
                type = "object"
                val properties = it.properties.mapIndexed { index, p ->
                    p.first to Schema<Any>().apply {
                        type = it.propertyTypes[index]
                        format = it.propertyFormats[index]
                    }
                }.toMap()
                setProperties(properties)
            }
        }


        val paths = Paths()

        schemas.forEach {
            swagger.schema(it.key, it.value)

            val bodyRequestItem = PathItem().apply {
                post(Operation()
                    .summary("adds a ${it.key}")
                    .operationId("add ${it.key}")
                    .responses(
                        ApiResponses()
                            .addApiResponse("default", errorResponse)
                    )
                    .requestBody(
                        RequestBody()
                            .description("the ${it.key} to add")
                            .content(
                                Content().addMediaType(
                                    "application/json", MediaType()
                                        .schema(Schema<Any?>().`$ref`(it.key))
                                )
                            )
                    ))
                patch(Operation()
                    .summary("update a ${it.key}")
                    .operationId("update ${it.key}")
                    .responses(
                        ApiResponses()
                            .addApiResponse("default", errorResponse)
                    )
                    .requestBody(
                        RequestBody()
                            .description("the ${it.key} to update")
                            .content(
                                Content().addMediaType(
                                    "application/json", MediaType()
                                        .schema(Schema<Any?>().`$ref`(it.key))
                                )
                            )
                    ))
                put(Operation()
                    .summary("update a ${it.key}")
                    .operationId("update ${it.key}")
                    .responses(
                        ApiResponses()
                            .addApiResponse("default", errorResponse)
                    )
                    .requestBody(
                        RequestBody()
                            .description("the ${it.key} to update")
                            .content(
                                Content().addMediaType(
                                    "application/json", MediaType()
                                        .schema(Schema<Any?>().`$ref`(it.key))
                                )
                            )
                    ))

            }

            paths.addPathItem("/api/${it.key}",bodyRequestItem)

            val firstParameter = it.value.properties.toList().first()

            val pathRequestItem = PathItem().apply {
                get(
                    Operation()
                        .summary("get a ${it.key}")
                        .operationId("get ${it.key}")
                        .responses(
                            ApiResponses()
                                .addApiResponse("default", errorResponse)
                        )
                        .responses(
                            ApiResponses()
                                .addApiResponse("200", okResponse)
                                .addApiResponse("default", errorResponse)
                        )
                        .apply {
                            addParametersItem(
                                Parameter()
                                    .`in`("path")
                                    .name(firstParameter.first)
                                    .description("${it.key} to fetch")
                                    .schema(firstParameter.second)
                            )
                        }
                )

                delete(
                    Operation()
                        .summary("delete a ${it.key}")
                        .operationId("delete ${it.key}")
                        .responses(
                            ApiResponses()
                                .addApiResponse("default", errorResponse)
                        )
                        .responses(
                            ApiResponses()
                                .addApiResponse("200", okResponse)
                                .addApiResponse("default", errorResponse)
                        )
                        .apply {
                            addParametersItem(
                                Parameter()
                                    .`in`("path")
                                    .name(firstParameter.first)
                                    .description("${it.key} to delete")
                                    .schema(firstParameter.second)
                            )
                        }
                )
            }

            paths.addPathItem("/api/${it.key}/{${firstParameter.first}}",pathRequestItem)

        }

        swagger.paths(paths)
        return Json.mapper().writeValueAsString(swagger)
    }

    private class FakeModule(val sutInfoDto: SutInfoDto?,
                             val controllerInfoDto: ControllerInfoDto?) : AbstractModule() {
        @Provides
        @Singleton
        fun getRemoteController(): RemoteController {
            return FakeRemoteController(sutInfoDto, controllerInfoDto)
        }
    }

    class FakeRemoteController(val sutInfoDto: SutInfoDto?,
                               val controllerInfoDto: ControllerInfoDto?) : RemoteController{

        private var executedActionCounter = 0
        private var targetIdCounter = 0

        private fun resetExecutedActionCounter(){
            executedActionCounter = 0
        }
        private fun executeAction(){
            executedActionCounter++
        }

        private fun newEvaluation(){
            targetIdCounter++
        }

        override fun getSutInfo(): SutInfoDto? {
            return sutInfoDto
        }

        override fun getControllerInfo(): ControllerInfoDto? {
            return controllerInfoDto
        }

        override fun startSUT(): Boolean {
            return true
        }

        override fun stopSUT(): Boolean {
            return true
        }

        override fun resetSUT(): Boolean {
            return true
        }

        override fun checkConnection() {
        }

        override fun startANewSearch(): Boolean {
            resetExecutedActionCounter()
            return true
        }

        override fun getTestResults(ids: Set<Int>, ignoreKillSwitch: Boolean): TestResultsDto? {
            assertNotNull(sqlInsertBuilder)
            newEvaluation()
            val result = TestResultsDto().apply {
                targets = listOf(TargetInfoDto().apply {
                    id = targetIdCounter
                    value = 1.0
                    actionIndex = randomness.nextInt(executedActionCounter)
                })
                additionalInfoList = (0 until executedActionCounter).map { AdditionalInfoDto() }
                extraHeuristics = (0 until executedActionCounter).map {
                    ExtraHeuristicsDto().apply {
                        if (randomness.nextBoolean()){
                            databaseExecutionDto = ExecutionDto().apply {
                                val table = randomness.choose( sqlInsertBuilder!!.getTableNames())
                                val failed = randomness.choose(sqlInsertBuilder!!.getTable(table).columns.map { it.name })
                                failedWhere = mapOf(table to setOf(failed))
                            }
                        }
                    }
                }
            }

            resetExecutedActionCounter()
            return result
        }

        override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto): ActionResponseDto? {
            return null
        }

        override fun registerNewAction(actionDto: ActionDto): Boolean {
            executeAction()
            return true
        }

        override fun address(): String {
            return "localhost:40100"
        }

        override fun close() {

        }

        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return true
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return null
        }

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
            return null
        }

    }
}
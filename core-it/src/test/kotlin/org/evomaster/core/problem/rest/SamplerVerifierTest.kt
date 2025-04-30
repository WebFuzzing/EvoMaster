package org.evomaster.core.problem.rest

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult
import org.evomaster.core.BaseModule
import org.evomaster.core.problem.rest.service.module.ResourceRestModule
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.time.Duration


class SamplerVerifierTest {


    @Test
    fun testBase() {

        val resourcePath = "swagger/sut/news.json"

        val sutInfo = SutInfoDto()
        sutInfo.restProblem = RestProblemDto()
        sutInfo.restProblem.openApiSchema = this::class.java.classLoader.getResource(resourcePath).readText()
        sutInfo.defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4

        val controllerInfo = ControllerInfoDto()

        val injector = getInjector(sutInfo, controllerInfo)

        val sampler = injector.getInstance(ResourceSampler::class.java)

        sampler.sample() //should not crash
    }

    //@Timeout(10, unit = TimeUnit.SECONDS) // this timeout is not working
   // @Execution(ExecutionMode.CONCURRENT) //issues with shared caches

    @TestFactory
    fun testSamplingFromAllSchemasUnderCoreResources(): Collection<DynamicTest>{
        return sampleFromSchemasAndCheckInvariants("../core/src/test/resources/swagger", "swagger")
    }


    //FIXME need to put back, and investigate performance bug
    @Disabled("Major issues with timeouts. Even before, took more than 1 hour. Need refactoring. Maven was not showing the failures (likely bug in Surefire)")
    @TestFactory
    fun testSamplingFromAPIsGuru(): Collection<DynamicTest>{
        return sampleFromSchemasAndCheckInvariants("./src/test/resources/APIs_guru", "APIs_guru")
    }


    private fun sampleFromSchemasAndCheckInvariants(relativePath: String, resourceFolder: String): Collection<DynamicTest> {

        return scanForSchemas(relativePath, resourceFolder)
            .sorted().map {
            DynamicTest.dynamicTest(it) {
                System.gc()
                assertTimeoutPreemptively(Duration.ofSeconds(30), it) {
                    runInvariantCheck(it, 100)
                }
            }
        }.toList()
    }

    private fun scanForSchemas(relativePath: String, resourceFolder: String) : List<String>{
        val target = File(relativePath)
        if (!target.exists()) {
            throw IllegalStateException("OpenAPI resource folder does not exist: ${target.absolutePath}")
        }

        return target.walk()
                .filter { it.isFile }
                .filter { !it.name.endsWith("features_service_null.json") } //issue with parser
                .filter { !it.name.endsWith("trace_v2.json") } // no actions are parsed
                .filter { !skipSchema(it.path) }
                .map {
                    val s = it.path.replace("\\", "/")
                            .replace(relativePath, resourceFolder)
                    s
                }.toList()
    }

    private fun skipSchema(path: String) : Boolean {
        return skipDueToMissingPath(path)
                || skipDueToHashTag(path)
                || skipDueToQuestionMarkInPath(path)
                || skipDueToMissingReference(path)
                || skipDueToUnhandledFormat(path)
                || skipDueToTimeout(path)
                || skipDueToException(path)
                || skipDueToUnhandledTypeFormat(path)
                || skipDueToInvalid(path)
                || skipDueToOverflow(path)
                || skipDueToInvalidGenes(path)
    }

    //TODO should look into theses
    private fun skipDueToOverflow(path: String) : Boolean{
        return (path.contains("spectrocoin") && path.contains("1.0.0"))
                || (path.contains("whapi.com") && path.contains("numbers") && path.contains("2.0"))
    }

    //TODO should look into theses
    private fun skipDueToInvalidGenes(path: String) : Boolean{
        return (path.contains("apple.com") && path.contains("sirikit-cloud-media") && path.contains("1.0.2"))
                || (path.contains("graphhopper.com")  && path.contains("1.0.0"))
    }

    // purposely invalid, artificial schemas
    private fun skipDueToInvalid(path: String) : Boolean{
        return path.contains("invalid_")
    }

    // skip MarketPayNotificationService since there does not exist paths, need to check if we update the schema
    private fun skipDueToMissingPath(path: String) : Boolean{
        return path.contains("MarketPayNotificationService")
    }

    // enable it once support #
    private fun skipDueToHashTag(path: String) : Boolean{
        return path.contains("amazonaws.com")
    }

    // enable it once support ?
    private fun skipDueToQuestionMarkInPath(path: String) : Boolean{
        return path.run {
            contains("box.com") || contains("clicksend.com") || contains("discourse.local")
                    || contains("flickr.com") || contains("formapi.io") || contains("hydramovies.com")
                    || contains("icons8.com") || contains("learnifier.com") || contains("mastercard.com")
                    || contains("osisoft.com") || contains("staging-ecitaco.com") || contains("weatherbit.io")
                    || (contains("azure.com") && contains("hdinsight-job"))
        }
    }

    private fun skipDueToMissingReference(path: String) : Boolean{
        return path.run {
            /*
                missing cross-reference among specifications
                eg, swagger/all-api-guru/azure.com/network-expressRouteCircuit/2017-08-01/swagger.yaml
                contains ./routeFilter.json#/definitions/RouteFilter

                it will lead to "Malformed or unreadable swagger supplied"
                
             */
            (contains("azure.com") && contains("network-"))
        }
    }

    private fun skipDueToUnhandledFormat(path: String): Boolean{
        return path.contains("bungie.net") //uint8
    }

    private fun skipDueToUnhandledTypeFormat(path: String) : Boolean{
        return (path.contains("ote-godaddy.com") && path.contains("1.0.0")) // Cannot handle combination null/host-name
    }

    /*
        test fails due to exception,

        need to check how to handle  "The incoming YAML document exceeds the limit: 3145728 code points."
     */
    private fun skipDueToException(path: String): Boolean{
        return path.run{
            (contains("googleapis.com") && contains("compute") && (contains("alpha") || contains("beta") || contains("v1")))
                    || (contains("googleapis.com") && contains("contentwarehouse") && contains("v1"))
                    || (contains("googleapis.com") && contains("youtubeAnalytics") && contains("v1"))
                    || (contains("gov.bc.ca") && contains("jobposting") && contains("1.0.0"))
                    || (contains("here.com") && contains("tracking") && contains("2.1.58"))
                    || (contains("interactivebrokers.com") && contains("1.0.0"))
                    || (contains("ipinfodb.com") && contains("1.0.0"))
                    || (contains("iva-api.com") && contains("2.0"))
                    || (contains("kubernetes.io") && contains("unversioned"))
                    || (contains("kubernetes.io") && contains("v1.10.0"))
                    || (contains("loket.nl") && contains("V2"))
                    || (contains("microsoft.com") && contains("cognitiveservices-Prediction") && (contains("1.1") || contains("2.0") || contains("3.0")))
                    || (contains("microsoft.com") && contains("graph") && (contains("v1.0") || contains("beta")))
                    || (contains("nexmo.com") && contains("number-insight") && contains("1.2.1"))
                    || (contains("opensuse.org") && contains("obs") && contains("2.10.50"))
                    || (contains("optimade.local") && contains("1.1.0"))
                    || (contains("pandascore.co") && contains("2.23.1"))
                    || (contains("plaid.com") && contains("2020-09-14_1.20.6"))
                    || (contains("probely.com") && contains("1.2.0"))
                    || (contains("rudder.example.local") && contains("13"))
                    || (contains("sendgrid.com") && contains("1.0.0")) // special characters are not allowed
                    || (contains("snyk.io") && contains("1.0.0"))
                    || (contains("soundcloud.com") && contains("1.0.0"))
                    || (contains("staging-ecotaco.com") && contains("1.0.0"))
                    || (contains("stoplight.io") && contains("api-v1"))
                    || (contains("storecove.com") && contains("2.0.1"))
                    || (contains("visualstudio.com") && contains("v1"))
                    || (contains("youneedabudget.com") && contains("1.0.0"))
                    || (contains("zenoti.com") && contains("1.0.0")) //  No actions for schema
                    || (contains("zoom.us") && contains("2.0.0")) // The incoming YAML document exceeds the limit: 3145728 code points.
                    || (contains("zuora.com") && contains("2021-08-20")) //The incoming YAML document exceeds the limit: 3145728 code points.
        }
    }

    /*
        fail due to timeout, need a further check
     */
    private fun skipDueToTimeout(path: String) : Boolean{
        return path.run{
            /*
                APIs_guru/bunq.com/1.0/openapi.yaml
                seems quite take time eg, with 20s, it still fails due to execution timed out after 20000 ms
             */
            (contains("bunq.com") && contains("1.0"))
                    || (contains("canada-holidays.ca"))
                    || (contains("cloudmersive.com") && contains("ocr"))
                    || (contains("docusign.net"))
                    || (contains("firstinspires.org"))
                    || (contains("github.com") && (contains("ghes-2.22") || contains("ghes-3.0") || contains("ghes-3.1")))
                    || (contains("data2crm.com") && contains("1"))
                    || (contains("googleapis.com") && contains("accesscontextmanager") && contains("v1"))
                    || (contains("googleapis.com") && contains("adexchangebuyer2") && contains("v2beta1"))
                    || (contains("googleapis.com") && contains("analyticsdata") && contains("v1beta"))
                    || (contains("googleapis.com") && contains("analyticsreporting") && contains("v4"))
                    || (contains("googleapis.com") && contains("batch") && contains("v1"))
                    || (contains("googleapis.com") && contains("bigquery") && contains("v2"))
                    || (contains("googleapis.com") && contains("chat") && contains("v1"))
                    || (contains("googleapis.com") && contains("cloudbilling") && contains("v1beta"))
                    || (contains("googleapis.com") && contains("cloudbuild") && contains("v1"))
                    || (contains("googleapis.com") && contains("clouddebugger") && contains("v2"))
                    || (contains("googleapis.com") && contains("cloudtrace") && contains("v2"))
                    || (contains("googleapis.com") && contains("container") && contains("v1"))
                    || (contains("googleapis.com") && contains("container") && contains("v1beta1"))
                    || (contains("googleapis.com") && contains("containeranalysis") && contains("v1"))
                    || (contains("googleapis.com") && contains("containeranalysis") && contains("v1alpha1"))
                    || (contains("googleapis.com") && contains("containeranalysis") && contains("v1beta1"))
                    || (contains("googleapis.com") && contains("dataflow") && contains("v1b3"))
                    || (contains("googleapis.com") && contains("dataproc") && contains("v1"))
                    || (contains("googleapis.com") && contains("datastore") && contains("v1beta3"))
                    || (contains("googleapis.com") && contains("accesscontextmanager") && contains("v1"))
                    || (contains("googleapis.com") && contains("dfareporting") && (contains("v3.3") || contains("v3.4") || contains("v3.5") || contains("v4")))
                    || (contains("googleapis.com") && contains("displayvideo") && (contains("v1") || contains("v2")))
                    || (contains("googleapis.com") && contains("dlp") && contains("v2"))
                    || (contains("googleapis.com") && contains("docs") && contains("v1"))
                    || (contains("googleapis.com") && contains("documentai") && contains("v1"))
                    || (contains("googleapis.com") && contains("documentai") && contains("v1beta3"))
                    || (contains("googleapis.com") && contains("domains") && (contains("v1") || contains("v1alpha2") || contains("v1beta1")))
                    || (contains("googleapis.com") && contains("doubleclickbidmanager") && (contains("v1") || contains("v2")))
                    || (contains("googleapis.com") && contains("drive") && (contains("v2") || contains("v3")))
                    || (contains("googleapis.com") && contains("firestore") && (contains("v1") || contains("v1beta1")))
                    || (contains("googleapis.com") && contains("forms") && contains("v1"))
                    || (contains("googleapis.com") && contains("gameservices") && (contains("v1") || contains("v1beta")))
                    || (contains("googleapis.com") && contains("genomics") && contains("v1"))
                    || (contains("googleapis.com") && contains("accesscontextmanager") && contains("v2alpha1"))
                    || (contains("googleapis.com") && contains("gkehub") && contains("v1"))
                    || (contains("googleapis.com") && contains("integrations") && contains("v1alpha"))
                    || (contains("googleapis.com") && contains("jobs") && (contains("v3") || contains("v4")))
                    || (contains("googleapis.com") && contains("monitoring") && (contains("v1") || contains("v3")))
                    || (contains("googleapis.com") && contains("mybusinessbusinessinformation") && contains("v1"))
                    || (contains("googleapis.com") && contains("mybusinesslodging") && contains("v1"))
                    || (contains("googleapis.com") && contains("networkmanagement") && contains("v1"))
                    || (contains("googleapis.com") && contains("networkservices") && contains("v1"))
                    || (contains("googleapis.com") && contains("osconfig") && contains("v1"))
                    || (contains("googleapis.com") && contains("paymentsresellersubscription") && contains("v1"))
                    || (contains("googleapis.com") && contains("people") && contains("v1"))
                    || (contains("googleapis.com") && contains("playablelocations") && contains("v3"))
                    || (contains("googleapis.com") && contains("policysimulator") && contains("v1"))
                    || (contains("googleapis.com") && contains("run") && contains("v2"))
                    || (contains("googleapis.com") && contains("safebrowsing") && contains("v4"))
                    || (contains("googleapis.com") && contains("servicecontrol") && contains("v1"))
                    || (contains("googleapis.com") && contains("servicemanagement") && contains("v1"))
                    || (contains("googleapis.com") && contains("sheets") && contains("v4"))
                    || (contains("googleapis.com") && contains("slides") && contains("v1"))
                    || (contains("googleapis.com") && contains("sourcerepo") && contains("v1"))
                    || (contains("googleapis.com") && contains("tagmanager") && contains("v1"))
                    || (contains("googleapis.com") && contains("testing") && contains("v1"))
                    || (contains("googleapis.com") && contains("toolresults") && contains("v1beta3"))
                    || (contains("googleapis.com") && contains("transcoder") && contains("v1"))
                    || (contains("googleapis.com") && contains("vision") && (contains("v1p1beta1") || contains("v1p2beta1")))
                    || (contains("googleapis.com") && contains("vmmigration") && contains("v1"))
                    || (contains("jellyfin.local") && contains("v1"))
                    || (contains("jirafe.com") && contains("2.0.0"))
                    || (contains("keycloak.local") && contains("1"))
                    || (contains("mist.com") && contains("0.12.4"))
                    || (contains("patientview.org") && contains("1.0"))
                    || (contains("royalmail.com") && contains("click-and-drop") && contains("1.0.0"))
                    || (contains("seldon.local") && (contains("seldon.local") || contains("wrapper")) && contains("0.1"))
                    || (contains("shotstack.io") && contains("v1"))
                    || (contains("stripe.com") && contains("2020-08-27"))
                    || (contains("ticketmaster.com") && contains("publish") && contains("v2"))
                    || (contains("tyk.com") && contains("1.9"))
                    || (contains("vocadb.net") && contains("v1"))
                    || (contains("walletobjects.googleapis.com") && contains("pay-passes") && contains("v1"))
                    || (contains("windows.net") && contains("batch-BatchService") && contains("2016-07-01.3.1"))
                    || (contains("windows.net") && contains("batch-BatchService") && contains("2017-05-01.5.0"))
                    || (contains("windows.net") && contains("batch-BatchService") && contains("2017-06-01.5.1"))
                    || (contains("xero.com") && contains("xero-payroll-au") && contains("2.9.4"))
                    || (contains("xero.com") && contains("xero_accounting") && contains("2.9.4"))
                    || (contains("xero.com") && contains("xero_accounting") && contains("2.9.4"))
                    || (contains("xero.com") && contains("xero_bankfeeds") && contains("2.9.4"))
                    || (contains("dataflowkit.com")  && contains("1.3"))
                    || (contains("rebilly.com")  && contains("2.1"))
        }
    }


    private fun runInvariantCheck(resourcePath: String, iterations: Int){

        val sutInfo = SutInfoDto()
        sutInfo.restProblem = RestProblemDto()
        sutInfo.restProblem.openApiSchema = this::class.java.classLoader.getResource(resourcePath).readText()
        sutInfo.defaultOutputFormat = SutInfoDto.OutputFormat.JAVA_JUNIT_4

        val controllerInfo = ControllerInfoDto()

        val injector = getInjector(sutInfo, controllerInfo, listOf("--seed","42"))

        val sampler = injector.getInstance(ResourceSampler::class.java)

        if(sampler.numberOfDistinctActions() == 0){
            throw IllegalStateException("No actions for schema")
        }

        for(i in 0..iterations) {
            val ind = sampler.sample()
            checkInvariant(ind)
        }
    }

    private fun checkInvariant(ind: Individual){

        assertTrue(ind.isInitialized(), "Sampled individual is not initialized")
        assertTrue(ind.areValidActionLocalIds(), "Sampled individual should have action components which have valid local ids")

        val actions = ind.seeAllActions()

        for(a in actions){

            val topGenes = a.seeTopGenes()
            for(tg in topGenes) {
                assertTrue(tg.isLocallyValid())
                assertTrue(tg.parent !is Gene)
            }
        }

        //TODO check global validity

        //TODO more checks, eg validity
    }

    private fun getInjector(
            sutInfoDto: SutInfoDto?,
            controllerInfoDto: ControllerInfoDto?,
            args: List<String> = listOf()): Injector {

        val base = BaseModule(args.toTypedArray())
        val problemModule = ResourceRestModule(false)
        val faker = FakeModule(sutInfoDto, controllerInfoDto)

        return LifecycleInjector.builder()
                .withModules(base, problemModule, faker)
                .build()
                .createInjector()
    }

    private class FakeModule(val sutInfoDto: SutInfoDto?,
                             val controllerInfoDto: ControllerInfoDto?) : AbstractModule() {
        //        override fun configure() {
//            bind(RemoteController::class.java)
//                    .to(FakeRemoteController::class.java)
//                    .asEagerSingleton()
//        }
        @Provides
        @Singleton
        fun getRemoteController(): RemoteController {
            return FakeRemoteController(sutInfoDto, controllerInfoDto)
        }
    }

    private class FakeRemoteController(
            val sutInfoDto: SutInfoDto?,
            val controllerInfoDto: ControllerInfoDto?) : RemoteController {
        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return true
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return null
        }

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
            return null
        }

        override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? {
            return null
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
            return true
        }

        override fun getTestResults(ids: Set<Int>,
                                    ignoreKillSwitch: Boolean,
                                    fullyCovered: Boolean,
                                    descriptiveIds: Boolean): TestResultsDto? {
            return null
        }

        override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto): ActionResponseDto? {
            return null
        }

        override fun postSearchAction(postSearchActionDto: PostSearchActionDto): Boolean {
            return true
        }

        override fun registerNewAction(actionDto: ActionDto): Boolean {
            return true
        }

        override fun address(): String {
            return "localhost:40100"
        }

        override fun close() {
        }

        override fun invokeScheduleTasksAndGetResults(dtos: ScheduleTaskInvocationsDto): ScheduleTaskInvocationsResult? {
            return null
        }
    }
}
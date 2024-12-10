package org.evomaster.core.output.service

import com.google.inject.AbstractModule
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.ceil

class TestSuiteWriterRPCTest{

    private val baseTargetFolder = "target/TestSuiteWriterRPCTest"

    private class ReducedModule : AbstractModule(){
        override fun configure() {
            bind(TestCaseWriter::class.java)
                    .to(RPCTestCaseWriter::class.java)
                    .asEagerSingleton()

            bind(PartialOracles::class.java)
                    .asEagerSingleton()
        }
    }


    @Test
    fun testSuiteWithCustomizedRPCExternalService(){

        val injector = LifecycleInjector.builder()
                .withModules(BaseModule(), ReducedModule())
                .build().createInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.KOTLIN_JUNIT_5
        config.outputFolder = "$baseTargetFolder/rpc_suite"
        config.outputFilePrefix = "testSuiteWithCustomizedRPCExternalService"
        config.outputFileSuffix = ""

        // rpc test generation configuration
        config.enablePureRPCTestGeneration = true
        config.enableCustomizedMethodForMockObjectHandling=true
        config.enableRPCAssertionWithInstance = true
        config.enableBasicAssertions = true
        config.saveMockedResponseAsSeparatedFile = true
        config.testResourcePathToSaveMockedResponse = "$baseTargetFolder/rpc_suite/resources"

        val expectedJson = 5

        val solution = Solution(
            mutableListOf(
                //build fake rpc individual in order to test its generated tests
                EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
                    actions = EvaluatedIndividualBuilder.buildFakeRPCAction(expectedJson),
                    externalServicesActions = (0 until expectedJson).map {
                        EvaluatedIndividualBuilder.buildFakeDbExternalServiceAction(1).plus(EvaluatedIndividualBuilder.buildFakeRPCExternalServiceAction(1))
                    }.toMutableList(),

                    format = OutputFormat.KOTLIN_JUNIT_5
                )
            ),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            listOf(),
            listOf()
        )


        val srcFolder = File(config.outputFolder)
        srcFolder.deleteRecursively()

        val writer = injector.getInstance(TestSuiteWriter::class.java)

        writer.writeTests(solution, FakeController::class.qualifiedName!!, null)


        val generatedTest = Paths.get("${config.outputFolder}/${config.outputFilePrefix}.kt")
        assertTrue(Files.exists(generatedTest))

        val generatedResource = Paths.get(config.testResourcePathToSaveMockedResponse)
        assertTrue(Files.isDirectory(generatedResource))
        assertTrue(Files.exists(generatedResource))
        assertEquals(expectedJson * 2, Files.list(generatedResource).count().toInt())

        val expectedExReset = "controller.mockRPCExternalServicesWithCustomizedHandling(null,false)"
        val expectedMockDbReset = "controller.mockDatabasesWithCustomizedHandling(null,false)"

        /*
            here, we only check the generated in text
            once we resolve the resource path in compiling test with separated files,
            this test might be extended to check the compiled test instead of tests in text
         */
        val testContent = String(Files.readAllBytes(generatedTest))
        assertTrue(testContent.contains(expectedExReset))
        assertTrue(testContent.contains(expectedMockDbReset))

        (0 until 5).forEach {
            assertTrue(testContent.contains("controller.mockRPCExternalServicesWithCustomizedHandling(controller.readFileAsStringFromTestResource(\"test_0_MockExternalServiceObjectInfo_${it}.json\"),true)"))
            assertTrue(testContent.contains("controller.mockRPCExternalServicesWithCustomizedHandling(controller.readFileAsStringFromTestResource(\"test_0_MockExternalServiceObjectInfo_${it}.json\"),false)"))

            assertTrue(testContent.contains("controller.mockDatabasesWithCustomizedHandling(controller.readFileAsStringFromTestResource(\"test_0_MockDatabaseObjectInfo_${it}.json\"),true)"))
            assertTrue(testContent.contains("controller.mockDatabasesWithCustomizedHandling(controller.readFileAsStringFromTestResource(\"test_0_MockDatabaseObjectInfo_${it}.json\"),false)"))
        }
    }


    @Test
    fun testSuiteMaxTestSuiteLimitPerFile(){

        val injector = LifecycleInjector.builder()
            .withModules(BaseModule(), ReducedModule())
            .build().createInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.maxTestsPerTestSuite = 5
        config.maxTestSize = 4

        val size = 12

        val fileSize = ceil(size * 1.0 / config.maxTestsPerTestSuite).toInt()

        val solution = Solution(
            (0 until  size).map {
                //build fake rpc individual in order to test its generated tests
                EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
                    actions = EvaluatedIndividualBuilder.buildFakeRPCAction(config.maxTestSize),
                    externalServicesActions = (0 until config.maxTestSize).map {
                        listOf<ApiExternalServiceAction>()
                    }.toMutableList(),
                    format = OutputFormat.KOTLIN_JUNIT_5
                )
            }.toMutableList(),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            listOf(),
            listOf()
        )

        val srcFolder = File(config.outputFolder)
        srcFolder.deleteRecursively()

        val split = TestSuiteSplitter.splitSolutionByLimitSize(solution as Solution<ApiWsIndividual>, config.maxTestsPerTestSuite)

        assertEquals(fileSize, split.size)
    }


    @Test
    fun testSuiteTestsByInterface(){

        val injector = LifecycleInjector.builder()
            .withModules(BaseModule(), ReducedModule())
            .build().createInjector()

        val config = injector.getInstance(EMConfig::class.java)

        val size = 12
        val interfaceSize = 4

        val prefixes = (0 until interfaceSize).map { "${config.outputFilePrefix}_Fake${it}Interface" }.plus(
            "${config.outputFilePrefix}_${TestSuiteSplitter.MULTIPLE_RPC_INTERFACES}"
        )

        val solution = Solution(
            (0 until  size).map {
                //build fake rpc individual in order to test its generated tests
                EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
                    actions = EvaluatedIndividualBuilder.buildFakeRPCAction(config.maxTestSize, "Fake${it % interfaceSize}Interface"),
                    externalServicesActions = (0 until config.maxTestSize).map {
                        listOf<ApiExternalServiceAction>()
                    }.toMutableList(),
                    format = OutputFormat.KOTLIN_JUNIT_5
                )
            }.plus(
                EvaluatedIndividualBuilder.buildEvaluatedRPCIndividual(
                    actions = (0 until interfaceSize).flatMap {
                        EvaluatedIndividualBuilder.buildFakeRPCAction(1, "Fake${it % interfaceSize}Interface")
                    }.toMutableList(),
                    externalServicesActions = (0 until interfaceSize).map {
                        listOf<ApiExternalServiceAction>()
                    }.toMutableList(),
                    format = OutputFormat.KOTLIN_JUNIT_5
                )

            ).toMutableList(),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            listOf(),
            listOf()
        )


        val split = TestSuiteSplitter.splitRPCByException(solution).splitOutcome


        assertEquals(prefixes.size, split.size)
        assertTrue(split.map { it.testSuiteNamePrefix }.containsAll(prefixes))


    }
}

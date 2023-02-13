package org.evomaster.core.output.service

import com.google.inject.AbstractModule
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
        config.enableCustomizedExternalServiceHandling=true
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
                            EvaluatedIndividualBuilder.buildFakeRPCExternalServiceAction(1)
                        }.toMutableList(),
                        format = OutputFormat.KOTLIN_JUNIT_5
                    )
                ),
                config.outputFilePrefix,
                config.outputFileSuffix,
                Termination.NONE
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
        assertEquals(expectedJson, Files.list(generatedResource).count().toInt())

        val expectedExReset = "controller.mockRPCExternalServicesWithCustomizedHandling(null,false)"

        /*
            here, we only check the generated in text
            once we resolve the resource path in compiling test with separated files,
            this test might be extended to check the compiled test instead of tests in text
         */
        val testContent = String(Files.readAllBytes(generatedTest))
        assertTrue(testContent.contains(expectedExReset))
        (0 until 5).forEach {
            assertTrue(testContent.contains("controller.mockRPCExternalServicesWithCustomizedHandling(controller.readFileAsStringFromTestResource(\"test_0_MockedResponseInfo_${it}.json\"),true)"))
        }
    }
}
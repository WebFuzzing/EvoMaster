package org.evomaster.core.output.service

import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class TestSuiteWriterTest{

    /*
        Generated files during the tests should always be somewhere
        under the "target" folder. Reason:
        - it is under .gitignore
        - "mvn clean" will remove them
     */
    private val baseTargetFolder = "target/TestSuiteWriterTest"

    @Test
    fun testEmptySuite(){

        val injector = LifecycleInjector.builder()
                .withModules(BaseModule())
                .build().createInjector()

        val solution = Solution<RestIndividual>(
                FitnessValue(0.0),
                mutableListOf()
                )

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.KOTLIN_JUNIT_5
        config.outputFolder = "$baseTargetFolder/empty_suite"
        config.testSuiteFileName = "Foo_testEmptySuite"

        //make sure we delete any existing folder from previous test runs
        val folder = File(config.outputFolder)
        folder.deleteRecursively()

        //compiled file should not exists yet
        val expectedCompiledFile = folder.toPath()
                .resolve("${config.testSuiteFileName}.class")
                .toFile()
        assertFalse(expectedCompiledFile.exists())

        val writer = injector.getInstance(TestSuiteWriter::class.java)

        //write the test suite
        writer.writeTests(solution, FakeController::class.qualifiedName!!)

        //the .kt file should exist, but .class not yet
        assertFalse(expectedCompiledFile.exists())

        CompilerForTestGenerated.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                folder,
                folder // for simplicity we compile into the same folder
                )

        //now the compiled filed should exist
        assertTrue(expectedCompiledFile.exists())
    }
}
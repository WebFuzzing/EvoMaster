package org.evomaster.core.output.service

import com.google.inject.AbstractModule
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.problem.rest.RestIndividual
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

    private class ReducedModule : AbstractModule(){
        override fun configure() {
            //point here is to avoid connections to SUT...
            bind(TestCaseWriter::class.java)
                    .to(RestTestCaseWriter::class.java)
                    .asEagerSingleton()

            bind(PartialOracles::class.java)
                    .asEagerSingleton()
        }
    }


    @Test
    fun testEmptySuite(){

        val injector = LifecycleInjector.builder()
                .withModules(BaseModule(), ReducedModule())
                .build().createInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.KOTLIN_JUNIT_5
        config.outputFolder = "$baseTargetFolder/empty_suite"
        config.outputFilePrefix = "Foo_testEmptySuite"
        config.outputFileSuffix = ""

        val solution = Solution<RestIndividual>(
            mutableListOf(),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            listOf(),
            listOf()
        )


        //make sure we delete any existing folder from previous test runs
        val srcFolder = File(config.outputFolder)
        srcFolder.deleteRecursively()

        //this is what used by Maven and IntelliJ
        val testClassFolder = File("target/test-classes")
        val expectedCompiledFile = testClassFolder.toPath()
                .resolve("${config.outputFilePrefix}.class")
                .toFile()
        expectedCompiledFile.delete()
        assertFalse(expectedCompiledFile.exists())


        val writer = injector.getInstance(TestSuiteWriter::class.java)


        //write the test suite
        writer.writeTests(solution, FakeController::class.qualifiedName!!, null)

        //the .kt file should exist, but .class not yet
        assertFalse(expectedCompiledFile.exists())

        CompilerForTestGenerated.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                srcFolder,
                testClassFolder
                )

        //now the compiled file should exist
        assertTrue(expectedCompiledFile.exists())

        /*
            as compiled directly into a folder of the classpath, current
            classloader should be able to load it
         */
        val testSuiteClass = this.javaClass.classLoader.loadClass(config.outputFilePrefix)

        val methods = testSuiteClass.declaredMethods
        assertTrue(methods.any { it.name == "initClass" })
        assertTrue(methods.any { it.name == "tearDown" })
        assertTrue(methods.any { it.name == "initTest" })
    }
}

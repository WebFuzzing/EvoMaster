package org.evomaster.core.output.service

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provider
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.search.Solution
import org.evomaster.test.utils.py.PyLoader
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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

            //will crash if methods called on it
            bind(CallGraphService::class.java)
                // avoid dependency injection on the instance, as it would require Sampler and 30+ other dependencies
                .toProvider(object : Provider<CallGraphService> {
                    override fun get(): CallGraphService {
                        return CallGraphService() // Created without Guice injection
                    }
                })
        }
    }

    private fun getInjector() : Injector {
        //Issue with @PostConstruct in CallGraphService
        //        val injector = LifecycleInjector.builder()
//                .withModules(BaseModule(), ReducedModule())
//                .build().createInjector()
        val injector = Guice.createInjector(BaseModule(), ReducedModule())
        return injector
    }

    @Test
    fun testEmptySuite(){

        val injector = getInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.KOTLIN_JUNIT_5
        config.outputFolder = "$baseTargetFolder/empty_suite"
        config.outputFilePrefix = "Foo_testEmptySuite"
        config.outputFileSuffix = ""

        val solution = getEmptySolution(config)


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

    @Test
    fun testPythonCreatesPythonUtilsFile(){

        val injector = getInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.PYTHON_UNITTEST
        config.outputFolder = "$baseTargetFolder/python_utils"
        config.outputFilePrefix = "Foo_testPythonUtils"
        config.outputFileSuffix = ""

        val solution = getEmptySolution(config)

        //make sure we delete any existing folder from previous test runs
        val srcFolder = File(config.outputFolder)
        srcFolder.deleteRecursively()

        val writer = injector.getInstance(TestSuiteWriter::class.java)
        //write the test suite
        writer.writeTests(solution, FakeController::class.qualifiedName!!, null)

        // the requirements file should exist
        val requirementsFile = Paths.get("${config.outputFolder}/${TestSuiteWriter.pythonUtilsFilename}")
        assertTrue(Files.exists(requirementsFile))

        val generatedUtils = String(Files.readAllBytes(requirementsFile))
        assertTrue(generatedUtils == PyLoader::class.java.getResource("/${TestSuiteWriter.pythonUtilsFilename}").readText())
    }

    private fun getEmptySolution(config: EMConfig): Solution<RestIndividual> {
        return Solution<RestIndividual>(
            mutableListOf(),
            config.outputFilePrefix,
            config.outputFileSuffix,
            Termination.NONE,
            listOf(),
            listOf()
        )
    }

}

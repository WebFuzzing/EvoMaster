package org.evomaster.core.output.service

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provider
import com.netflix.governator.guice.LifecycleInjector
import com.sun.net.httpserver.HttpServer
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.output.compiler.GeneratedRegressionCompiler
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.search.Solution
import org.evomaster.test.utils.py.PyLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TestSuiteWriterTest{

    @TempDir
    lateinit var temporaryDirectory: Path

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun generatedEncoderDoesNotAddCharsetButPreservesExplicitCharset(format: OutputFormat) {
        val injector = getInjector()
        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = format
        config.problemType = EMConfig.ProblemType.REST
        config.outputFolder = temporaryDirectory.resolve("suite").toString()
        config.outputFilePrefix = "CharsetSuite"
        config.outputFileSuffix = ""
        injector.getInstance(TestSuiteWriter::class.java).writeTests(getEmptySolution(config), FakeController::class.qualifiedName!!, null)
        val extension = if (format.isJava()) ".java" else ".kt"
        val content = Files.readString(Paths.get(config.outputFolder, config.outputFilePrefix + extension))
        val encoderLine = content.lineSequence().single { it.contains(".encoderConfig(") }.trim().removeSuffix(";")
        val received = LinkedBlockingQueue<Pair<String, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") {
            received.add(it.requestHeaders.getFirst("Content-Type") to String(it.requestBody.readAllBytes(), Charsets.UTF_8))
            it.sendResponseHeaders(204, -1)
            it.close()
        }
        server.start()
        try {
            val imports = """
                import io.restassured.RestAssured;
                import io.restassured.config.EncoderConfig;
                import io.restassured.http.ContentType;
            """.trimIndent()
            val request = """RestAssured.given().config(RestAssured.config()$encoderLine).contentType(type).body("hello").post("http://127.0.0.1:${server.address.port}/")"""
            val code = if (format.isJava()) "$imports\npublic class CharsetProbe { public void run(String type) { $request; } }"
                else "$imports\nclass CharsetProbe { fun run(type: String) { $request } }"
            GeneratedRegressionCompiler.withClass(temporaryDirectory.resolve("compiled"), format, "CharsetProbe", code) { klass ->
                val instance = klass.getDeclaredConstructor().newInstance()
                for (type in listOf("application/octet-stream", "text/plain", "application/json", "application/octet-stream; charset=UTF-8")) {
                    klass.getMethod("run", String::class.java).invoke(instance, type)
                    val actual = received.poll(5, TimeUnit.SECONDS)
                    assertEquals(type, actual?.first)
                    assertEquals("hello", actual?.second)
                }
            }
        } finally {
            server.stop(0)
        }
    }

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
        val injector = Guice.createInjector(BaseModule(arrayOf("--blackBox","false")), ReducedModule())
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

    @Test
    fun testEmptySuiteWithEnabledEnvVar(){

        val injector = getInjector()

        val config = injector.getInstance(EMConfig::class.java)
        config.createTests = true
        config.outputFormat = OutputFormat.KOTLIN_JUNIT_5
        config.outputFolder = "$baseTargetFolder/empty_suite_use_env_var"
        config.outputFilePrefix = "Foo_testEmptySuiteUseEnvVar"
        config.outputFileSuffix = ""
        config.useEnvVarsForPathInTests = true
        config.jdkEnvVarName = "JAVA_HOME_FAKE"
        config.sutDistEnvVarName = "WFC_HOME_FAKE"
        config.sutJarEnvVarName = "fake-sut.jar"

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

        val generatedTest = Paths.get("${config.outputFolder}/${config.outputFilePrefix}.kt")
        assertTrue(Files.exists(generatedTest))

        /*
            here, we only check the generated in text
            as it requires to use method in SutHandler
         */
        val testContent = String(Files.readAllBytes(generatedTest))
        val expectedJdkPath = "org.evomaster.core.output.service.FakeController(extractSutJarNameWithEnvVarName(\"${config.sutDistEnvVarName}\", \"${config.sutJarEnvVarName}\"))"
        val expectedSutPath = ".setJavaCommand(extractJDKPathWithEnvVarName(\"${config.jdkEnvVarName}\"))"

        assertTrue(testContent.contains(expectedJdkPath))
        assertTrue(testContent.contains(expectedSutPath))
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

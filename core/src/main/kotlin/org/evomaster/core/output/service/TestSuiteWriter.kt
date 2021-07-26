package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.core.EMConfig
import org.evomaster.core.output.*
import org.evomaster.core.problem.rest.BlackBoxUtils
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import javax.annotation.PostConstruct


/**
 * Given a Solution as input, convert it to a string representation of
 * the tests that can be written to file and be compiled
 */
class TestSuiteWriter {

    companion object {
        const val jsImport = "EM"

        private const val controller = "controller"
        private const val baseUrlOfSut = "baseUrlOfSut"
        private const val expectationsMasterSwitch = "ems"
        private const val fixtureClass = "ControllerFixture"
        private const val fixture = "_fixture"

        private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var searchTimeController: SearchTimeController

    @Inject
    private lateinit var testCaseWriter: TestCaseWriter

    @Inject
    private lateinit var partialOracles: PartialOracles

    private var activePartialOracles = mutableMapOf<String, Boolean>()


    fun writeTests(
        solution: Solution<*>,
        controllerName: String?
    ) {
        val name = TestSuiteFileName(solution.getFileName())
        val content = convertToCompilableTestCode(solution, name, controllerName)
        saveToDisk(content, config, name)
    }


    fun convertToCompilableTestCode(
        solution: Solution<*>,
        testSuiteFileName: TestSuiteFileName,
        controllerName: String?
    ): String {

        val lines = Lines()
        val testSuiteOrganizer = TestSuiteOrganizer()

        activePartialOracles = partialOracles.activeOracles(solution.individuals)

        header(solution, testSuiteFileName, lines, controllerName)

        if (! config.outputFormat.isJavaScript()) {
            /*
                In Java/Kotlin/C# the tests are inside a class, but not in JS
             */
            lines.indent()
        }

        classFields(lines, config.outputFormat)

        beforeAfterMethods(controllerName, lines, config.outputFormat, testSuiteFileName)

        //catch any sorting problems (see NPE is SortingHelper on Trello)
        val tests = try {
            testSuiteOrganizer.sortTests(solution, config.customNaming)
        } catch (ex: Exception) {
            var counter = 0
            log.warn(
                "A failure has occurred with the test sorting. Reverting to default settings. \n"
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
            )
            solution.individuals.map { ind -> TestCase(ind, "test_${counter++}") }
        }

        for (test in tests) {
            lines.addEmpty(2)

            // catch writing problems on an individual test case basis
            val testLines = try {
                if (config.outputFormat.isCsharp())
                    testCaseWriter.convertToCompilableTestCode(test, "$fixture.$baseUrlOfSut")
                else
                    testCaseWriter.convertToCompilableTestCode(test, baseUrlOfSut)
            } catch (ex: Exception) {
                log.warn(
                    "A failure has occurred in writing test ${test.name}. \n "
                            + "Exception: ${ex.localizedMessage} \n"
                            + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
                )
                Lines()
            }
            lines.add(testLines)
        }

        if (! config.outputFormat.isJavaScript()) {
            lines.deindent()
        }

        footer(lines)

        return lines.toString()
    }



    private fun saveToDisk(
        testFileContent: String,
        config: EMConfig,
        testSuiteFileName: TestSuiteFileName
    ) {

        val path = Paths.get(config.outputFolder, testSuiteFileName.getAsPath(config.outputFormat))

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }

    private fun classDescriptionEmptyLine(lines: Lines) {
        if (config.outputFormat.isJava()) {
            lines.add(" * <br>")
        } else {
            lines.add(" * ")
        }
    }

    private fun escapeDocs(s: String): String {
        return if (config.outputFormat.isKotlin()) {
            //in Kotlin Docs, [] has special meaning
            s.replace("[", "\\[").replace("]", "\\]")
        } else {
            s
        }
    }

    private fun classDescriptionComment(solution: Solution<*>, lines: Lines) {
        lines.add("/**")
        lines.add(
            " * This file was automatically generated by EvoMaster on ${
                escapeDocs(
                    ZonedDateTime.now().toString()
                )
            }"
        )
        classDescriptionEmptyLine(lines)
        lines.add(" * The generated test suite contains ${solution.individuals.size} tests")
        classDescriptionEmptyLine(lines)
        lines.add(" * Covered targets: ${solution.overall.coveredTargets()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * Used time: ${searchTimeController.getElapsedTime()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * Needed budget for current results: ${searchTimeController.neededBudget()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * ${solution.termination.comment}")
        lines.add(" */")

    }

    /**
     * This is needed for C#, in particular XUnit
     */
    private fun defineFixture(lines: Lines, controllerName: String?) {
        lines.add("public class $fixtureClass : IDisposable")

        lines.block {
            lines.addEmpty(2)
            lines.add("public ISutHandler $controller { get; private set; }")
            lines.add("public string $baseUrlOfSut { get; private set; }")

            lines.addEmpty()

            lines.add("public $fixtureClass()")
            lines.block {

                lines.addEmpty(1)
                addStatement("$controller = new $controllerName()", lines)
                addStatement("$controller.SetupForGeneratedTest()", lines)
                addStatement("$baseUrlOfSut = $controller.StartSut ()", lines)
                addStatement("Assert.NotNull($baseUrlOfSut)", lines)

            }

            lines.addEmpty()

            lines.add("public void Dispose()")
            lines.block {
                addStatement("$controller.StopSut ()", lines)
            }
        }
        lines.addEmpty()
    }

    private fun header(
        solution: Solution<*>,
        name: TestSuiteFileName,
        lines: Lines,
        controllerName: String?
    ) {

        val format = config.outputFormat

        if (name.hasPackage() && format.isJavaOrKotlin()) {
            addStatement("package ${name.getPackage()}", lines)
            lines.addEmpty(2)
        }

        if (format.isJUnit5()) {
            addImport("org.junit.jupiter.api.AfterAll", lines)
            addImport("org.junit.jupiter.api.BeforeAll", lines)
            addImport("org.junit.jupiter.api.BeforeEach", lines)
            addImport("org.junit.jupiter.api.Test", lines)
            addImport("org.junit.jupiter.api.Assertions.*", lines, true)
        }
        if (format.isJUnit4()) {
            addImport("org.junit.AfterClass", lines)
            addImport("org.junit.BeforeClass", lines)
            addImport("org.junit.Before", lines)
            addImport("org.junit.Test", lines)
            addImport("org.junit.Assert.*", lines, true)
        }

        if (format.isJava()) {
            //in Kotlin this should not be imported
            addImport("java.util.Map", lines)
        }

        if (format.isJavaOrKotlin()) {
            addImport("io.restassured.RestAssured", lines)
            addImport("io.restassured.RestAssured.given", lines, true)
            addImport("io.restassured.response.ValidatableResponse", lines)
            addImport("org.evomaster.client.java.controller.api.EMTestUtils.*", lines, true)
            addImport("org.evomaster.client.java.controller.SutHandler", lines)
            addImport("org.evomaster.client.java.controller.db.dsl.SqlDsl.sql", lines, true)
            addImport(InsertionDto::class.qualifiedName!!, lines)
            addImport("java.util.List", lines)


            // TODO: BMR - this is temporarily added as WiP. Should we have a more targeted import (i.e. not import everything?)
            if (config.enableBasicAssertions) {
                addImport("org.hamcrest.Matchers.*", lines, true)
                //addImport("org.hamcrest.core.AnyOf.anyOf", lines, true)
                addImport("io.restassured.config.JsonConfig", lines)
                addImport("io.restassured.path.json.config.JsonPathConfig", lines)
                addImport("io.restassured.config.RedirectConfig.redirectConfig", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.NumberMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.StringMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.SubStringMatcher.*", lines, true)
            }

            if (config.expectationsActive) {
                addImport("org.evomaster.client.java.controller.expect.ExpectationHandler.expectationHandler", lines, true)
                addImport("org.evomaster.client.java.controller.expect.ExpectationHandler", lines)
                addImport("io.restassured.path.json.JsonPath", lines)
                addImport("java.util.Arrays", lines)
            }
        }

        if (format.isJavaScript()) {
            lines.add("const superagent = require(\"superagent\");")
            lines.add("const $jsImport = require(\"evomaster-client-js\").EMTestUtils;")
            if (controllerName != null) {
                lines.add("const $controllerName = require(\"${config.jsControllerPath}\");")
            }
        }

        if (format.isCsharp()) {
            addUsing("System", lines)
            addUsing("System.Text", lines)
            addUsing("System.Linq", lines)
            addUsing("Xunit", lines)
            addUsing("System.Net.Http", lines)
            addUsing("System.Net.Http.Headers", lines)
            addUsing("System.Threading.Tasks", lines)
            addUsing("Newtonsoft.Json", lines)
            addUsing("EvoMaster.Controller", lines)
        }

        lines.addEmpty(4)

        classDescriptionComment(solution, lines)

        if (format.isCsharp()) {

            //TODO configured from EMConfig. possibly with termination rather in the fixture class name
            lines.add("namespace EvoMasterTests${solution.termination.suffix}{")

            lines.indent()

            defineFixture(lines, controllerName)
        }

        if (format.isJavaOrKotlin() || format.isCsharp()) {
            defineClass(name, lines)
            lines.addEmpty()
        }
    }

    private fun classFields(lines: Lines, format: OutputFormat) {
        if(format.isCsharp()){
            lines.addEmpty()
            addStatement("private $fixtureClass $fixture", lines)
            lines.addEmpty()
        }
    }


    private fun staticVariables(controllerName: String?, lines: Lines) {

        if (config.outputFormat.isJava()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private static final SutHandler $controller = new $controllerName();")
                lines.add("private static String $baseUrlOfSut;")
            } else {
                lines.add("private static String $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\";")
            }
        } else if (config.outputFormat.isKotlin()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private val $controller : SutHandler = $controllerName()")
                lines.add("private lateinit var $baseUrlOfSut: String")
            } else {
                lines.add("private val $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\"")
            }
        } else if (config.outputFormat.isJavaScript()) {

            if (!config.blackBox || config.bbExperiments) {
                lines.add("const $controller = new $controllerName();")
                lines.add("let $baseUrlOfSut;")
            } else {
                lines.add("const $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\";")
            }
        } else if (config.outputFormat.isCsharp()) {
            lines.add("private static readonly HttpClient Client = new HttpClient ();")
        }

        if (config.expectationsActive) {
            if (config.outputFormat.isJavaOrKotlin()) {
                //TODO JS and C#
                if (activePartialOracles.any { it.value }) {
                    lines.add(
                        "/** [$expectationsMasterSwitch] - expectations master switch - is the variable that activates/deactivates expectations " +
                                "individual test cases"
                    )
                    lines.add(("* by default, expectations are turned off. The variable needs to be set to [true] to enable expectations"))
                    lines.add("*/")
                    if (config.outputFormat.isJava()) {
                        lines.add("private static boolean $expectationsMasterSwitch = false;")
                    } else if (config.outputFormat.isKotlin()) {
                        lines.add("private val $expectationsMasterSwitch = false")
                    }
                }
                partialOracles?.variableDeclaration(lines, config.outputFormat, activePartialOracles)
            }
        }
        //Note: ${config.expectationsActive} can be used to get the active setting, but the default
        // for generated code should be false.
    }

    private fun initClassMethod(lines: Lines) {

        // Note: for C#, this is done in the Fixture class

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@BeforeClass")
            format.isJUnit5() -> lines.add("@BeforeAll")
        }
        when {
            format.isJava() -> lines.add("public static void initClass()")
            format.isKotlin() -> {
                lines.add("@JvmStatic")
                lines.add("fun initClass()")
            }
            format.isJavaScript() -> lines.add("beforeAll( async () =>")
        }

        lines.block {
            if (!config.blackBox) {
                when {
                    config.outputFormat.isJavaScript() -> {
                        addStatement("await $controller.setupForGeneratedTest()", lines)
                        addStatement("baseUrlOfSut = await $controller.startSut()", lines)
                    }
                    config.outputFormat.isJavaOrKotlin() -> {
                        addStatement("$controller.setupForGeneratedTest()", lines)
                        addStatement("baseUrlOfSut = $controller.startSut()", lines)
                    }
                }

                when {
                    format.isJavaOrKotlin() -> addStatement("assertNotNull(baseUrlOfSut)", lines)
                    format.isJavaScript() -> addStatement("expect(baseUrlOfSut).toBeTruthy()", lines)
                }
            }

            if (format.isJavaOrKotlin()) {
                addStatement("RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()", lines)
                addStatement("RestAssured.useRelaxedHTTPSValidation()", lines)
                addStatement("RestAssured.urlEncodingEnabled = false", lines)
            }

            if (config.enableBasicAssertions && format.isJavaOrKotlin()) {
                lines.add("RestAssured.config = RestAssured.config()")
                lines.indented {
                    lines.add(".jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))")
                    lines.add(".redirect(redirectConfig().followRedirects(false))")
                }
                appendSemicolon(lines)
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun tearDownMethod(lines: Lines) {

        if (config.blackBox) {
            return
        }

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@AfterClass")
            format.isJUnit5() -> lines.add("@AfterAll")
        }
        when {
            format.isJava() -> lines.add("public static void tearDown()")
            format.isKotlin() -> {
                lines.add("@JvmStatic")
                lines.add("fun tearDown()")
            }
            format.isJavaScript() -> lines.add("afterAll( async () =>")
        }

        if (!format.isCsharp()) {
            lines.block {
                when {
                    format.isJavaScript() -> {
                        addStatement("await $controller.stopSut()", lines)
                    }
                    else -> {
                        addStatement("$controller.stopSut()", lines)
                    }
                }
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun initTestMethod(lines: Lines, name: TestSuiteFileName) {

        if (config.blackBox) {
            return
        }

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@Before")
            format.isJUnit5() -> lines.add("@BeforeEach")
        }
        when {
            format.isJava() -> lines.add("public void initTest()")
            format.isKotlin() -> {
                lines.add("fun initTest()")
            }
            format.isJavaScript() -> lines.add("beforeEach(async () => ")
            //for C# we are actually setting up the constructor for the test class
            format.isCsharp() -> lines.add("public ${name.getClassName()} ($fixtureClass fixture)")
        }


        lines.block {
            if (format.isJavaScript()) {
                addStatement("await $controller.resetStateOfSUT()", lines)
            } else if (format.isJavaOrKotlin()) {
                addStatement("$controller.resetStateOfSUT()", lines)
            } else if (format.isCsharp()) {
                addStatement("$fixture = fixture", lines)
                addStatement("$fixture.controller.ResetStateOfSut()", lines)
            }
        }


        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun beforeAfterMethods(
        controllerName: String?,
        lines: Lines,
        format: OutputFormat,
        testSuiteFileName: TestSuiteFileName
    ) {

        lines.addEmpty()

        val staticInit = {
            staticVariables(controllerName, lines)

            if (!format.isCsharp()) {
                lines.addEmpty(2)
                initClassMethod(lines)
                lines.addEmpty(2)

                tearDownMethod(lines)
            }
        }

        if (config.outputFormat.isKotlin()) {
            lines.add("companion object")
            lines.block(1, staticInit)
        } else {
            staticInit.invoke()
        }
        lines.addEmpty(2)

        initTestMethod(lines, testSuiteFileName)
        lines.addEmpty(2)
    }


    private fun footer(lines: Lines) {
        if (config.outputFormat.isJavaOrKotlin() || config.outputFormat.isCsharp()) {
            //due to opening of class
            lines.addEmpty(2)
            lines.add("}")
        }

        if (config.outputFormat.isCsharp()) {
            //due to opening of namespace
            lines.deindent()
            lines.addEmpty(2)
            lines.add("}")
        }
    }

    private fun defineClass(name: TestSuiteFileName, lines: Lines) {

        lines.addEmpty()

        val format = config.outputFormat

        when {
            format.isJava() -> lines.append("public ")
            format.isKotlin() -> lines.append("internal ")
            format.isCsharp() -> lines.append("public ")
        }

        if (!format.isCsharp())
            lines.append("class ${name.getClassName()} {")
        else
            lines.append("class ${name.getClassName()} : IClassFixture<$fixtureClass> {")
    }

    private fun addImport(klass: String, lines: Lines, static: Boolean = false) {

        //Kotlin for example does not use "static" in the imports
        val s = if (static && config.outputFormat.isJava()) "static" else ""

        addStatement("import $s $klass", lines)
    }

    private fun addUsing(library: String, lines: Lines, static: Boolean = false) {

        val s = if (static) "static" else ""

        addStatement("using $s $library", lines)
    }

    private fun addStatement(statement: String, lines: Lines) {
        lines.add(statement)
        appendSemicolon(lines)
    }

    private fun appendSemicolon(lines: Lines) {
        if (config.outputFormat.let { it.isJava() || it.isJavaScript() || it.isCsharp() }) {
            lines.append(";")
        }
    }



    /**
     *  FIXME replace with direct injection
     */
    @Deprecated("replace with direct injection")
    fun getPartialOracles(): PartialOracles {
        return partialOracles
    }


}
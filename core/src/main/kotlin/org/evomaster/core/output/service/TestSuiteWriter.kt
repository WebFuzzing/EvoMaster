package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.output.*
import org.evomaster.core.output.TestWriterUtils.getWireMockVariableName
import org.evomaster.core.output.TestWriterUtils.handleDefaultStubForAsJavaOrKotlin
import org.evomaster.core.output.dto.DtoWriter
import org.evomaster.core.output.naming.NumberedTestCaseNamingStrategy
import org.evomaster.core.output.naming.TestCaseNamingStrategyFactory
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.BlackBoxUtils
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.security.service.HttpCallbackVerifier
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.test.utils.EMTestUtils
import org.evomaster.test.utils.SeleniumEMUtils
import org.evomaster.test.utils.js.JsLoader
import org.evomaster.test.utils.py.PyLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime


/**
 * Given a Solution as input, convert it to a string representation of
 * the tests that can be written to file and be compiled
 */
class TestSuiteWriter {

    companion object {

        const val jsImport = "EM"

        /**
         * variable name of Sut handler
         */
        const val controller = "controller"
        const val driver = "driver"

        private const val pythonUtilsFilenameNoExtension = "em_test_utils"
        const val pythonUtilsFilename = "$pythonUtilsFilenameNoExtension.py"
        const val javascriptUtilsFilename = "EMTestUtils.js"

        private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)

        private const val baseUrlOfSut = "baseUrlOfSut"
        private const val fixtureClass = "ControllerFixture"
        private const val fixture = "_fixture"
        private const val browser = "browser"
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var searchTimeController: SearchTimeController

    @Inject
    private lateinit var testCaseWriter: TestCaseWriter

    @Inject
    private lateinit var partialOracles: PartialOracles

    @Inject(optional = true)
    private lateinit var sampler: Sampler<*>

    @Inject(optional = true)
    private lateinit var remoteController: RemoteController

    @Inject
    private lateinit var externalServiceHandler: HttpWsExternalServiceHandler

    @Inject
    private lateinit var httpCallbackVerifier: HttpCallbackVerifier


    fun writeTests(testSuiteCode: TestSuiteCode){
        saveToDisk(testSuiteCode.code, Paths.get(config.outputFolder, testSuiteCode.testSuitePath))
    }

    fun writeTests(
        solution: Solution<*>,
        controllerName: String?,
        controllerInput: String?,
        snapshotTimestamp: String = ""
    ) {

        val name = solution.getFileName()
        val content = convertToCompilableTestCode(solution, name, snapshotTimestamp, controllerName, controllerInput)
        saveToDisk(content.code, getTestSuitePath(name, config))
    }

    /**
     * write tests during seeding
     */
    fun writeTestsDuringSeeding(solution: Solution<*>,
                                controllerName: String?,
                                controllerInput: String?,
                                snapshotTimestamp: String = ""){

        if (!config.exportTestCasesDuringSeeding || solution.individualsDuringSeeding.isEmpty()) return

        val solutionDuringSeeding = solution.extractSolutionDuringSeeding()
        writeTests(solutionDuringSeeding, controllerName, controllerInput, snapshotTimestamp)

    }


    fun convertToCompilableTestCode(
        solution: Solution<*>,
        testSuiteFileName: TestSuiteFileName,
        timestamp: String = "",
        controllerName: String?,
        controllerInput: String?
    ): TestSuiteCode {

        val lines = Lines(config.outputFormat)
        val testSuiteOrganizer = TestSuiteOrganizer()
        val namingStrategy = TestCaseNamingStrategyFactory(config).create(solution)

        header(solution, testSuiteFileName, lines, timestamp, controllerName)

        if (!config.outputFormat.isJavaScript()) {
            /*
                In Java/Kotlin/C# the tests are inside a class, but not in JS
             */
            lines.indent()
        }

        classFields(lines, config.outputFormat)

        beforeAfterMethods(solution, controllerName, controllerInput, lines, config.outputFormat, testSuiteFileName)

        //catch any sorting problems (see NPE is SortingHelper on Trello)
        val tests = try {
            // TODO skip to sort RPC for the moment
                testSuiteOrganizer.sortTests(solution, namingStrategy, config.testCaseSortingStrategy)
        } catch (ex: Exception) {
            log.warn(
                "A failure has occurred with the test sorting. Reverting to default settings. \n"
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
            )
            // fallback to numbered naming strategy upon failure
            NumberedTestCaseNamingStrategy(solution).getTestCases()
        }

        val testSuitePath = getTestSuitePath(testSuiteFileName, config)

        val tc = tests.mapNotNull { test ->

            // catch writing problems on an individual test case basis
            val testLines = try {
                if (config.outputFormat.isCsharp())
                    testCaseWriter.convertToCompilableTestCode(test, "$fixture.$baseUrlOfSut", testSuitePath)
                else
                    testCaseWriter.convertToCompilableTestCode(test, baseUrlOfSut, testSuitePath)
            } catch (ex: Exception) {
                log.warn(
                    "A failure has occurred in writing test ${test.name}. \n "
                            + "Exception: ${ex.localizedMessage} \n"
                            + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
                )
                assert(false) // in our tests, this should not happen... but should not crash in production
                Lines(config.outputFormat)
            }

            if(testLines.isEmpty()){
                null
            } else {
                lines.addEmpty(2)
                val start = lines.nextLineNumber()
                lines.add(testLines)
                val end = lines.nextLineNumber() - 1
                TestCaseCode(test.name,test.test,testLines.toString(), start, end)
            }
        }

        if (!config.outputFormat.isJavaScript()) {
            lines.deindent()
        }

        footer(lines)

        // additional handling on generated tests
        testCaseWriter.additionalTestHandling(tests)

        return TestSuiteCode(
            solution.getFileName().name,
            solution.getFileRelativePath(config.outputFormat),
            lines.toString(),
            tc
        )
    }

    // TODO: take DTO extraction and writing to a different class
    fun writeDtos(solutionFilename: String) {
        val testSuiteFileName = TestSuiteFileName(solutionFilename)
        val testSuitePath = getTestSuitePath(testSuiteFileName, config).parent
        val restSampler = sampler as AbstractRestSampler
        DtoWriter().write(testSuitePath, testSuiteFileName.getPackage(), config.outputFormat, restSampler.getActionDefinitions())
    }

    private fun handleResetDatabaseInput(solution: Solution<*>): String {
        if (!config.outputFormat.isJavaOrKotlin())
            throw IllegalStateException("DO NOT SUPPORT resetDatabased for " + config.outputFormat)

        val accessedTable = mutableSetOf<String>()
        solution.individuals.forEach { e ->
            //TODO will need to be refactored when supporting Web Frontend
            if (e.individual is ApiWsIndividual) {
                accessedTable.addAll(e.individual.getInsertTableNames())
            }
            e.fitness.databaseExecutions.values.forEach { de ->
                accessedTable.addAll(de.insertedData.map { it.key })
                accessedTable.addAll(de.updatedData.map { it.key })
                accessedTable.addAll(de.deletedData)
            }
        }
        val all = sampler.extractFkTables(accessedTable)

        //if (all.isEmpty()) return "null"

        val tableNamesInSchema = remoteController.getCachedSutInfo()?.sqlSchemaDto?.tables?.map { it.name }?.toSet()
            ?: setOf()

        val missingTables = all.filter { x ->  tableNamesInSchema.none { y -> y.equals(x,true) } }.sorted()
        if(missingTables.isNotEmpty()){
            /*
                Weird case... but actually seen it in familie-ba-sak, regarding table "task", which is in the migration
                files (V9) but then somehow doesn't show up in the database...
                TODO should investigate what the heck is happening there
             */
            log.warn("Some SQL commands have referred to tables that do not seem to appear in the database schema: " +
                    "${missingTables.joinToString(", ")}")
        }

        val input = if(all.isEmpty()) ""
            else all.filter { x -> tableNamesInSchema.any{y -> y.equals(x,true)} }.sorted().joinToString(",") { "\"$it\"" }

        return when {
            config.outputFormat.isJava() -> "Arrays.asList($input)"
            config.outputFormat.isKotlin() -> "listOf($input)"
            else -> throw IllegalStateException("DO NOT SUPPORT resetDatabased for " + config.outputFormat)
        }
    }


    private fun saveToDisk(
        testFileContent: String,
        path: Path
    ) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }

    private fun getTestSuitePath(testSuiteFileName: TestSuiteFileName, config: EMConfig) : Path{
        return Paths.get(config.outputFolder, testSuiteFileName.getAsPath(config.outputFormat));
    }

    private fun removeFromDisk(
        config: EMConfig,
        testSuiteFileName: TestSuiteFileName
    ) {

        val path = Paths.get(config.outputFolder, testSuiteFileName.getAsPath(config.outputFormat))

        Files.deleteIfExists(path)
    }

    private fun classDescriptionEmptyLine(lines: Lines) {
        if (config.outputFormat.isJava()) {
            lines.add(" * <br>")
        } else {
            lines.addBlockCommentLine(" ")
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

    private fun classDescriptionComment(solution: Solution<*>, lines: Lines, timestamp: String = "") {
        lines.startCommentBlock()
        lines.addBlockCommentLine(
            " This file was automatically generated by EvoMaster on ${
                escapeDocs(
                    ZonedDateTime.now().toString()
                )
            }"
        )
        classDescriptionEmptyLine(lines)

        if (timestamp != "") {
            lines.addBlockCommentLine(" ************************************ WARNING ************************************ ")
            lines.addBlockCommentLine(" * This is an snapshot of the generated tests after $timestamp seconds elapsed. *")
            lines.addBlockCommentLine(" * The execution of Evomaster has not finished. *")
            lines.addBlockCommentLine(" ********************************************************************************* ")
        }

        lines.addBlockCommentLine(" The generated test suite contains ${solution.individuals.size} tests")
        classDescriptionEmptyLine(lines)
        lines.addBlockCommentLine(" Covered targets: ${solution.overall.coveredTargets()}")
        classDescriptionEmptyLine(lines)
        lines.addBlockCommentLine(" Used time: ${searchTimeController.getElapsedTime()}")
        classDescriptionEmptyLine(lines)
        lines.addBlockCommentLine(" Needed budget for current results: ${searchTimeController.neededBudget()}")
        classDescriptionEmptyLine(lines)
        lines.addBlockCommentLine(" ${solution.termination.comment}")
        lines.endCommentBlock()

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
        timestamp: String = "",
        controllerName: String?
    ) {

        val format = config.outputFormat

        if(format.isPython()){
            lines.add("#!/usr/bin/env python")
            lines.addEmpty(1)
        }

        lines.addMultiLineComment(listOf(
            "LICENSE DISCLAIMER",
            "This file has been generated by EvoMaster.",
            "The content of this file is not subject to the license of EvoMaster itself, i.e., LGPL.",
            "This generated software (i.e., the test suite in this file) can be freely used, modified, ",
            "and distributed as you see fit without any restrictions."
        ))

        if (name.hasPackage() && format.isJavaOrKotlin()) {
            addStatement("package ${name.getPackage()}", lines)
            lines.addEmpty(2)
        }

        if (format.isJUnit5()) {
            addImport("org.junit.jupiter.api.AfterAll", lines)
            addImport("org.junit.jupiter.api.BeforeAll", lines)
            addImport("org.junit.jupiter.api.BeforeEach", lines)
            addImport("org.junit.jupiter.api.Test", lines)
            addImport("org.junit.jupiter.api.Timeout", lines)
            addImport("org.junit.jupiter.api.Assertions.*", lines, true)
            if (config.useTestMethodOrder) {
                addImport("org.junit.jupiter.api.MethodOrderer", lines)
                addImport("org.junit.jupiter.api.TestMethodOrder", lines)
            }
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
            addImport("java.util.Arrays", lines)
            if (config.dtoForRequestPayload) {
                val pkgPrefix = if (name.getPackage().isNotEmpty()) "${name.getPackage()}." else ""
                addImport("${pkgPrefix}dto.*", lines)
                addImport("java.util.ArrayList", lines)
            }
        }

        if (format.isJavaOrKotlin()) {

            addImport("java.util.List", lines)
            addImport(EMTestUtils::class.java.name +".*", lines, true)
            addImport("org.evomaster.client.java.controller.SutHandler", lines)

            if (useRestAssured()) {
                addImport("io.restassured.RestAssured", lines)
                addImport("io.restassured.RestAssured.given", lines, true)
                addImport("io.restassured.response.ValidatableResponse", lines)
            }

            if ((config.isEnabledExternalServiceMocking() && solution.needWireMockServers())
                || (config.ssrf && solution.hasSsrfFaults())) {
                addImport("com.github.tomakehurst.wiremock.client.WireMock.*", lines, true)
                addImport("com.github.tomakehurst.wiremock.WireMockServer", lines)
                addImport("com.github.tomakehurst.wiremock.core.WireMockConfiguration", lines)
                addImport(
                    "com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer",
                    lines
                )
            }

            if(config.isEnabledExternalServiceMocking() && solution.needsHostnameReplacement() ){
                addImport("com.alibaba.dcm.DnsCacheManipulator", lines)
            }


            if(solution.hasAnySqlAction()) {
                addImport("org.evomaster.client.java.sql.dsl.SqlDsl.sql", lines, true)
                addImport("org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto", lines)
                addImport(InsertionDto::class.qualifiedName!!, lines)
            }

            if(solution.hasAnyMongoAction()) {
                addImport("org.evomaster.client.java.controller.mongo.dsl.MongoDsl.mongo", lines, true)
                addImport("org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto", lines)
                addImport(MongoInsertionDto::class.qualifiedName!!, lines)
            }

            if (config.enableBasicAssertions) {

                if(useHamcrest()) {
                    addImport("org.hamcrest.Matchers.*", lines, true)
                }

                //addImport("org.hamcrest.core.AnyOf.anyOf", lines, true)
                if (useRestAssured()) {
                    addImport("io.restassured.config.JsonConfig", lines)
                    addImport("io.restassured.path.json.config.JsonPathConfig", lines)
                    addImport("io.restassured.config.RedirectConfig.redirectConfig", lines, true)
                }

                addImport("org.evomaster.client.java.controller.contentMatchers.NumberMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.StringMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.SubStringMatcher.*", lines, true)
            }

            if (config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                addImport("org.testcontainers.containers.BrowserWebDriverContainer", lines)
                addImport("org.openqa.selenium.chrome.ChromeOptions", lines)
                addImport("org.openqa.selenium.remote.RemoteWebDriver", lines)
                addImport(SeleniumEMUtils::class.java.name + ".*", lines, true)
            }
        }

        if (format.isJavaScript()) {
            lines.add("const superagent = require(\"superagent\");")

            val jsUtils = JsLoader::class.java.getResource("/$javascriptUtilsFilename").readText()
            saveToDisk(jsUtils, Paths.get(config.outputFolder, javascriptUtilsFilename))
            lines.add("const $jsImport = require(\"./$javascriptUtilsFilename\");")

            if (controllerName != null) {
                lines.add("const $controllerName = require(\"${config.jsControllerPath}\");")
            }
            if (config.testTimeout > 0) {
                lines.add("jest.setTimeout(${config.testTimeout * 1000});")
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

        if (format.isPython()) {
            lines.add("import json")
            lines.add("import unittest")
            lines.add("import requests")
            if (config.testTimeout > 0) {
                //see https://stackoverflow.com/questions/32309683/timeout-decorator-is-it-possible-to-disable-or-make-it-work-on-windows
                lines.add("import os")
                lines.add("if os.name == 'nt':")
                lines.indented {
                    lines.add("class timeout_decorator:")
                    lines.indented {
                        lines.add("@staticmethod")
                        lines.add("def timeout(*args, **kwargs):")
                        lines.indented {
                            lines.add("return lambda f: f # return a no-op decorator")
                        }
                    }
                }
                lines.add("else:")
                lines.indented {
                    lines.add("import timeout_decorator")
                }
            }
            lines.add("from $pythonUtilsFilenameNoExtension import *")
            val pythonUtils = PyLoader::class.java.getResource("/$pythonUtilsFilename").readText()
            saveToDisk(pythonUtils, Paths.get(config.outputFolder, pythonUtilsFilename))
        }

        when {
            format.isPython() -> lines.addEmpty(2)
            else -> lines.addEmpty(4)
        }

        classDescriptionComment(solution, lines, timestamp)

        if (format.isCsharp()) {

            //TODO configured from EMConfig. possibly with termination rather in the fixture class name
            lines.add("namespace EvoMasterTests${solution.termination.suffix}{")

            lines.indent()

            defineFixture(lines, controllerName)
        }

        if (format.isJavaOrKotlin() || format.isCsharp() || format.isPython()) {
            defineClass(name, lines)
            lines.addEmpty()
        }
    }

    private fun classFields(lines: Lines, format: OutputFormat) {
        if (format.isCsharp()) {
            lines.addEmpty()
            addStatement("private $fixtureClass $fixture", lines)
            lines.addEmpty()
        }
    }

    private fun getJaCoCoInit(): String {
        if (config.jaCoCoAgentLocation.isNotBlank()) {
            val agent = config.jaCoCoAgentLocation.replace("\\", "\\\\")
            val cli = config.jaCoCoCliLocation.replace("\\", "\\\\")
            val exec = config.jaCoCoOutputFile.replace("\\", "\\\\")
            val port = config.jaCoCoPort
            return ".setJaCoCo(\"$agent\",\"$cli\",\"${exec}\",$port)"
        }
        return ""
    }

    private fun getJavaCommand(): String {
        if (config.javaCommand != "java") {
            val java = config.javaCommand.replace("\\", "\\\\")
            return ".setJavaCommand(\"$java\")"
        }
        return ""
    }

    private fun staticVariables(
        controllerName: String?,
        controllerInput: String?,
        lines: Lines,
        solution: Solution<*>
    ) {

        val wireMockServers = getActiveWireMockServers()

        val executable = if (controllerInput.isNullOrBlank()) ""
        else "\"$controllerInput\"".replace("\\", "\\\\")

        if (config.outputFormat.isJava()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private static final SutHandler $controller = new $controllerName($executable)")
                lines.append(getJaCoCoInit())
                lines.append(getJavaCommand())
                lines.append(";")
                lines.add("private static String $baseUrlOfSut;")
            } else {
                lines.add("private static String $baseUrlOfSut = \"${BlackBoxUtils.targetUrl(config, sampler)}\";")
            }
            if (config.isEnabledExternalServiceMocking() && solution.needWireMockServers()) {
                wireMockServers
                    .forEach { externalService ->
                        addStatement("private static WireMockServer ${getWireMockVariableName(externalService)}", lines)
                    }
            }

            if (config.ssrf && solution.hasSsrfFaults()) {
                httpCallbackVerifier.getActionVerifierMappings().forEach { v ->
                    addStatement("private static WireMockServer ${v.getVerifierName()}", lines)
                }
            }

            if(config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                lines.add("private static final BrowserWebDriverContainer $browser = new BrowserWebDriverContainer()")
                lines.indented {
                    lines.add(".withCapabilities(ChromeOptions())")
                    lines.add(".withAccessToHost(true)")
                    lines.append(";")
                }
                lines.add("private static RemoteWebDriver $driver;")
            }
        } else if (config.outputFormat.isKotlin()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private val $controller : SutHandler = $controllerName($executable)")
                lines.append(getJaCoCoInit())
                lines.append(getJavaCommand())
                lines.add("private lateinit var $baseUrlOfSut: String")
            } else {
                lines.add("private val $baseUrlOfSut = \"${BlackBoxUtils.targetUrl(config, sampler)}\"")
            }
            if (config.isEnabledExternalServiceMocking() && solution.needWireMockServers()) {
                wireMockServers
                    .forEach { action ->
                        addStatement("private lateinit var ${getWireMockVariableName(action)}: WireMockServer", lines)
                    }
            }

            if (config.ssrf && solution.hasSsrfFaults()) {
                httpCallbackVerifier.getActionVerifierMappings().forEach { v ->
                    addStatement("private lateinit var ${v.getVerifierName()}: WireMockServer", lines)
                }
            }

            if(config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                lines.add("private val $browser : BrowserWebDriverContainer<*> =  BrowserWebDriverContainer()")
                lines.indented {
                    lines.add(".withCapabilities(ChromeOptions())")
                    lines.add(".withAccessToHost(true)")
                }
                lines.add("private lateinit var $driver : RemoteWebDriver")
            }

        } else if (config.outputFormat.isJavaScript()) {

            if (!config.blackBox || config.bbExperiments) {
                lines.add("const $controller = new $controllerName();")
                lines.add("let $baseUrlOfSut;")
            } else {
                lines.add("const $baseUrlOfSut = \"${BlackBoxUtils.targetUrl(config, sampler)}\";")
            }
        } else if (config.outputFormat.isCsharp()) {
            lines.add("private static readonly HttpClient Client = new HttpClient ();")
        } else if (config.outputFormat.isPython()) {
            if (config.blackBox) {
                lines.add("$baseUrlOfSut = \"${BlackBoxUtils.targetUrl(config, sampler)}\"")
            }
        }

        testCaseWriter.addExtraStaticVariables(lines)

//        if (config.expectationsActive) {
//            if (config.outputFormat.isJavaOrKotlin()) {
//                //TODO JS and C#
//                if (activePartialOracles.any { it.value }) {
//                    lines.add(
//                        "/** [$expectationsMasterSwitch] - expectations master switch - is the variable that activates/deactivates expectations " +
//                                "individual test cases"
//                    )
//                    lines.add(("* by default, expectations are turned off. The variable needs to be set to [true] to enable expectations"))
//                    lines.add("*/")
//                    if (config.outputFormat.isJava()) {
//                        lines.add("private static boolean $expectationsMasterSwitch = false;")
//                    } else if (config.outputFormat.isKotlin()) {
//                        lines.add("private val $expectationsMasterSwitch = false")
//                    }
//                }
//                partialOracles?.variableDeclaration(lines, config.outputFormat, activePartialOracles)
//            }
//        }
        //Note: ${config.expectationsActive} can be used to get the active setting, but the default
        // for generated code should be false.
    }

    private fun initClassMethod(solution: Solution<*>, lines: Lines) {

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
                        addStatement("$baseUrlOfSut = await $controller.startSut()", lines)
                    }
                    config.outputFormat.isJavaOrKotlin() -> {
                        addStatement("$controller.setupForGeneratedTest()", lines)
                        addStatement("$baseUrlOfSut = $controller.startSut()", lines)
                        //registerOrExecuteInitSqlCommands
                        addStatement("$controller.registerOrExecuteInitSqlCommandsIfNeeded()", lines)

                        if(config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                            val infoDto = remoteController.getCachedSutInfo()!!
                            addStatement("$baseUrlOfSut = validateAndGetUrlOfStartingPageForDocker($baseUrlOfSut,\"${infoDto.webProblem.urlPathOfStartingPage}\", true)", lines)
                        }
                        /*
                            now only support white-box
                            TODO remove this later if we do not use test generation with driver
                         */
                        if (config.problemType == EMConfig.ProblemType.RPC) {
                            addStatement("$controller.extractRPCSchema()", lines)
                        }
                    }
                }

                when {
                    format.isJavaOrKotlin() -> addStatement("assertNotNull(baseUrlOfSut)", lines)
                    format.isJavaScript() -> addStatement("expect(baseUrlOfSut).toBeTruthy()", lines)
                }
            }

            if(config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                if(format.isJavaOrKotlin()){
                    addStatement("$browser.start()", lines)

                    if(format.isJava()) {
                        addStatement("$driver = new RemoteWebDriver($browser.seleniumAddress, new ChromeOptions())", lines)
                    }
                    if(format.isKotlin()){
                        addStatement("$driver = RemoteWebDriver($browser.seleniumAddress, ChromeOptions())", lines)
                    }
                }
            }

            if (config.problemType == EMConfig.ProblemType.REST || config.problemType == EMConfig.ProblemType.GRAPHQL) {
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
                    lines.appendSemicolon()
                }
            }

            val wireMockServers = getActiveWireMockServers()
            if (config.isEnabledExternalServiceMocking() && wireMockServers.isNotEmpty() && solution.needWireMockServers()) {
                if (format.isJavaOrKotlin()) {
                    wireMockServers
                        .forEach { externalService ->
                            val address = externalService.getWireMockAddress()
                            val name = getWireMockVariableName(externalService)

                            if (format.isJava()) {
                                lines.add("$name = new WireMockServer(new WireMockConfiguration()")
                            }

                            if (format.isKotlin()) {
                                lines.add("$name = WireMockServer(WireMockConfiguration()")
                            }

                            lines.indented {
                                lines.add(".bindAddress(\"$address\")")
                                if (externalService.isHttps()) {
                                    lines.add(".httpsPort(${externalService.getWireMockPort()})")
                                } else {
                                    lines.add(".port(${externalService.getWireMockPort()})")
                                }
                                if (format.isJava()) {
                                    addStatement(".extensions(new ResponseTemplateTransformer(false)))", lines)
                                }

                                if (format.isKotlin()) {
                                    addStatement(".extensions(ResponseTemplateTransformer(false)))", lines)
                                }
                            }
                            addStatement("${name}.start()", lines)
                        }
                } else {
                    log.warn("In mocking of external services, we do NOT support for other format ($format) except JavaOrKotlin")
                }
            }

            if (config.ssrf && solution.hasSsrfFaults()) {
                httpCallbackVerifier.getActionVerifierMappings().forEach { v ->
                    if (format.isJava()) {
                        lines.add("${v.getVerifierName()} = new WireMockServer(new WireMockConfiguration()")
                    }
                    if (format.isKotlin()) {
                        lines.add("${v.getVerifierName()} = WireMockServer(WireMockConfiguration()")
                    }

                    lines.indented {
                        lines.add(".port(${v.port})")
                        if (format.isJava()) {
                            addStatement(".extensions(new ResponseTemplateTransformer(false)))", lines)
                        }
                        if (format.isKotlin()) {
                            addStatement(".extensions(ResponseTemplateTransformer(false)))", lines)
                        }
                    }
                    addStatement("${v.getVerifierName()}.start()", lines)
                    addStatement("assertNotNull(${v.getVerifierName()})", lines)

                    lines.addEmpty(1)
                }
            }

            testCaseWriter.addExtraInitStatement(lines)
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun tearDownMethod(lines: Lines, solution: Solution<*>) {

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
                        if (format.isJavaOrKotlin()
                            && config.isEnabledExternalServiceMocking()
                        ) {
                            if(solution.needWireMockServers()) {
                                getActiveWireMockServers().forEach { action ->
                                        addStatement("${getWireMockVariableName(action)}.stop()", lines)
                                }
                            }
                            if(solution.needsHostnameReplacement()) {
                                addStatement("DnsCacheManipulator.clearDnsCache()", lines)
                            }
                        }
                        if(config.problemType == EMConfig.ProblemType.WEBFRONTEND){
                            addStatement("$browser.stop()", lines)
                        }
                    }
                }
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun initTestMethod(solution: Solution<*>, lines: Lines, name: TestSuiteFileName) {

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
                //TODO add resetDatabase
                addStatement("await $controller.resetStateOfSUT()", lines)
            } else if (format.isJavaOrKotlin()) {
                if (config.employSmartDbClean == true) {
                    addStatement("$controller.resetDatabase(${handleResetDatabaseInput(solution)})", lines)
                }
                addStatement("$controller.resetStateOfSUT()", lines)

                if (format.isJavaOrKotlin() && config.isEnabledExternalServiceMocking() && solution.needWireMockServers()) {
                    getActiveWireMockServers()
                        .forEach { es ->
                            addStatement("${getWireMockVariableName(es)}.resetAll()", lines)
                            // set the default responses for all wm
                            handleDefaultStubForAsJavaOrKotlin(lines, es, format)
                            lines.appendSemicolon()
                        }
                }

                if (config.enableCustomizedMethodForMockObjectHandling && testCaseWriter is RPCTestCaseWriter){
                    lines.add((testCaseWriter as RPCTestCaseWriter).resetExternalServicesWithCustomizedMethod())
                    lines.add((testCaseWriter as RPCTestCaseWriter).resetMockDatabaseObjectWithCustomizedMethod())
                }


            } else if (format.isCsharp()) {
                addStatement("$fixture = fixture", lines)
                //TODO add resetDatabase
                addStatement("$fixture.controller.ResetStateOfSut()", lines)
            }

            if (format.isJavaOrKotlin()
                && config.isEnabledExternalServiceMocking()
                && solution.needsHostnameReplacement()
            ) {
                addStatement("DnsCacheManipulator.clearDnsCache()", lines)
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun beforeAfterMethods(
        solution: Solution<*>,
        controllerName: String?,
        controllerInput: String?,
        lines: Lines,
        format: OutputFormat,
        testSuiteFileName: TestSuiteFileName
    ) {

        lines.addEmpty()

        val staticInit = {
            staticVariables(controllerName, controllerInput, lines, solution)

            if (!format.isCsharp()) {
                lines.addEmpty(2)
                initClassMethod(solution, lines)
                lines.addEmpty(2)

                tearDownMethod(lines, solution)
            }
        }

        if (config.outputFormat.isKotlin()) {
            lines.add("companion object")
            lines.block(1, staticInit)
        } else {
            staticInit.invoke()
        }
        lines.addEmpty(2)

        initTestMethod(solution, lines, testSuiteFileName)
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

        if (config.outputFormat.isPython()) {
            lines.addEmpty(2)
            lines.add("if __name__ == '__main__':")
            lines.indent()
            lines.add("unittest.main()")
        }
    }

    private fun defineClass(name: TestSuiteFileName, lines: Lines) {

        lines.addEmpty()

        val format = config.outputFormat

        if (format.isKotlin() && format.isJUnit5() && config.useTestMethodOrder) {
            lines.add("@TestMethodOrder(MethodOrderer.MethodName::class)")
        }

        when {
            format.isJava() -> lines.append("public ")
            format.isKotlin() -> lines.append("internal ")
            format.isCsharp() -> lines.append("public ")
        }

        when {
            format.isCsharp() -> lines.append("class ${name.getClassName()} : IClassFixture<$fixtureClass> {")
            format.isPython() -> lines.append("class ${name.getClassName()}(unittest.TestCase):")
            else -> lines.append("class ${name.getClassName()} {")
        }
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
        lines.addStatement(statement)
    }


    /**
     *  FIXME replace with direct injection
     */
    @Deprecated("replace with direct injection")
    fun getPartialOracles(): PartialOracles {
        return partialOracles
    }


    private fun useRestAssured() = config.problemType == EMConfig.ProblemType.REST || config.problemType == EMConfig.ProblemType.GRAPHQL

    //TODO better check. need to review use in RPC and GraphQL
    private fun useHamcrest() = config.problemType != EMConfig.ProblemType.WEBFRONTEND

    /**
     * Returns a distinct List of [HttpExternalServiceAction] from the given solution
     */
    private fun getWireMockServerActions(solution: Solution<*>): List<HttpWsExternalService> {
        return solution.individuals
            .map{ it.individual}
            .filterIsInstance<RestIndividual>()
            .flatMap {
                it.seeExternalServiceActions()
                    .filterIsInstance<HttpExternalServiceAction>()
                    .filter { it.active }
                    .map { it.externalService }
                    //.plus( it.fitness.getViewEmployedDefaultWM())
            }
            .distinctBy { it.getSignature() }.toList()
    }

    private fun getActiveWireMockServers(): List<HttpWsExternalService> {
        return externalServiceHandler.getExternalServices()
            .filter { it.value.getIP() != ExternalServiceSharedUtils.DEFAULT_WM_LOCAL_IP }
            .filter { it.value.isActive() }
            .map { it.value }
            .distinctBy { it.getSignature() }
            .toList()
    }

}

package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestWriterUtils.getWireMockVariableName
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


abstract class TestCaseWriter {

    @Inject
    protected lateinit var config: EMConfig


    /**
     * In the tests, we might need to generate new variables.
     * We must guarantee that no 2 variables have the same name.
     * Easiest approach is to just use a counter that is incremented
     * at each new generated variable
     */
    protected var counter = 0

    protected val format: OutputFormat
        get() {
            return config.outputFormat
        }


    companion object {
        private val log = LoggerFactory.getLogger(TestCaseWriter::class.java)
    }


    /**
     * save content to the same folder where [testResourcePath] is with a file name [fileName]
     */
    protected fun saveTextToDisk(text: String, testResourcePath: Path, fileName: String){
        if (!Files.exists(testResourcePath))
            Files.createDirectories(testResourcePath)

        val textToFile = Paths.get(testResourcePath.toFile().path, fileName)
        Files.deleteIfExists(textToFile)
        Files.createFile(textToFile)

        textToFile.toFile().appendText(text)
    }


    private fun addTestComments(lines: Lines, test: TestCase){

        lines.startCommentBlock()
        //maybe here we could have general messages... if needed

        addTestCommentBlock(lines, test)
        lines.endCommentBlock()
    }

    protected abstract fun addTestCommentBlock(lines: Lines, test: TestCase)

    fun convertToCompilableTestCode(
            test: TestCase,
            baseUrlOfSut: String,
            testSuitePath: Path? = null
    ): Lines {

        counter = 0

        val lines = Lines(config.outputFormat)

//        if (config.testSuiteSplitType == EMConfig.TestSuiteSplitType.FAULTS
//            && test.test.getClusters().size != 0
//        ) {
//            clusterComment(lines, test)
//        }

        if(config.addTestComments) {
            addTestComments(lines, test)
        }

        if (format.isJUnit()) {
            if (config.testTimeout <= 0) {
                lines.add("@Test")
            } else {
                if (format.isJUnit4()) {
                    lines.add("@Test(timeout = ${config.testTimeout * 1000})")
                } else if (format.isJUnit5()) {
                    lines.add("@Test @Timeout(${config.testTimeout})")
                }
            }
        }

        if (format.isPython() && config.testTimeout > 0) {
            lines.add("@timeout_decorator.timeout(${config.testTimeout})")
        }

        //TODO: check xUnit instead
        if (format.isCsharp()) {
            lines.add("[Fact]")
        }

        when {
            format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
            format.isKotlin() -> lines.add("fun ${test.name}()  {")
            format.isJavaScript() -> lines.add("test(\"${test.name}\", async () => {")
            format.isCsharp() -> lines.add("public async Task ${test.name}() {")
            format.isPython() -> lines.add("def ${test.name}(self):")
        }


        lines.indented {
            val ind = test.test
            val insertionVars = mutableListOf<Pair<String, String>>()
            // FIXME: HostnameResolutionActions can be a separately, for now it's under
            //  handleFieldDeclarations.
            handleTestInitialization(lines, baseUrlOfSut, ind, insertionVars, test.name)
            handleActionCalls(lines, baseUrlOfSut, ind, insertionVars, testCaseName = test.name, testSuitePath)
        }


        if (!format.isPython()) {
            lines.add("}")
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
        return lines
    }

    fun handleHostnameResolutionActions(
        lines: Lines,
        actions: List<HostnameResolutionAction>
    ) {

        actions.forEach { a ->
            val ea = actions.filter { it.hostname == a.hostname }

            if (ea.size > 1) {
                // This should not happen
                throw IllegalStateException("Have more than one action for ${a.hostname}")
            }

            lines.add("DnsCacheManipulator.setDnsCache(\"${a.hostname}\", \"${a.localIPAddress}\")")
            lines.appendSemicolon()
        }
    }

    protected fun handleExternalServiceActions(
        lines: Lines,
        actions: List<HttpExternalServiceAction>
    ) {
        // TODO: Handle same request pattern exists multiple time.
        //  Currently we don't handle the same request pattern exists multiple
        //  time. For that, need to create WireMock scenario to have multiple
        //  responses for the same request pattern.
        actions
            .filter { it.active }
            .forEachIndexed { index, action ->
                val response = action.response as HttpWsResponseParam
                val name = getWireMockVariableName(action.externalService)

                // Default behaviour of WireMock has been removed, since found no purpose
                // in case if there is a failure regarding no routes found in WireMock
                // consider adding that later
                lines.addStatement("assertNotNull(${name})")

                TestWriterUtils.handleStubForAsJavaOrKotlin(
                    lines,
                    action.externalService,
                    response,
                    action.request.method.lowercase(),
                    "urlEqualTo(\"${action.request.url}\")",
                    index+1,
                    format
                )
                lines.appendSemicolon()
                lines.addEmpty(1)
            }
    }

    /**
     * Before starting to make actions (e.g. HTTP calls in web apis), check if we need to declare any field, ie variable,
     * for this test, and other needed setups, like SQL insertions.
     * @param lines are generated lines which save the generated test scripts
     * @param ind is the final individual (ie test) to be generated into the test scripts
     * @param insertionVars contains variable names of sql insertions (Pair.first) with their results (Pair.second).
     */
    protected abstract fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testName: String
    )

    /**
     * handle action call generation
     * @param lines are generated lines which save the generated test scripts
     * @param baseUrlOfSut is the base url of sut
     * @param ind is the final individual (ie test) to be generated into the test scripts
     * @param insertionVars contains variable names of sql insertions (Pair.first) with their results (Pair.second).
     */
    protected abstract fun handleActionCalls(
            lines: Lines,
            baseUrlOfSut: String,
            ind: EvaluatedIndividual<*>,
            insertionVars: MutableList<Pair<String, String>>,
            testCaseName: String,
            testSuitePath: Path?
    )

    /**
     * handle action call generation
     * @param action is the call to be generated
     * @param index is the index of the action in a test
     * @param testCaseName is the name of the test to be saved
     * @param lines are generated lines which save the generated test scripts
     * @param result is the execution result of the action
     * @param testSuitePath is the path where to save the test suite, such info might be used to save files used in the test
     * @param baseUrlOfSut is the base url of sut
     */
    protected fun addActionLines(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String){

        if(result is EnterpriseActionResult){
            result.getFaults().sortedBy { it.category.code }
                .forEach {
                    val cat = it.category
                    lines.addSingleCommentLine("Fault${cat.code}. ${cat.name}. ${it.context}")
                }
        }

        addActionLinesPerType(action, index, testCaseName, lines, result, testSuitePath, baseUrlOfSut)
    }

    /**
     * Shouldn't be called directly, as called by addActionLines
     */
    protected abstract fun addActionLinesPerType(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String)


    protected abstract fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean

    /**
     * add extra static variable that could be specific to a problem
     */
    open fun addExtraStaticVariables(lines: Lines) {}

    /**
     * add extra init statement before all tests are executed (e.g., @BeforeAll for junit)
     * that could be specific to a problem
     */
    open fun addExtraInitStatement(lines: Lines) {}

    protected fun addActionInTryCatch(
        call: Action,
        index: Int,
        testCaseName: String,
        lines: Lines,
        res: ActionResult,
        testSuitePath: Path?,
        baseUrlOfSut: String
    ) {
        when {
            /*
                TODO do we need to handle differently in JS due to Promises?
             */
            format.isJavaOrKotlin() -> lines.add("try{")
            format.isJavaScript() -> lines.add("try{")
            format.isCsharp() -> lines.add("try{")
            format.isPython() -> lines.add("try:")
        }

        lines.indented {
            addActionLines(call,index, testCaseName, lines, res, testSuitePath, baseUrlOfSut)

            if (shouldFailIfExceptionNotThrown(res)) {
                if (!format.isJavaScript()) {
                    /*
                        TODO need a way to do it for JS, see
                        https://github.com/facebook/jest/issues/2129
                        what about expect(false).toBe(true)?
                     */
                    if (format.isPython()) {
                        lines.add("raise AssertionError(\"Expected exception\")")
                    } else {
                        lines.add("fail(\"Expected exception\");")
                    }
                }
            }
        }

        /*
         Python does not distinguish between errors and exceptions,
         therefore we need to throw a specific exception to catch and throw again in the catch.
         Any other exception thrown will go to the second catch which has a `pass` body that allows for the next code to be executed
         */
        if (format.isPython()) {
            lines.add("except AssertionError as e:")
            lines.indented {
                lines.add("raise e")
            }
        }

        when {
            format.isJava() -> lines.add("} catch(Exception e){")
            format.isKotlin() -> lines.add("} catch(e: Exception){")
            format.isJavaScript() -> lines.add("} catch(e){")
            format.isCsharp() -> lines.add("} catch(Exception e){")
            format.isPython() -> lines.add("except Exception as e:")
        }

        res.getErrorMessage()?.let {
            lines.indented {
                lines.addSingleCommentLine("${it.replace('\n', ' ').replace('\r', ' ')}")
            }
        }

        if (format.isPython()) {
            lines.indented {
                lines.add("pass")
            }
        } else {
            lines.add("}")
        }
    }


    @Deprecated("dont' use")
    protected fun clusterComment(lines: Lines, test: TestCase) {
        if (test.test.clusterAssignments.size > 0) {
            lines.startCommentBlock()
            lines.addBlockCommentLine("[${test.name}] is a part of 1 or more clusters, as defined by the selected clustering options. ")
            for (c in test.test.clusterAssignments) {
                lines.addBlockCommentLine("$c")
            }
            lines.endCommentBlock()
        }
    }

    /**
     * an optional handling for handling generated tests
     */
    open fun additionalTestHandling(tests: List<TestCase>) {
        // do nothing
    }


}

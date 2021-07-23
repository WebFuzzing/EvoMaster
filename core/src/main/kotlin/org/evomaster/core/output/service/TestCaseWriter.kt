package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.slf4j.LoggerFactory


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

    protected val format : OutputFormat
        get(){ return config.outputFormat}


    companion object {
        private val log = LoggerFactory.getLogger(TestCaseWriter::class.java)
    }



    fun convertToCompilableTestCode(
            test: TestCase,
            baseUrlOfSut: String
    ): Lines {

        counter = 0

        val lines = Lines()

        if (config.testSuiteSplitType == EMConfig.TestSuiteSplitType.CLUSTER
                && test.test.getClusters().size != 0) {
            clusterComment(lines, test)
        }

        if (format.isJUnit()) {
            lines.add("@Test")
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
        }

        lines.indented {
            val ind = test.test
            handleFieldDeclarations(lines, baseUrlOfSut, ind)
            handleActionCalls(lines, baseUrlOfSut, ind)
        }

        lines.add("}")

        if (format.isJavaScript()) {
            lines.append(");")
        }
        return lines
    }

    /**
     * Before starting to make actions (eg HTTP calls in web apis), check if we need to declare any field, ie variable,
     * for this test.
     */
    protected abstract fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>)

    protected abstract fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>)

    protected abstract fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String)

    protected abstract fun shouldFailIfException(result: ActionResult): Boolean


    protected fun addActionInTryCatch(call: Action,
                                      lines: Lines,
                                      res: ActionResult,
                                      baseUrlOfSut: String) {
        when {
            /*
                TODO do we need to handle differently in JS due to Promises?
             */
            format.isJavaOrKotlin() -> lines.add("try{")
            format.isJavaScript() -> lines.add("try{")
            format.isCsharp() -> lines.add("try{")
        }

        lines.indented {
            addActionLines(call, lines, res, baseUrlOfSut)

            if (shouldFailIfException(res)) {
                if (!format.isJavaScript()) {
                    /*
                        TODO need a way to do it for JS, see
                        https://github.com/facebook/jest/issues/2129
                        what about expect(false).toBe(true)?
                     */
                    lines.add("fail(\"Expected exception\");")
                }
            }
        }

        when {
            format.isJava() -> lines.add("} catch(Exception e){")
            format.isKotlin() -> lines.add("} catch(e: Exception){")
            format.isJavaScript() -> lines.add("} catch(e){")
            format.isCsharp() -> lines.add("} catch(Exception e){")
        }

        res.getErrorMessage()?.let {
            lines.indented {
                lines.add("//${it.replace('\n', ' ').replace('\r',' ')}")
            }
        }
        lines.add("}")
    }


    protected fun capitalizeFirstChar(name: String): String {
        return name[0].toUpperCase() + name.substring(1)
    }


    protected fun clusterComment(lines: Lines, test: TestCase) {
        if (test.test.clusterAssignments.size > 0) {
            lines.add("/**")
            lines.add("* [${test.name}] is a part of 1 or more clusters, as defined by the selected clustering options. ")
            for (c in test.test.clusterAssignments) {
                lines.add("* $c")
            }
            lines.add("*/")
        }
    }

}
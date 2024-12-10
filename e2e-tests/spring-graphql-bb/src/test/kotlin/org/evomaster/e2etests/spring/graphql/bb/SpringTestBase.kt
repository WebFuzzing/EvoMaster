package org.evomaster.e2etests.spring.graphql.bb

import org.apache.commons.io.FileUtils
import org.evomaster.ci.utils.CIUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.BlackBoxUtils
import org.evomaster.e2etests.utils.CoveredTargets
import org.evomaster.e2etests.utils.GraphQLTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Paths
import java.time.Duration
import java.util.ArrayList
import java.util.function.Consumer


abstract class SpringTestBase : GraphQLTestBase() {

    /*
        WARNING
        lot of code below is copied&pasted from rest-bb.
        reason is that dealing with multi-inheritance in JVM is not possible,
        and refactoring would not be straightforward
     */


    @BeforeEach
    fun clearTargets(){
        CoveredTargets.reset()

        /*
            some weird issue with Surefire plugin, not happening on builds for Win and OSX... weird.
            trying to fix it with forkNode option
         */
        //CIUtils.skipIfOnLinuxOnGA()
    }

    protected fun addBlackBoxOptions(
        args: MutableList<String>,
        outputFormat: OutputFormat
    ) {
        setOption(args, "blackBox", "true")
        setOption(args, "bbTargetUrl", "$baseUrlOfSut/graphql")
        setOption(args, "problemType", "GRAPHQL")
        setOption(args, "outputFormat", outputFormat.toString())

        //this is deprecated
        setOption(args, "bbExperiments", "false")
    }

    fun executeAndEvaluateBBTest(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        targetLabel: String,
        lambda: Consumer<MutableList<String>>
    ){
        executeAndEvaluateBBTest(outputFormat, outputFolderName, iterations, timeoutMinutes,
            listOf(targetLabel),
            lambda)
    }

    fun executeAndEvaluateBBTest(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        targetLabels: Collection<String>,
        lambda: Consumer<MutableList<String>>
    ){
        assertFalse(CoveredTargets.areCovered(targetLabels))
        runBlackBoxEM(outputFormat, outputFolderName, iterations, timeoutMinutes, lambda)
        BlackBoxUtils.checkCoveredTargets(targetLabels)

        CoveredTargets.reset()
        runGeneratedTests(outputFormat, outputFolderName)
        BlackBoxUtils.checkCoveredTargets(targetLabels)
    }


    fun runBlackBoxEM(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ){
        val baseLocation = when {
            outputFormat.isJavaScript() -> BlackBoxUtils.baseLocationForJavaScript
            outputFormat.isPython() -> BlackBoxUtils.baseLocationForPython
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
        runTestForNonJVM(outputFormat, baseLocation, outputFolderName, iterations, timeoutMinutes, lambda)
    }

    fun runGeneratedTests(outputFormat: OutputFormat, outputFolderName: String){

        when{
            outputFormat.isJavaScript() -> BlackBoxUtils.runNpmTests(BlackBoxUtils.relativePath(outputFolderName))
            outputFormat.isPython() -> BlackBoxUtils.runPythonTests(BlackBoxUtils.relativePath(outputFolderName))
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
    }


    private fun runTestForNonJVM(
        outputFormat: OutputFormat,
        rootOutputFolderBasePath: String,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ) {

        val folder = Paths.get(rootOutputFolderBasePath, outputFolderName)

        assertTimeoutPreemptively(Duration.ofMinutes(timeoutMinutes.toLong())) {
            FileUtils.deleteDirectory(folder.toFile())

            handleFlaky {
                val args = getArgsWithCompilation(
                    iterations,
                    outputFolderName,
                    ClassName("FIXME"),
                    true,
                    TestSuiteSplitType.FAULTS.toString(),
                    "FALSE"
                )
                setOption(args, "outputFolder", folder.toString())
                setOption(args, "testSuiteFileName", "")
                addBlackBoxOptions(args, outputFormat)

                defaultSeed++
                lambda.accept(ArrayList(args))
            }
        }
    }

}

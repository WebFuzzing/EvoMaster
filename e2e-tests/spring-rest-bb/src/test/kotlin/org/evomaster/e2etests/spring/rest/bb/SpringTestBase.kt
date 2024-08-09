package org.evomaster.e2etests.spring.rest.bb


import org.apache.commons.io.FileUtils
import org.evomaster.ci.utils.CIUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.BlackBoxUtils
import org.evomaster.e2etests.utils.CoveredTargets
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.function.Consumer
import kotlin.collections.Collection


abstract class SpringTestBase : RestTestBase() {

    companion object {
        /*
            dirty hack to avoid applying instrumentation
         */
        init {
            EnterpriseTestBase.shouldApplyInstrumentation = false
        }

        @JvmStatic
        @AfterAll
        fun resetInstrumentation(){
            EnterpriseTestBase.shouldApplyInstrumentation = true
        }
    }



    @BeforeEach
    fun clearTargets(){
        CoveredTargets.reset()

        /*
            some weird issue with Surefire plugin, not happening on builds for Win and OSX... weird.
            trying to fix it with forkNode option.

            Update: still having very weird issues with dependencies not found:
            -----------
             Failed to execute goal on project evomaster-e2e-tests-bb-workspace-rest:
             Could not resolve dependencies for project org.evomaster:evomaster-e2e-tests-bb-workspace-rest:jar:3.0.1-SNAPSHOT:
             The following artifacts could not be resolved: org.evomaster:evomaster-client-java-dependencies:pom:3.0.1-SNAPSHOT,
             org.evomaster:evomaster-client-java-controller:jar:3.0.1-SNAPSHOT:
             Could not find artifact org.evomaster:evomaster-client-java-dependencies:pom:3.0.1-SNAPSHOT -> [Help 1]
            ------------
            but it works on Mac and Win???
            disabling for now...
        */
        CIUtils.skipIfOnLinuxOnGA()
    }

    protected fun addBlackBoxOptions(
        args: MutableList<String>,
        outputFormat: OutputFormat
    ) {
        setOption(args, "blackBox", "true")
        setOption(args, "bbTargetUrl", baseUrlOfSut)
        setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
        setOption(args, "problemType", "REST")
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
        assumeTrue(outputFormat != OutputFormat.DEFAULT)

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
            outputFormat.isJava() -> BlackBoxUtils.baseLocationForJava
            outputFormat.isKotlin() -> BlackBoxUtils.baseLocationForKotlin
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
        runTestForNonJVM(outputFormat, baseLocation, outputFolderName, iterations, timeoutMinutes, lambda)
    }

    fun runGeneratedTests(outputFormat: OutputFormat, outputFolderName: String){

        when{
            outputFormat.isJavaScript() -> BlackBoxUtils.runNpmTests(BlackBoxUtils.relativePath(outputFolderName))
            outputFormat.isPython() -> BlackBoxUtils.runPythonTests(BlackBoxUtils.relativePath(outputFolderName))
            outputFormat.isJava() -> BlackBoxUtils.runJavaTests(outputFolderName)
            outputFormat.isKotlin() -> BlackBoxUtils.runKotlinTests(outputFolderName)
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
        val folder = if(outputFormat.isJavaOrKotlin()){
            Paths.get(rootOutputFolderBasePath)
        } else {
            Paths.get(rootOutputFolderBasePath, outputFolderName)
        }

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

                if(outputFormat.isJava()){
                    setOption(args,"outputFilePrefix",BlackBoxUtils.getOutputFilePrefixJava(outputFolderName))
                }
                if(outputFormat.isKotlin()){
                    setOption(args,"outputFilePrefix",BlackBoxUtils.getOutputFilePrefixKotlin(outputFolderName))
                }

                defaultSeed++
                lambda.accept(ArrayList(args))
            }
        }
    }

}

package org.evomaster.e2etests.python.rest.bb

import org.apache.commons.io.FileUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.BlackBoxUtils
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Paths
import java.time.Duration
import java.util.function.Consumer

/**
 * Base class for black-box e2e tests whose SUT is an external (non-JVM) process,
 * such as a Python/Django application. Unlike the Spring black-box base, it never starts
 * an EvoMaster Driver/Controller: it only runs EvoMaster against a running HTTP API.
 *
 * Like the standard black-box tests, generated tests are produced in every supported
 * output format (Java, Kotlin, JavaScript, Python) and each is compiled/executed against
 * the running SUT with its own toolchain (Maven, npm, Python).
 */
abstract class ExternalSutBlackBoxTestBase : RestTestBase() {

    companion object {
        init {
            // we never instrument anything here: the SUT is an external process
            EnterpriseTestBase.shouldApplyInstrumentation = false
        }

        @JvmStatic
        private var sutBaseUrl: String = ""

        @JvmStatic
        private var sutSchemaPath: String = "/api/schema/"

        /**
         * Register the running external SUT before the black-box tests run.
         * Compiled to an inherited static method, so subclasses can call it from their
         * own @BeforeAll, mirroring how EnterpriseTestBase.initClass is used.
         */
        @JvmStatic
        protected fun configureSut(baseUrl: String, schemaPath: String) {
            sutBaseUrl = baseUrl
            sutSchemaPath = schemaPath
        }

        @JvmStatic
        @AfterAll
        fun resetInstrumentation() {
            EnterpriseTestBase.shouldApplyInstrumentation = true
        }
    }

    private fun addBlackBoxOptions(args: MutableList<String>, outputFormat: OutputFormat) {
        setOption(args, "blackBox", "true")
        setOption(args, "bbTargetUrl", sutBaseUrl)
        setOption(args, "bbSwaggerUrl", "$sutBaseUrl$sutSchemaPath")
        setOption(args, "problemType", "REST")
        setOption(args, "outputFormat", outputFormat.toString())
        setOption(args, "bbExperiments", "false")
    }

    /**
     * Run EvoMaster in black-box mode against the external SUT, generate tests in [outputFormat],
     * then compile/run the generated tests against the still-running SUT.
     * Black-box assertions on the search archive must be done inside [lambda].
     */
    fun executeAndEvaluateBBTest(
        outputFormat: OutputFormat,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ) {
        assumeTrue(outputFormat != OutputFormat.DEFAULT)

        val baseLocation = when {
            outputFormat.isJavaScript() -> BlackBoxUtils.baseLocationForJavaScript
            outputFormat.isPython() -> BlackBoxUtils.baseLocationForPython
            outputFormat.isJava() -> BlackBoxUtils.baseLocationForJava
            outputFormat.isKotlin() -> BlackBoxUtils.baseLocationForKotlin
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }

        runBlackBoxEM(outputFormat, baseLocation, outputFolderName, iterations, timeoutMinutes, lambda)
        runGeneratedTests(outputFormat, outputFolderName)
    }

    private fun runGeneratedTests(outputFormat: OutputFormat, outputFolderName: String) {
        when {
            outputFormat.isJavaScript() -> BlackBoxUtils.runNpmTests(BlackBoxUtils.relativePath(outputFolderName))
            outputFormat.isPython() -> BlackBoxUtils.runPythonTests(BlackBoxUtils.relativePath(outputFolderName))
            outputFormat.isJava() -> BlackBoxUtils.runJavaTests(outputFolderName)
            outputFormat.isKotlin() -> BlackBoxUtils.runKotlinTests(outputFolderName)
            else -> throw IllegalArgumentException("Not supported output type $outputFormat")
        }
    }

    private fun runBlackBoxEM(
        outputFormat: OutputFormat,
        rootOutputFolderBasePath: String,
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ) {
        val folder = if (outputFormat.isJavaOrKotlin()) {
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

                if (outputFormat.isJava()) {
                    setOption(args, "outputFilePrefix", BlackBoxUtils.getOutputFilePrefixJava(outputFolderName))
                }
                if (outputFormat.isKotlin()) {
                    setOption(args, "outputFilePrefix", BlackBoxUtils.getOutputFilePrefixKotlin(outputFolderName))
                }

                defaultSeed++
                lambda.accept(ArrayList(args))
            }
        }
    }
}

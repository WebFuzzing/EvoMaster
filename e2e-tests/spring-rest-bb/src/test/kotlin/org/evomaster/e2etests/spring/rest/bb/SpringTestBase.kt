package org.evomaster.e2etests.spring.rest.bb


import org.apache.commons.io.FileUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.function.Consumer


abstract class SpringTestBase : RestTestBase() {

    companion object{
        const val JS_BASE_PATH = "./javascript"
        const val GENERATED_FOLDER_NAME = "generated"
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


    fun runTestForJS(
        outputFolderName: String,
        iterations: Int,
        timeoutMinutes: Int,
        lambda: Consumer<MutableList<String>>
    ) {
        val baseLocation = "$JS_BASE_PATH/$GENERATED_FOLDER_NAME"
        runTestForNonJVM(OutputFormat.JS_JEST, baseLocation, outputFolderName, iterations, timeoutMinutes, lambda)
    }

    fun runTestForNonJVM(
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
package org.evomaster.e2etests.spring.rest.bb


import org.apache.commons.io.FileUtils
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig.TestSuiteSplitType
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.utils.CoveredTargets
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer


abstract class SpringTestBase : RestTestBase() {

    companion object{
        const val JS_BASE_PATH = "./javascript"
        const val GENERATED_FOLDER_NAME = "generated"
    }

    @BeforeEach
    fun clearTargets(){
        CoveredTargets.reset()
    }

    protected fun relativePath(folderName: String) = "$GENERATED_FOLDER_NAME/$folderName"

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

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
    }

    private fun npm() = if(isWindows()) "npm.cmd" else "npm"

    private fun runNpmInstall(){

        val command = listOf(npm(),"ci")

        val builder = ProcessBuilder(command)
        builder.inheritIO()
        builder.directory(File(JS_BASE_PATH))

        val process = builder.start()
        val timeout = 30L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if(!terminated){
            process.destroy()
            throw IllegalStateException("NPM installation failed within $timeout seconds")
        }

        if(process.exitValue() != 0){
            throw IllegalStateException("NPM installation failed with status code: ${process.exitValue()}")
        }
    }

    fun runNpmTests(folderRelativePath: String){

        runNpmInstall()

        val command = listOf(npm(),"test", folderRelativePath)

        val builder = ProcessBuilder(command)
        builder.inheritIO()
        builder.directory(File(JS_BASE_PATH))

        val process = builder.start()
        val timeout = 120L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if(!terminated){
            process.destroy()
            throw IllegalStateException("NPM tests did not complete within $timeout seconds")
        }

        if(process.exitValue() != 0){
            throw IllegalStateException("NPM tests failed with status code: ${process.exitValue()}")
        }
    }
}
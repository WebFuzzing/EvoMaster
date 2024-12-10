package org.evomaster.e2etests.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object BlackBoxUtils {

    private const val JS_BASE_PATH = "./javascript"
    private const val PY_BASE_PATH = "./python"
    private const val MAVEN_BASE_PATH = "./maven"
    private const val GENERATED_FOLDER_NAME = "generated"

    const val baseLocationForJavaScript = "$JS_BASE_PATH/$GENERATED_FOLDER_NAME"
    const val baseLocationForPython = "$PY_BASE_PATH/$GENERATED_FOLDER_NAME"
    const val baseLocationForJava = "$MAVEN_BASE_PATH/src/test/java"
    const val baseLocationForKotlin = "$MAVEN_BASE_PATH/src/test/kotlin"

    fun relativePath(folderName: String) = "$GENERATED_FOLDER_NAME/$folderName"

    fun checkCoveredTargets(targetLabels: Collection<String>) {
        targetLabels.forEach {
            assertTrue(CoveredTargets.isCovered(it), "Target '$it' is not covered")
        }
        assertEquals(targetLabels.size, CoveredTargets.numberOfCoveredTargets())
    }


    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
    }

    private fun npm() = if (isWindows()) "npm.cmd" else "npm"

    private fun mvn() = if (isWindows()) "mvn.cmd" else "mvn"

    private fun runNpmInstall() {
        val command = listOf(npm(), "ci")

        executeInstallShellCommand(command, JS_BASE_PATH, "NPM")
    }

    private fun installPythonRequirements() {
        val upgradePipCommand = listOf("python", "-m", "pip", "install", "--upgrade", "pip", "--user")
        executeInstallShellCommand(upgradePipCommand, PY_BASE_PATH, "pip")

        val installRequirementsCommand = listOf("pip", "install", "-r", "./requirements.txt", "--user")
        executeInstallShellCommand(installRequirementsCommand, PY_BASE_PATH, "requirements")
    }

    private fun executeInstallShellCommand(command: List<String>, directory: String, technology: String) {
        val builder = ProcessBuilder(command)
        //Surefire does NOT like it if we redirect output to file (Maven ll not work)...
        builder.inheritIO()
        builder.directory(File(directory))

        val process = builder.start()
        val timeout = 30L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if (!terminated) {
            process.destroy()
            throw IllegalStateException("$technology installation failed within $timeout seconds")
        }

        if (process.exitValue() != 0) {
            throw IllegalStateException("$technology installation failed with status code: ${process.exitValue()}")
        }
    }

    private fun runTestsCommand(command: List<String>, directory: String, technology: String) {
        val builder = ProcessBuilder(command)
        //Surefire does NOT like it if we redirect output to file (Maven ll not work)...
        builder.inheritIO()
        builder.directory(File(directory))

        val process = builder.start()
        val timeout = 120L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if (!terminated) {
            process.destroy()
            throw IllegalStateException("$technology tests did not complete within $timeout seconds")
        }

        if (process.exitValue() != 0) {
            throw IllegalStateException("$technology tests failed with status code: ${process.exitValue()}")
        }
    }

    fun runNpmTests(folderRelativePath: String) {
        runNpmInstall()

        val command = listOf(npm(), "test", folderRelativePath)
        runTestsCommand(command, JS_BASE_PATH, "NPM")
    }

    fun runPythonTests(folderRelativePath: String) {
        installPythonRequirements()

        val command = listOf("python", "-m", "unittest", "discover", "-s", folderRelativePath, "-p", "*_Test.py")
        runTestsCommand(command, PY_BASE_PATH, "Python")
    }

    fun runJavaTests(outputFolderName: String) {
        val prefix = getOutputFilePrefixJava(outputFolderName)
        runMavenTests(prefix)
    }

    fun runKotlinTests(outputFolderName: String){
        val prefix = getOutputFilePrefixKotlin(outputFolderName)
        runMavenTests(prefix)
    }

    private fun runMavenTests(prefix: String){
        val command = listOf(mvn(),"clean","test","-Dtest=$prefix*")
        runTestsCommand(command, MAVEN_BASE_PATH, "Maven")
    }

    fun getOutputFilePrefixJava(outputFolderName: String) = "com.java.$outputFolderName.EM"

    fun getOutputFilePrefixKotlin(outputFolderName: String) = "com.kotlin.$outputFolderName.EM"

}

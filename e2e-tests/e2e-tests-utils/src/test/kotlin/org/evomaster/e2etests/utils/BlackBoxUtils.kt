package org.evomaster.e2etests.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object BlackBoxUtils {

    private const val JS_BASE_PATH = "./javascript"
    private const val GENERATED_FOLDER_NAME = "generated"

    const val baseLocationForJavaScript = "$JS_BASE_PATH/$GENERATED_FOLDER_NAME"

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

    private fun runNpmInstall() {

        val command = listOf(npm(), "ci")

        val builder = ProcessBuilder(command)
        builder.inheritIO()
        builder.directory(File(JS_BASE_PATH))

        val process = builder.start()
        val timeout = 30L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if (!terminated) {
            process.destroy()
            throw IllegalStateException("NPM installation failed within $timeout seconds")
        }

        if (process.exitValue() != 0) {
            throw IllegalStateException("NPM installation failed with status code: ${process.exitValue()}")
        }
    }

    fun runNpmTests(folderRelativePath: String) {

        runNpmInstall()

        val command = listOf(npm(), "test", folderRelativePath)

        val builder = ProcessBuilder(command)
        builder.inheritIO()
        builder.directory(File(JS_BASE_PATH))

        val process = builder.start()
        val timeout = 120L
        val terminated = process.waitFor(timeout, TimeUnit.SECONDS)

        if (!terminated) {
            process.destroy()
            throw IllegalStateException("NPM tests did not complete within $timeout seconds")
        }

        if (process.exitValue() != 0) {
            throw IllegalStateException("NPM tests failed with status code: ${process.exitValue()}")
        }
    }
}
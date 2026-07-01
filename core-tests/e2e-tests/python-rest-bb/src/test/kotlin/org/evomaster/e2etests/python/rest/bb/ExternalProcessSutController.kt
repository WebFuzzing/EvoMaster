package org.evomaster.e2etests.python.rest.bb

import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Manages the lifecycle of an external (non-JVM) System Under Test started as an OS process.
 * Used to run black-box e2e tests against APIs implemented in languages other than the JVM ones,
 * e.g. a Python/Django application.
 */
open class ExternalProcessSutController(
    private val command: List<String>,
    private val workingDirectory: File,
    private val environment: Map<String, String>,
    val baseUrl: String,
    private val readinessPath: String
) {

    private var process: Process? = null

    fun start(startupTimeoutSeconds: Long = 90) {
        val builder = ProcessBuilder(command)
        builder.directory(workingDirectory)
        builder.inheritIO()
        builder.environment().putAll(environment)
        process = builder.start()
        waitUntilReady(startupTimeoutSeconds)
    }

    private fun waitUntilReady(timeoutSeconds: Long) {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        var lastError: Exception? = null

        while (System.currentTimeMillis() < deadline) {
            val p = process
            if (p != null && !p.isAlive) {
                throw IllegalStateException("SUT process terminated during startup with exit code ${p.exitValue()}")
            }
            try {
                val conn = URL("$baseUrl$readinessPath").openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                conn.disconnect()
                // any HTTP response means the server is up and serving
                if (code in 200..599) {
                    return
                }
            } catch (e: Exception) {
                lastError = e
            }
            Thread.sleep(500)
        }

        stop()
        throw IllegalStateException(
            "SUT did not become ready within $timeoutSeconds seconds at $baseUrl$readinessPath",
            lastError
        )
    }

    fun stop() {
        val p = process ?: return
        p.descendants().forEach { it.destroy() }
        p.destroy()
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.descendants().forEach { it.destroyForcibly() }
            p.destroyForcibly()
        }
        process = null
    }

    companion object {
        fun findFreePort(): Int {
            ServerSocket(0).use { return it.localPort }
        }
    }
}

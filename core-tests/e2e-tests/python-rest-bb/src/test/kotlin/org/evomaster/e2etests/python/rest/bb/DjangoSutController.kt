package org.evomaster.e2etests.python.rest.bb

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Starts the minimal Django/DRF SUT located under sut/django as an external process.
 *
 * To keep the developer/CI machine clean and to guarantee the pinned dependency versions,
 * all Python dependencies are installed into a dedicated virtual environment ([venvDir]),
 * not into the system or user site-packages. The venv interpreter is then used to run
 * migrations and the development server.
 *
 * The OpenAPI schema is exposed at [schemaPath] (drf-spectacular).
 */
class DjangoSutController(
    private val djangoProjectDir: File = File("sut/django")
) {

    val schemaPath = "/api/schema/"

    private val port = ExternalProcessSutController.findFreePort()
    val baseUrl = "http://localhost:$port"

    // interpreter used only to bootstrap the virtual environment
    private val systemPython: String = resolvePython()

    private val venvDir = File(djangoProjectDir, ".venv")
    // interpreter inside the virtual environment, used for everything else
    private val venvPython: String = venvPythonPath(venvDir)

    // created in start(), once the venv interpreter is available
    private var process: ExternalProcessSutController? = null

    fun start() {
        createVenv()
        installRequirements()
        // start from a clean database, also guarding against leftovers from a crashed run
        deleteDatabaseFiles()
        migrate()
        process = ExternalProcessSutController(
            command = listOf(venvPython, "manage.py", "runserver", "127.0.0.1:$port", "--noreload"),
            workingDirectory = djangoProjectDir,
            environment = mapOf("DJANGO_SETTINGS_MODULE" to "sutproject.settings"),
            baseUrl = baseUrl,
            readinessPath = schemaPath
        )
        process!!.start(90)
    }

    fun stop() {
        // stop the server first, then delete the DB: while the process is alive it holds
        // a lock on the SQLite file (notably on Windows) and deletion would fail
        process?.stop()
        deleteDatabaseFiles()
    }

    /**
     * Remove the SQLite database (and its possible auxiliary files) so that each test run
     * starts from a clean state and nothing is left behind on the machine.
     */
    private fun deleteDatabaseFiles() {
        val dbFileNames = listOf("db.sqlite3", "db.sqlite3-journal", "db.sqlite3-wal", "db.sqlite3-shm")
        for (name in dbFileNames) {
            val file = File(djangoProjectDir, name)
            if (!file.exists()) {
                continue
            }
            var deleted = false
            // the file may stay briefly locked right after the server is killed
            for (attempt in 0 until 10) {
                if (file.delete()) {
                    deleted = true
                    break
                }
                Thread.sleep(200)
            }
            if (!deleted && file.exists()) {
                file.deleteOnExit()
            }
        }
    }

    private fun createVenv() {
        // reuse an existing venv if its interpreter is already present
        if (File(venvPython).exists()) {
            return
        }
        runBlocking(listOf(systemPython, "-m", "venv", venvDir.absolutePath), 120, "python -m venv")
    }

    private fun installRequirements() {
        // no --user: we are installing into the dedicated virtual environment
        runBlocking(
            listOf(venvPython, "-m", "pip", "install", "-r", "requirements.txt"),
            300, "pip install (django SUT venv)"
        )
    }

    private fun migrate() {
        runBlocking(listOf(venvPython, "manage.py", "migrate", "--noinput"), 60, "django migrate")
    }

    private fun runBlocking(command: List<String>, timeoutSeconds: Long, label: String) {
        val builder = ProcessBuilder(command).directory(djangoProjectDir).inheritIO()
        builder.environment()["DJANGO_SETTINGS_MODULE"] = "sutproject.settings"
        val p = builder.start()
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            throw IllegalStateException("$label timed out after $timeoutSeconds seconds")
        }
        if (p.exitValue() != 0) {
            throw IllegalStateException("$label failed with exit code ${p.exitValue()}")
        }
    }

    companion object {

        private fun isWindows(): Boolean {
            return System.getProperty("os.name").lowercase().contains("win")
        }

        /**
         * Path of the Python interpreter inside a virtual environment, which differs by OS.
         */
        private fun venvPythonPath(venvDir: File): String {
            val relative = if (isWindows()) "Scripts/python.exe" else "bin/python"
            return File(venvDir, relative).absolutePath
        }

        /**
         * Python installations differ across OSes and CI. Prefer "python", fall back to "python3".
         * Mirrors the resolution logic already used in BlackBoxUtils for generated tests.
         * Only used to create the virtual environment.
         */
        private fun resolvePython(): String {
            for (candidate in listOf("python", "python3")) {
                try {
                    val p = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
                    if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0) {
                        return candidate
                    }
                } catch (_: Exception) {
                    // try next candidate
                }
            }
            throw IllegalStateException("Could not find a working 'python' or 'python3' on PATH")
        }
    }
}

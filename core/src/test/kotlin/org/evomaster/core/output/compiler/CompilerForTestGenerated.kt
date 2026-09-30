package org.evomaster.core.output.compiler

import org.evomaster.core.output.OutputFormat
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.Services
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths
import kotlin.reflect.KClass


object CompilerForTestGenerated{

    fun compile(
            format: OutputFormat,
            code: String,
            testClassName: String
    ) {

        val baseFolder = File("target/evomaster-tests")
        val source = baseFolder.toPath().resolve(Paths.get("sut_$testClassName")).toFile()
        source.deleteRecursively()
        source.mkdirs()

        val extension = if (format.isJava()) "java" else "kt"
        val target = source.toPath().resolve(Paths.get("$testClassName.$extension")).toFile()

        target.createNewFile()
        target.writeText(code)

        /*
            The idea here is that, if we compile directly to test-classes, then
            we can just use the classes directly by reflection without needing to mess
            up with new classpaths and classloaders
         */
        compile(format, source, File("target/test-classes"))
    }


    fun compile(
            format: OutputFormat,
            source: File,
            destination: File
    ) {

        when {
            format.isKotlin() -> compileKotlin(source, destination)
            /*
                This used to wait on the move to JDK 11, since doing it on 8 meant dealing with
                tools.jar. The compiler has been part of the platform since then, so it is just
                asked for.
             */
            format.isJava() -> compileJava(source, destination)
            else -> throw IllegalStateException("Format $format not supported yet for compilation checks")
        }
    }


    private fun compileJava(source: File, destination: File){

        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: throw IllegalStateException("No Java compiler available: a JDK is needed, not a JRE")

        val files = source.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.toList()

        if (files.isEmpty()) {
            throw IllegalStateException("No Java sources to compile under " + source.absolutePath)
        }

        destination.mkdirs()

        val diagnostics = DiagnosticCollector<JavaFileObject>()

        compiler.getStandardFileManager(diagnostics, null, null).use { manager ->

            val units = manager.getJavaFileObjectsFromFiles(files)

            val options = listOf(
                "-classpath", System.getProperty("java.class.path"),
                "-d", destination.absolutePath
            )

            val ok = compiler.getTask(null, manager, diagnostics, options, null, units).call()

            if (ok != true) {
                val errors = diagnostics.diagnostics.joinToString(System.lineSeparator())
                throw RuntimeException("Failed to compile class. Error:" + System.lineSeparator() + errors)
            }
        }
    }

    private fun compileKotlin(source: File, destination: File){

        //collect error messages to a buffer, and print them only if compilation fails
        val buffer = ByteArrayOutputStream()
        val collector = PrintingMessageCollector(
                PrintStream(buffer),
                MessageRenderer.PLAIN_RELATIVE_PATHS,
                true
        )

        val arguments = K2JVMCompilerArguments().apply {
            verbose = true
            jvmTarget = JvmTarget.JVM_1_8.description
            freeArgs = listOf(source.absolutePath)
            classpath = System.getProperty("java.class.path")
        }
        arguments.destination = destination.absolutePath


        val compiler = K2JVMCompiler()

        val exitCode = compiler.exec(collector, Services.EMPTY, arguments)

        if(exitCode != ExitCode.OK) {
            throw RuntimeException("Failed to compile class. Error:\n" + buffer.toString())
        }
    }
}
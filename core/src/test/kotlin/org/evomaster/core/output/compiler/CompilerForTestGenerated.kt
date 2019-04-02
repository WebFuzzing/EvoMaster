package org.evomaster.core.output.compiler

import org.evomaster.core.output.OutputFormat
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.Services
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

        val target = source.toPath().resolve(Paths.get("$testClassName.kt")).toFile()

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

        /*
            Handling Java in JDK is doable (can do same as done in EvoSuite), but bit
            messy/unclean (see all issues with tools.jar).
            In JDK 11 should be easier (but haven't verified it), and invalidate how done
            in JKD 8.

            TODO: so, we will support Java only once upgraded to JDK 11
         */

        when(format){
            OutputFormat.KOTLIN_JUNIT_5 -> compileKotlin(source, destination)
            else -> throw IllegalStateException("Format $format not supported yet for compilation checks")
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
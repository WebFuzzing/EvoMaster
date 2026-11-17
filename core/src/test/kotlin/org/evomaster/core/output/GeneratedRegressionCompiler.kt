package org.evomaster.core.output

import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider

/** Compile the actual writer output, not a manually reproduced version of it. */
internal object GeneratedRegressionCompiler {
    fun withClass(directory: Path, format: OutputFormat, name: String, code: String, check: (Class<*>) -> Unit) {
        Files.createDirectories(directory)
        val source = directory.resolve(name + if (format.isJava()) ".java" else ".kt")
        Files.writeString(source, code)
        if (format.isJava()) {
            val compiler = ToolProvider.getSystemJavaCompiler()
            assertNotNull(compiler)
            assertEquals(0, compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                "-d", directory.toString(), source.toString()), code)
        } else {
            CompilerForTestGenerated.compile(format, source.toFile(), directory.toFile())
        }
        URLClassLoader(arrayOf(directory.toUri().toURL()), GeneratedRegressionCompiler::class.java.classLoader).use {
            check(it.loadClass(name))
        }
    }
}

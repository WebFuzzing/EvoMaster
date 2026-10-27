package org.evomaster.core.output.compiler

import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.charset.Charset
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

class CompilerForTestGeneratedTest{


    @Test
    fun testCompileBaseKotlinClass() {
        checkCompileKotlinClass(File("src/test/data/base"), "AKotlinClassToCompile")
    }

    @Test
    fun testCompileKotlinClassWithDependencies() {
        checkCompileKotlinClass(File("src/test/data/dependencies"), "AKotlinClassWithDependencies")
    }


    @Test
    fun testCompileJavaCode(){

        val name = "FooCompileJavaCodeClass"
        val msg = "Hello World!"
        val code = "public class $name { public String exe() { return \"$msg\"; } }"

        CompilerForTestGenerated.compile(
                OutputFormat.JAVA_JUNIT_5,
                code,
                name
        )

        val klass = this.javaClass.classLoader.loadClass(name)
        val obj = klass.newInstance()
        val res = klass.getDeclaredMethod("exe").invoke(obj) as String

        assertEquals(msg, res)
    }

    @Test
    fun testJavaThatDoesNotCompileSaysWhy(){

        val name = "FooBrokenJavaClass"
        val code = "public class $name { this is not Java }"

        val error = assertThrows(RuntimeException::class.java) {
            CompilerForTestGenerated.compile(OutputFormat.JAVA_JUNIT_5, code, name)
        }

        assertTrue(error.message!!.contains("Failed to compile"), error.message)
    }

    @Test
    fun testCompileCode(){

        val name = "FooCompileCodeClass"
        val msg = "Hello World!"
        val code = "class $name { fun exe() = \"$msg\" }"

        CompilerForTestGenerated.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                code,
                name
        )

        val klass = this.javaClass.classLoader.loadClass(name)
        val obj = klass.newInstance()
        val res = klass.getDeclaredMethod("exe").invoke(obj) as String

        assertEquals(msg, res)
    }


    @Test
    fun testJavaSourceUsesUtf8WithANonUtf8JvmDefault() {
        val directory = Files.createTempDirectory("generated-java-encoding").toFile()
        val output = File(directory, "output.log")
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
            .joinToString(File.pathSeparator) { File(it).absolutePath }
        val process = ProcessBuilder(
            File(System.getProperty("java.home"), "bin/java").absolutePath,
            "-Dfile.encoding=ISO-8859-1", "-cp", classpath, javaClass.name
        ).directory(directory).redirectErrorStream(true).redirectOutput(output).start()
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Encoding probe timed out")
            assertEquals(0, process.exitValue(), output.readText())
        } finally {
            if (process.isAlive) {
                process.destroyForcibly().waitFor()
            }
            directory.deleteRecursively()
        }
    }

    companion object {
        /** Runs in a fresh JVM so its default charset cannot be cached from another test. */
        @JvmStatic
        fun main(args: Array<String>) {
            check(Charset.defaultCharset() == Charsets.ISO_8859_1)
            val name = "GeneratedUnicodeJava"
            val expected = "\u00e9"
            CompilerForTestGenerated.compile(OutputFormat.JAVA_JUNIT_5,
                "public class $name { public String value() { return \"$expected\"; } }", name)
            URLClassLoader(arrayOf(File("target/test-classes").toURI().toURL())).use { loader ->
                val klass = loader.loadClass(name)
                val value = klass.getDeclaredMethod("value").invoke(klass.getDeclaredConstructor().newInstance())
                check(expected == value) { "The compiler changed a non-ASCII literal" }
            }
        }
    }

    private fun checkCompileKotlinClass(source: File, className: String){

        assertTrue(source.exists())
        assertTrue(source.isDirectory)

        val destination = File("target/compiler-tests")
        assertTrue(destination.deleteRecursively())
        assertFalse(destination.exists())

        //should not throw any exception
        CompilerForTestGenerated.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                source,
                destination
        )


        val bytecode = destination.toPath().resolve("$className.class")
        assertTrue(Files.exists(bytecode))
    }
}
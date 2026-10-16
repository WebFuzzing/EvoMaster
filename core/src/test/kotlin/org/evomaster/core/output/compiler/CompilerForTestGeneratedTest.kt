package org.evomaster.core.output.compiler

import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

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

        /*
            The Java half, which the AsyncAPI E2E is otherwise the only thing that exercises --
            and that one needs Docker and a broker to say anything at all.
         */
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
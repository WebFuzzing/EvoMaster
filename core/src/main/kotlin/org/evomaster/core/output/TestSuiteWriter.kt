package org.evomaster.core.output

import org.evomaster.core.search.Solution
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Given a Solution as input, convert it to a string representation of
 * the tests that can be written to file and be compiled
 */
class TestSuiteWriter {

    companion object {

        fun writeTests(
                solution: Solution<*>,
                format: OutputFormat,
                outputFolder: String,
                testSuiteFileName: String){

            val name = TestSuiteFileName(testSuiteFileName)

            val content = convertToCompilableTestCode(solution, format, name)
            saveToDisk(content, format, outputFolder, name)
        }


        fun convertToCompilableTestCode(
                solution: Solution<*>,
                format: OutputFormat,
                testSuiteFileName: TestSuiteFileName)
                : String {


            //TODO name for each test, can start with just a counter


            val buffer = StringBuilder(2048)

            header(format, testSuiteFileName, buffer)

            footer(buffer)

            return buffer.toString()
        }


        fun saveToDisk(testFileContent: String,
                       format: OutputFormat,
                       outputFolder: String,
                       testSuiteFileName: TestSuiteFileName){

            val path = Paths.get(outputFolder, testSuiteFileName.getAsPath(format))

            Files.createDirectories(path.parent)
            Files.deleteIfExists(path)
            Files.createFile(path)

            path.toFile().appendText(testFileContent)
        }


        private fun header(format: OutputFormat, name: TestSuiteFileName, buffer: StringBuilder){

            if(name.hasPackage() && format.isJavaOrKotlin()){
                buffer.append("package "+name.getPackage())
                addSemicolon(format, buffer)
                newLine(buffer)
            }

            //TODO add generate comment with EvoMaster note and time

            newLines(2, buffer)

            if(format.isJUnit5()){
                addImport("org.junit.jupiter.api.AfterAll", buffer, format)
                addImport("org.junit.jupiter.api.BeforeAll", buffer, format)
                addImport("org.junit.jupiter.api.BeforeEach", buffer, format)
            }

            newLines(2, buffer)

            if(format.isJavaOrKotlin()){
                defineClass(format, name, buffer)
            }
        }


        private fun footer(buffer: StringBuilder){

            newLines(2, buffer);
            buffer.append("}")
        }

        private fun defineClass(format: OutputFormat, name: TestSuiteFileName, buffer: StringBuilder){

            when{
                format.isJava() -> buffer.append("public ")
                format.isKotlin() -> buffer.append("internal ")
            }

            buffer.append("class ${name.getClassName()} {")
        }

        private fun addImport(klass: String, buffer: StringBuilder, format: OutputFormat){

            buffer.append("import $klass")
            addSemicolon(format, buffer)
            newLine(buffer)
        }

        private fun newLines(n: Int, buffer: StringBuilder){
            if(n <= 0){
                throw IllegalArgumentException("Invalid n=$n")
            }

            (1..n).forEach { newLine(buffer) }
        }

        private fun newLine(buffer: StringBuilder){
            buffer.append("\n")
        }

        private fun addSemicolon(format: OutputFormat, buffer: StringBuilder) {
            if(format.isJava()){
                buffer.append(";")
            }
        }
    }
}
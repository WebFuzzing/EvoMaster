package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Files
import java.nio.file.Path

abstract class JvmDtoOutput: DtoOutput {

    protected fun setPackage(lines: Lines, suitePackage: String) {
        val pkgPrefix = if (suitePackage.isNotEmpty()) "$suitePackage." else ""
        lines.addStatement("package ${pkgPrefix}dto")
        lines.addEmpty()
    }

    protected fun addImports(lines: Lines) {
        lines.addStatement("import java.util.HashMap")
        lines.addStatement("import java.util.List")
        lines.addStatement("import java.util.Map")
        lines.addStatement("import java.util.Optional")
        lines.addEmpty()
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonAnyGetter")
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonAnySetter")
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonIgnore")
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonInclude")
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonProperty")
        lines.addEmpty()
    }

    protected fun appendDtoPackage(name: String): String {
        return "dto.$name"
    }

    protected fun getTestSuitePath(testSuitePath: Path, dtoFilename: TestSuiteFileName, outputFormat: OutputFormat) : Path{
        return testSuitePath.resolve(dtoFilename.getAsPath(outputFormat))
    }

    protected fun saveToDisk(testFileContent: String, path: Path) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }

    fun capitalizeFirstChar(word: String) : String {
        if(word.isEmpty()){
            return word
        }

        return word.substring(0, 1).uppercase() +
                word.substring(1)
    }

}

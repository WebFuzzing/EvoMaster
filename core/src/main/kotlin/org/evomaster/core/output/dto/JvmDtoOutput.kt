package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Files
import java.nio.file.Path

abstract class JvmDtoOutput: DtoOutput {

    val customControlCharMapperFactory = "CustomControlCharMapperFactory"

    protected fun setPackage(lines: Lines, suitePackage: String) {
        val pkgPrefix = if (suitePackage.isNotEmpty()) "$suitePackage." else ""
        lines.addStatement("package ${pkgPrefix}dto")
        lines.addEmpty()
    }

    protected fun addImports(lines: Lines) {
        lines.addStatement("import java.util.List")
        lines.addStatement("import java.util.Optional")
        lines.addEmpty()
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonInclude")
        lines.addStatement("import com.fasterxml.jackson.annotation.JsonProperty")
        lines.addEmpty()
    }

    protected fun addMapperImports(lines: Lines) {
        lines.addStatement("import com.fasterxml.jackson.core.SerializableString")
        lines.addStatement("import com.fasterxml.jackson.core.io.CharacterEscapes")
        lines.addStatement("import com.fasterxml.jackson.databind.ObjectMapper")
        lines.addStatement("import com.fasterxml.jackson.datatype.jdk8.Jdk8Module")
        lines.addEmpty()
        lines.addStatement("import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory")
        lines.addStatement("import java.lang.reflect.Type")
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

}

package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Path

class KotlinDtoOutput(val outputFormat: OutputFormat): JvmDtoOutput() {

    override fun writeClass(testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        declareDtoClass(lines, dtoFilename.getClassName(), dtoClass)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, dtoFilename, outputFormat))
    }

    override fun writeObjectMapperClass(testSuitePath: Path, testSuitePackage: String) {
        val mapperFilename = TestSuiteFileName(appendDtoPackage(customControlCharMapperFactory))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addMapperImports(lines)
        addMapperContentClass(lines)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, mapperFilename, outputFormat))
    }

    override fun getNewObjectStatement(dtoName: String, dtoVarName: String): String {
        return "val $dtoVarName = $dtoName()"
    }

    override fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String {
        return "$dtoVarName.${attributeName} = $value"
    }

    override fun getNewListStatement(listType: String, listVarName: String): String {
        return "val $listVarName = mutableListOf<$listType>()"
    }

    override fun getAddElementToListStatement(listVarName: String, value: String): String {
        return "$listVarName.add($value)"
    }

    private fun declareDtoClass(lines: Lines, dtoFilename: String, dtoClass: DtoClass) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("class $dtoFilename(")
        addVariables(lines, dtoClass)
        lines.add(")")
    }

    private fun addVariables(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.name}\")")
                lines.add("var ${it.name}: ${it.type}? = null,")
            }
            lines.addEmpty()
        }
    }

    private fun addMapperContentClass(lines: Lines) {
        lines.add("class $customControlCharMapperFactory : Jackson2ObjectMapperFactory {")
        lines.addEmpty()
        lines.indented {
            lines.add("override fun create(cls: Type, charset: String): ObjectMapper {")
            lines.indented {
                lines.add("val mapper = ObjectMapper()")
                lines.add("mapper.registerModule(Jdk8Module())")
                lines.add("mapper.factory.setCharacterEscapes(NoEscapeControlChars())")
                lines.add("return mapper")
            }
            lines.add("}")
        }
        lines.addEmpty()
        lines.add("}")
        lines.addEmpty()
        lines.add("class NoEscapeControlChars : CharacterEscapes() {")
        lines.addEmpty()
        lines.indented {
            lines.add("private val escapes: IntArray")
            lines.addEmpty()
            lines.add("init {")
            lines.indented {
                lines.add("val std = CharacterEscapes.standardAsciiEscapesForJSON()")
                lines.add("escapes = std.copyOf(std.size)")
                lines.add("for (i in 0..0x1f) {")
                lines.indented {
                    lines.add("escapes[i] = CharacterEscapes.ESCAPE_NONE")
                }
                lines.add("}")
//                for (i in 0..0x1f) {
//                    lines.add("escapes[$i] = CharacterEscapes.ESCAPE_NONE")
////                    escapes[i] = CharacterEscapes.ESCAPE_NONE
//                }
            }
            lines.add("}")
            lines.addEmpty()
            lines.add("override fun getEscapeCodesForAscii(): IntArray {")
            lines.indented {
                lines.add("return escapes")
            }
            lines.add("}")
            lines.addEmpty()
            lines.add("override fun getEscapeSequence(ch: Int): SerializableString? {")
            lines.indented {
                lines.add("return null")
            }
            lines.add("}")
            lines.addEmpty()
        }
        lines.add("}")
    }

}

package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.utils.StringUtils
import java.nio.file.Path

class JavaDtoOutput(val outputFormat: OutputFormat): JvmDtoOutput() {

    override fun writeClass(testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        initDtoClass(lines, dtoFilename.getClassName())
        addClassContent(lines, dtoClass)
        closeClass(lines)
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
        return "$dtoName $dtoVarName = new $dtoName();"
    }

    override fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String {
        return "$dtoVarName.set${StringUtils.capitalization(attributeName)}($value);"
    }

    override fun getNewListStatement(listType: String, listVarName: String): String {
        return "List<$listType> $listVarName = new ArrayList<$listType>();"
    }

    override fun getAddElementToListStatement(listVarName: String, value: String): String {
        return "$listVarName.add($value);"
    }

    private fun initDtoClass(lines: Lines, dtoFilename: String) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("public class $dtoFilename {")
        lines.addEmpty()
    }

    private fun closeClass(lines: Lines) {
        lines.add("}")
    }

    private fun addClassContent(lines: Lines, dtoClass: DtoClass) {
        addVariables(lines, dtoClass)
        addGettersAndSetters(lines, dtoClass)
    }

    private fun addVariables(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.name}\")")
                lines.add("private Optional<${it.type}> ${it.name};")
            }
            lines.addEmpty()
        }
    }

    private fun addGettersAndSetters(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            val varName = it.name
            val varType = it.type
            val capitalizedVarName = StringUtils.capitalization(varName)
            lines.indented {
                lines.add("public Optional<${varType}> get${capitalizedVarName}() {")
                lines.indented {
                    lines.add("return ${varName};")
                }
                lines.add("}")
                lines.addEmpty()
                lines.add("public void set${capitalizedVarName}(${varType} ${varName}) {")
                lines.indented {
                    lines.add("this.${varName} = Optional.ofNullable(${varName});")
                }
                lines.add("}")
            }
            lines.addEmpty()
        }
    }

    private fun addMapperContentClass(lines: Lines) {
        lines.add("public class $customControlCharMapperFactory implements Jackson2ObjectMapperFactory {")
        lines.addEmpty()
        lines.indented {
            lines.add("@Override")
            lines.add("public ObjectMapper create(Type cls, String charset) {")
            lines.indented {
                lines.add("ObjectMapper mapper = new ObjectMapper();")
                lines.add("mapper.registerModule(new Jdk8Module());")
                lines.add("mapper.getFactory().setCharacterEscapes(new NoEscapeControlChars());")
                lines.add("return mapper;")
            }
            lines.add("}")
        }
        lines.addEmpty()
        lines.add("}")
        lines.addEmpty()
        lines.add("class NoEscapeControlChars extends CharacterEscapes {")
        lines.addEmpty()
        lines.indented {
            lines.add("private final int[] escapes;")
            lines.addEmpty()
            lines.add("public NoEscapeControlChars() {")
            lines.indented {
                lines.add("int[] std = CharacterEscapes.standardAsciiEscapesForJSON();")
                lines.add("escapes = java.util.Arrays.copyOf(std, std.length);")
                lines.add("escapes[0x1F] = CharacterEscapes.ESCAPE_NONE;")
            }
            lines.add("}")
            lines.addEmpty()
            lines.add("@Override")
            lines.add("public int[] getEscapeCodesForAscii() {")
            lines.indented {
                lines.add("return escapes;")
            }
            lines.add("}")
            lines.addEmpty()
            lines.add("@Override")
            lines.add("public SerializableString getEscapeSequence(int ch) {")
            lines.indented {
                lines.add("return null;")
            }
            lines.add("}")
            lines.addEmpty()
        }
        lines.add("}")
    }
}

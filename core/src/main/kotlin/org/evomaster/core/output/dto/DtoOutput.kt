package org.evomaster.core.output.dto

import org.evomaster.core.output.OutputFormat
import java.nio.file.Path

/**
 * When [EMConfig.dtoForRequestPayload] is enabled DTO classes will be written in the filesystem under the `dto`
 * package. These DTO classes will then be used for test case writing for JSON request payloads. Instead of having a
 * stringified view of the payload, EM will leverage these DTOs.
 */
interface DtoOutput {

    /**
     * Writes a DTO class in the corresponding [org.evomaster.core.output.OutputFormat].
     *
     * @param outputFormat under which the java class must be written
     * @param testSuitePath under which the java class must be written
     * @param testSuitePackage under which the java class must be written
     * @param dtoClass to be written to filesystem
     */
    fun writeClass(outputFormat: OutputFormat, testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass)

    /**
     * Writes an ObjectMapper class in the corresponding [org.evomaster.core.output.OutputFormat] for
     * Jackson/RestAssured to use when serializing DTOs.
     *
     * @param testSuitePath under which the class must be written
     * @param testSuitePackage under which the class must be written
     */
    fun writeObjectMapperClass(outputFormat: OutputFormat, testSuitePath: Path, testSuitePackage: String)

    /**
     * @param dtoName that will be instantiated for payload
     * @param dtoVarName variable name under which the DTO Object will be instantiated
     *
     * @return the object initialization statement
     */
    fun getNewObjectStatement(dtoName: String, dtoVarName: String): String

    /**
     * @param dtoVarName variable name in which values will be set
     * @param attributeName being set
     * @param value being set to the DTO in [dtoVarName]
     *
     * @return the attribute set statement
     */
    fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String

    /**
     * @param listType that the list will hold
     * @param listVarName variable name under which the list will be instantiated
     *
     * @return the list initialization statement
     */
    fun getNewListStatement(listType: String, listVarName: String): String

    /**
     * @param listVarName variable in which elements will be added
     * @param value being added to the list in [listVarName]
     *
     * @return the list add statement
     */
    fun getAddElementToListStatement(listVarName: String, value: String): String

}

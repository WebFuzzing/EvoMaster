package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import java.nio.file.Path

/**
 * When [EMConfig.dtoForRequestPayload] is enabled DTO classes will be written in the filesystem under the `dto`
 * package. These DTO classes will then be used for test case writing for JSON request payloads. Instead of having a
 * stringified view of the payload, EM will leverage these DTOs.
 */
interface DtoOutput {

    /**
     * @param testSuitePath under which the java class must be written
     * @param testSuitePackage under which the java class must be written
     * @param outputFormat forwarded to the [Lines] helper class and for setting the .java extension in the generated file
     * @param dtoClass to be written to filesystem
     */
    fun writeClass(testSuitePath: Path, testSuitePackage: String, outputFormat: OutputFormat, dtoClass: DtoClass)

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

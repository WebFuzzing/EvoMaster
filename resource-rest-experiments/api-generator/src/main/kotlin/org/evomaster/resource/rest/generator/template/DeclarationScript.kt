package org.evomaster.resource.rest.generator.template

/**
 * created by manzh on 2019-08-13
 */
interface DeclarationScript : ScriptTemplate {

    fun getType() : String

    fun getDefaultValue() : String?

    fun generateDefaultVarName() : String = getName()+"_var"

    fun generateAsVarOfConstructor(types : RegisterType) : String

    fun generateSetterStatement(varName : String) : String

    fun getTags() : List<String>


}
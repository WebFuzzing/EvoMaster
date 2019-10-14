package org.evomaster.resource.rest.generator.template

/**
 * created by manzh on 2019-08-13
 */
interface ScriptTemplate {

    fun getName() : String

    fun getBoundary() : Boundary

    fun isStatic() : Boolean = false

    fun isFinal() : Boolean = false

    fun generate(types : RegisterType) : String

}
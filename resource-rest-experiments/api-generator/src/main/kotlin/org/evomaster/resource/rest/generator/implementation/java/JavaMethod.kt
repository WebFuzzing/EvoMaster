package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-15
 */
abstract class JavaMethod : MethodScript, JavaTemplate {

    override fun generateHeading(types: RegisterType): String =
            formatMethodHeading(getBoundary(), isStatic(), getReturn()?.run{types.getType(this)}, getName(),getParams().map { Pair(it.key, types.getType(it.value)) }.toMap(), getParamTag())

    override fun generateEnding(types: RegisterType): String = methodEnd()

    override fun getInvocation(obj: String?, paramVars: List<String>): String = formatMethodInvocation(obj, getName(), paramVars)

    override fun getIfSnippets(): List<IfSnippet> = listOf()

}
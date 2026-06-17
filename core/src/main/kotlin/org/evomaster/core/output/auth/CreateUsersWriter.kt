package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.auth.CookieWriter.cookiesName
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.CreateUsers
import org.evomaster.core.problem.httpws.auth.Generator
import org.evomaster.core.search.Individual
import org.evomaster.test.utils.EMTestUtils
import kotlin.collections.set

object CreateUsersWriter {

    fun generatorName(name: String, g: Generator): String =
        TestWriterUtils.safeVariableName("generator_${name}_${g.placeHolder}")

    fun getCreateUsersForNamedAuth(name: String, ind: Individual): CreateUsers? {

        return ind.seeAllActions()
            .filterIsInstance<HttpWsAction>()
            .find { it.auth.name == name }
            ?.auth?.createUsers
    }

    fun getCreateUsersAuth(ind: Individual) = ind.seeAllActions()
        .filterIsInstance<HttpWsAction>()
        .filter { it.auth.createUsers != null }
        .distinctBy { it.auth.name }
        .map { it.auth.createUsers!! }


    fun handleCreateUsers(user: CreateUsers, format: OutputFormat, lines: Lines) {

        for(g in user.generators) {

            when {
                format.isJava() -> lines.add("final String ${generatorName(user.name, g)} = ")
                format.isKotlin() -> lines.add("val ${generatorName(user.name, g)} : String = ")
                format.isJavaScript() -> lines.add("const ${generatorName(user.name, g)} = ")
                format.isPython() -> lines.add("${generatorName(user.name, g)} = ")
            }

            val min = g.minLength
            val max = g.maxLength
            val prefix = if(g.prefix == null) null else "\"${g.prefix}\""
            val postfix = if(g.postfix == null) null else "\"${g.postfix}\""

            lines.append(" EMTestUtils.createString($min, $max, $prefix, $postfix)")
            lines.appendSemicolon()
        }


    }
}
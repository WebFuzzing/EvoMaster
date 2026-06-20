package org.evomaster.core.output.auth

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.service.HttpWsTestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.CreateUsers
import org.evomaster.core.problem.httpws.auth.Generator
import org.evomaster.core.problem.httpws.auth.PlaceHolderResolver
import org.evomaster.core.search.Individual


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


    /**
     * If needed, make call to create a new user.
     */
    fun handleCreateUsers(
        name: String,
        ind: Individual,
        format: OutputFormat,
        lines: Lines,
        testCaseWriter: HttpWsTestCaseWriter,
        baseUrlOfSut: String
    ) : PlaceHolderResolver? {

        val user = getCreateUsersForNamedAuth(name, ind)
            ?: return null

        val resolverData = mutableMapOf<String, String>()

        for(g in user.generators) {

            val variableName = generatorName(user.name, g)
            resolverData[g.placeHolder] = variableName

            when {
                format.isJava() -> lines.add("final String $variableName = ")
                format.isKotlin() -> lines.add("val $variableName : String = ")
                format.isJavaScript() -> lines.add("const $variableName = ")
                format.isPython() -> lines.add("$variableName = ")
            }

            val min = g.minLength
            val max = g.maxLength
            val prefix = if(g.prefix == null) null else "\"${g.prefix}\""
            val postfix = if(g.postfix == null) null else "\"${g.postfix}\""

            if (format.isJavaScript()) {
                lines.append("${TestSuiteWriter.jsImport}.")
            }
            when{
                // function in EMTestUtils
                format.isPython() -> lines.append("create_string")
                else -> lines.append("createString")
            }
            lines.append("($min, $max, $prefix, $postfix)")
            lines.appendSemicolon()
        }
        lines.addEmpty()

        val infoMsg = "Create new user dynamically for ${user.name}"

        if(! format.isPython()){
            lines.addEmpty()
            testCaseWriter.startRequest(lines)
            lines.appendSingleCommentLine(infoMsg)
            lines.indent(2)
        } else {
            lines.addSingleCommentLine(infoMsg)
        }

        AuthWriter.addBodyOfCallCommand(lines, user.call, testCaseWriter, format, baseUrlOfSut, null, null)
        lines.appendSemicolon()

        if (! format.isPython()){
            lines.deindent(2)
        }
        lines.addEmpty()

        return PlaceHolderResolver(user.name, resolverData)
    }
}
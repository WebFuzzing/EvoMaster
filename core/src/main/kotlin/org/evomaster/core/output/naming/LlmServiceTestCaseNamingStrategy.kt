package org.evomaster.core.output.naming

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.llm.Prompts.RE_ITERATE_TEST_CASE_NAME
import org.evomaster.core.llm.Prompts.getPromptForTestCaseName
import org.evomaster.core.llm.service.LlmService
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LlmServiceTestCaseNamingStrategy(
    solution: Solution<*>,
    private val outputFormat: OutputFormat,
    private val llmService: LlmService,
    maxTestCaseNameLength: Int,
    private val testCaseWriter: TestCaseWriter
) : NumberedTestCaseNamingStrategy(solution) {

    private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)
    private val fallbackLlmTestCaseName = "llmInteractionFailed_reviewName"
    private val generatedNames = mutableSetOf<String>()

    private val remainingNameChars = maxTestCaseNameLength - namePrefixChars()

    override fun expandName(
        individual: EvaluatedIndividual<*>,
        nameTokens: MutableList<String>,
        ambiguitySolvers: List<AmbiguitySolver>
    ): String {
        //for debugging time issues
//        val newName = TimeUtils.measureTimeMillis(
//            { ms, _ -> println("LLM took: ${ms}ms") },
//            {generateLlmName(TestCase(individual, "test"))}
//        )

        val newName = generateLlmName(TestCase(individual, "test"))

        return if (newName.isNotEmpty()) "_$newName" else ""
    }

    private fun generateLlmName(test: TestCase): String {
        var newName = try {
            getNewName(test)
        }catch (e:Exception){
            return fallbackLlmTestCaseName
        }

        if(newName == null){
            newName = promptReIterateName()
                ?:
                return fallbackLlmTestCaseName
        }

        newName = sanitizeName(newName)
        org.evomaster.core.Lazy.assert { isValidSuffix(newName) }

        generatedNames.add(newName)
        return newName
    }

    // LLM is sometimes returning names as "\n\ntheNewName_" so we need to fix that and return "theNewName".
    private fun sanitizeName(testName: String): String {

        val name = testName.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
            .replace(" ", "")
            .replace("'", "")
            .replace("\"","")
        if(name.isBlank()){
            return fallbackLlmTestCaseName
        }

        return TestWriterUtils.safeVariableName(name, "_")
    }

    private fun getNewName(test: TestCase): String? {
        val testLines = getTestSourceCode(test)
        val targetLanguage = getTargetLanguage()
        val prompt = getPromptForTestCaseName(targetLanguage, remainingNameChars, generatedNames, testLines.toString())
        val data = llmService.chat(prompt.first, prompt.second)
        return extractResponse(data)
    }

    private fun extractResponse(data: String): String? {
        if(data.isBlank()){
            return null
        }
        val mapper = ObjectMapper()
        val json = try {
            mapper.readTree(data)
        } catch (e: JsonProcessingException) {
            /*
                Not a JSON response, not even a JSON string.
                but what if it is an unquoted word? we could use it then
             */
            val trimmed = data.trim()
            if(trimmed.isNotEmpty() && !trimmed.contains(' ')){
                return trimmed
            }
            return null
        }
        if (json.isTextual) {
            return data
        } else if (json.isObject) {
            val fields = json.fields().asSequence().toList()
            if (fields.size == 1) {
                return fields[0].value.textValue()
            }
        }
        return null
    }

    // Just in case the LLM did not follow the directive of just giving the new name as output.
    private fun promptReIterateName(): String? {
        val data = llmService.chat(RE_ITERATE_TEST_CASE_NAME)
        return extractResponse(data)
    }

    // With this regex, we check that the output by the LLM is only the test case name. We validate:
    //  1. Whitespace check — if the output contains a newline, it's not a bare name.
    //  2. Word count — split on spaces
    //  3. Illegal character check — a valid name contains only alphanumeric characters and underscores
    //  4. Length check — if the output exceeds it, it's invalid regardless of format.
    private fun isValidSuffix(output: String): Boolean {
        val stripped = output.trim()
        return stripped.matches(Regex("[A-Za-z0-9_]+")) && stripped.length <= remainingNameChars
    }

    private fun getTargetLanguage(): String {
        return when {
            outputFormat.isJava() -> "Java"
            outputFormat.isKotlin() -> "Kotlin"
            outputFormat.isJavaScript() -> "JavaScript"
            outputFormat.isPython() -> "Python"
            else -> throw IllegalStateException("Unrecognized language: $outputFormat")
        }
    }

    private fun getTestSourceCode(test: TestCase): Lines {
        return try {
            testCaseWriter.convertToCompilableTestCode(test, TestSuiteWriter.baseUrlOfSut, null)
        } catch (ex: Exception) {
            log.warn(
                "A failure has occurred in generating test code ${test.name} for LLM naming strategy. \n "
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
            )
            assert(false) // in our tests, this should not happen... but should not crash in production
            Lines(outputFormat)
        }
    }
}

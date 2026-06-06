package org.evomaster.core.output.naming

import com.google.inject.Inject
import org.evomaster.core.llm.service.LlmService
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LLMServiceTestCaseNamingStrategy(
    solution: Solution<*>,
    private val outputFormat: OutputFormat,
    private val llmService: LlmService,
    private val maxTestCaseNameLength: Int
) : NumberedTestCaseNamingStrategy(solution) {

    private val validNameRegex = Regex("[A-Za-z0-9_]+")

    @Inject
    private lateinit var testCaseWriter: TestCaseWriter

    private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)

    private val baseUrlOfSut = "baseUrlOfSut"
    private val fixture = "_fixture"
    private val remainingNameChars = maxTestCaseNameLength - namePrefixChars()

    override fun getTestCases(): List<TestCase> {
        val testCases = super.getTestCases()
        return generateLlmNames(testCases)
    }

    override fun expandName(
        individual: EvaluatedIndividual<*>,
        nameTokens: MutableList<String>,
        ambiguitySolvers: List<AmbiguitySolver>
    ): String {
        return super.expandName(individual, nameTokens, ambiguitySolvers)
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        return super.resolveAmbiguities(duplicatedIndividuals)
    }

    private fun generateLlmNames(tests: List<TestCase>): List<TestCase> {
        return tests.map { test ->
            return solution.individuals.map { TestCase(it, generateLlmName(test)) }
        }
    }

    private fun generateLlmName(test: TestCase): String {
        var newName = getNewName(test)
        while (!isValidSuffix(newName)) {
            newName = promptReIterateName()
        }
        return newName
    }

    private fun getNewName(test: TestCase): String {
        val testLines = getTestSourceCode(test)
        val targetLanguage = getTargetLanguage()
        return llmService.chat("""
            You are an expert software engineer specializing in test naming.

            Given the following test case written in $targetLanguage, produce a descriptive suffix to append to its existing name.

            ## Rules

            - The suffix must follow the naming conventions of $targetLanguage (e.g. snake_case for Python, camelCase for Java).
            - The suffix must be derived directly from the code: what unit is being exercised, under what conditions, and what outcome is asserted.
            - The suffix should follow the pattern `<method/feature>_<condition>_<expectedOutcome>` or a close variant natural to $targetLanguage test frameworks. Pick whichever fits the test best.
            - Use only information present in the test body — do not invent context.
            - Be specific: prefer `createUser_duplicateEmail_throwsConflict` over `createUser_fails`.
            - Do not include words like "test", "check", "verify", or "ensure" in the suffix.
            - The suffix must not exceed $remainingNameChars characters.
            - Output only the suffix, nothing else.

            ## Language

            $targetLanguage

            ## Max suffix length

            $remainingNameChars characters

            ## Test case

            $testLines

        """.trimIndent())
    }

    private fun promptReIterateName(): String {
        return llmService.chat("Your previous response contained more than just the suffix. Output only the suffix, nothing else. No explanation, no punctuation, no extra text.")
    }

    // With this regex, we check that the output by the LLM is only the test case name. We validate:
    //  1. Whitespace check — if the output contains a newline, it's not a bare name.
    //  2. Word count — split on spaces
    //  3. Illegal character check — a valid name contains only alphanumeric characters and underscores
    //  4. Length check — if the output exceeds it, it's invalid regardless of format.
    private fun isValidSuffix(output: String): Boolean {
        val stripped = output.trim()
        return stripped.matches(validNameRegex) && stripped.length <= remainingNameChars
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
            if (outputFormat.isCsharp())
                testCaseWriter.convertToCompilableTestCode(test, "$fixture.$baseUrlOfSut", null)
            else
                testCaseWriter.convertToCompilableTestCode(test, baseUrlOfSut, null)
        } catch (ex: Exception) {
            log.warn(
                "A failure has occurred in writing test ${test.name}. \n "
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. "
            )
            assert(false) // in our tests, this should not happen... but should not crash in production
            Lines(outputFormat)
        }
    }
}

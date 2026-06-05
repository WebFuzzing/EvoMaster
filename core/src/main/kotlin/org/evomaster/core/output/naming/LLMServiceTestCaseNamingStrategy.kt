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
    private val llmService: LlmService
) : NumberedTestCaseNamingStrategy(solution) {

    @Inject
    private lateinit var testCaseWriter: TestCaseWriter

    private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)

    private val baseUrlOfSut = "baseUrlOfSut"
    private val fixture = "_fixture"

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
        val testLines = getTestSourceCode(test)
        val newName = llmService.chat("""
          You are a professional java programmer. The ultimate goal is to improve the readability of the 
          test case I will send you, only by returning a new test case name.
          
          Improve the readability of the test below by returning ONLY the 
          test name, NOT THE FUNCTIONS CALLED INSIDE THE TESTS, STATIC METHOD, 
          CALLED STATIC CLASS, VARIABLE NAMES NOR IDENTIFIERS.
          You must use test case code to determine a more descriptive test case name.
          When naming, you should not mention the goal of readability improvement,
          we're looking for real case test name improvement. Remember in test  
          naming it is important that:
          - Names be descriptive, there must be a relationship between the 
            written code and the test case name.
          - Names should distinguish uniquely a test case from another.
          - Names allow us to understand which part of the source code is being
            tested. In this case, which part of the API described by the API spec.
          --------------------------------------------------------------------------------------------------
          Test to name:
          
          $testLines
          --------------------------------------------------------------------------------------------------
                            
          Answer with name only.
        """)
        // re-iterate until the name starts with `test_#_{NAME}`
        return newName
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

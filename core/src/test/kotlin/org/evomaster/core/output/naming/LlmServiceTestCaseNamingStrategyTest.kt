package org.evomaster.core.output.naming

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.llm.mock.MockChatModel
import org.evomaster.core.llm.service.LlmService
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.service.GraphQLTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList
import kotlin.jvm.java

class LlmServiceTestCaseNamingStrategyTest {

    companion object {
        // Setting a shorter name length just to force a condition in which the LLM response does not comply
        // with the ask since the mock will return a longer name, we force the re-prompt and later the fallback name
        const val MAX_NAME_LENGTH = 40
    }

    @Test
    fun testNameTestInPython() {
        val outputFormat = OutputFormat.PYTHON_UNITTEST
        val baseModule = BaseModule(arrayOf("--llm", "true", "--llmProvider", "MOCK", "--outputFormat", outputFormat.name))
        val llmService = getLlmService(baseModule)
        val graphTestCaseWriter = getTestCaseWriter(baseModule.getEMConfig())

        MockChatModel.reset()
        MockChatModel.mockResponse("this_test_name_was_llm_generated") { it.contains("[targetLanguage]:Python") }
        MockChatModel.mockResponse("thisTestNameWasLlmGenerated") { it.contains("[targetLanguage]:Java") }

        val testCases = generateTestCases(outputFormat, llmService, graphTestCaseWriter)

        assertEquals(1, testCases.size)
        assertEquals("test_0_this_test_name_was_llm_generated", testCases[0].name)
    }

    @Test
    fun testNameTestInJava() {
        val outputFormat = OutputFormat.JAVA_JUNIT_4
        val baseModule = BaseModule(arrayOf("--llm", "true", "--llmProvider", "MOCK", "--outputFormat", outputFormat.name))
        val llmService = getLlmService(baseModule)
        val graphTestCaseWriter = getTestCaseWriter(baseModule.getEMConfig())

        MockChatModel.reset()
        MockChatModel.mockResponse("this_test_name_was_llm_generated") { it.contains("[targetLanguage]:Python") }
        MockChatModel.mockResponse("thisTestNameWasLlmGenerated") { it.contains("[targetLanguage]:Java") }

        val testCases = generateTestCases(outputFormat, llmService, graphTestCaseWriter)

        assertEquals(1, testCases.size)
        assertEquals("test_0_thisTestNameWasLlmGenerated", testCases[0].name)
    }

    @Test
    fun testFallbackNameIsAssignedAfterTwoFailedPrompts() {
        val outputFormat = OutputFormat.JAVA_JUNIT_4
        val baseModule = BaseModule(arrayOf("--llm", "true", "--llmProvider", "MOCK", "--outputFormat", outputFormat.name))
        val llmService = getLlmService(baseModule)
        val graphTestCaseWriter = getTestCaseWriter(baseModule.getEMConfig())

        MockChatModel.reset()
        MockChatModel.mockResponse("[\"aNameThatExceeds The Amount of Characters and has ! f\"]") { it.contains("[targetLanguage]:Java") }
        MockChatModel.mockResponse("\"This name+ will !!! also have over 40chars and shouldBe_rejected") { it.contains("Your previous response") }

        val testCases = generateTestCases(outputFormat, llmService, graphTestCaseWriter)

        assertEquals(1, testCases.size)
        assertEquals("test_0_llmInteractionFailed_reviewName", testCases[0].name)
    }

    private fun getLlmService(baseModule: BaseModule): LlmService {
        val injector: Injector = LifecycleInjector.builder()
            .withModules(listOf<com.google.inject.Module>(baseModule))
            .build().createInjector()
        return injector.getInstance(LlmService::class.java)
    }

    private fun getTestCaseWriter(emConfig: EMConfig): TestCaseWriter {
        val graphTestCaseWriter = GraphQLTestCaseWriter()
        val field = TestCaseWriter::class.java.getDeclaredField("config")
        field.isAccessible = true
        field.set(graphTestCaseWriter, emConfig)
        return graphTestCaseWriter
    }

    private fun generateTestCases(outputFormat: OutputFormat, llmService: LlmService, testCaseWriter: TestCaseWriter): List<TestCase> {
        val eIndividual = getEvaluatedIndividual(GQMethodType.MUTATION)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = LlmServiceTestCaseNamingStrategy(solution, outputFormat, llmService, MAX_NAME_LENGTH, testCaseWriter)
        return namingStrategy.getTestCases()
    }

    private fun getEvaluatedIndividual(query: GQMethodType): EvaluatedIndividual<GraphQLIndividual> {
        val sampleType = SampleType.RANDOM
        val op = OptionalGene("foo", StringGene("foo", "foo\""))
        val param = GQInputParam("foo", op)
        val action = GraphQLAction("Mutation:add", "add", query, mutableListOf(param))

        val actions = mutableListOf<EnterpriseActionGroup<*>>()
        actions.add(EnterpriseActionGroup(mutableListOf(action),GraphQLAction::class.java))
        val individual = GraphQLIndividual(sampleType, actions)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val result = GraphQlCallResult(action.getLocalId())
        val results = listOf(result)
        return EvaluatedIndividual(fitnessVal, individual, results)
    }

}

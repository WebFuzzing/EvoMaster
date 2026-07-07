package org.evomaster.e2etests.spring.openapi.v3.dictionaryllm

import com.foo.rest.examples.spring.openapi.v3.dictionaryllm.DictionaryLlmController
import org.evomaster.core.llm.LlmProvider
import org.evomaster.core.llm.mock.MockChatModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class DictionaryLlmEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DictionaryLlmController())
        }
    }


    @Test
    fun testRunEM() {

        defaultSeed = 43

        runTestHandlingFlakyAndCompilation(
                "DictionaryLlmEM",
                100
        ) { args: MutableList<String> ->

            //White-box would trivially cover it via taint analysis
            setOption(args, "blackBox", "true")
            setOption(args, "base", baseUrlOfSut)
            setOption(args, "schema", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "useDictionaryDataPool", "true")
            setOption(args, "llm", "true")
            setOption(args, "llmProvider", LlmProvider.MOCK.name)

            MockChatModel.reset()
            MockChatModel.mockResponse("[\"triceratops\"]"){ it.contains("giganotosaurus")}

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/dictionaryllm", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/dictionaryllm", "OK")
        }
    }
}
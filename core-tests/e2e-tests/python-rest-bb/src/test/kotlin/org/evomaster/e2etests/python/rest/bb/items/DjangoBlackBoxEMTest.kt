package org.evomaster.e2etests.python.rest.bb.items

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.python.rest.bb.DjangoSutController
import org.evomaster.e2etests.python.rest.bb.ExternalSutBlackBoxTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DjangoBlackBoxEMTest : ExternalSutBlackBoxTestBase() {

    companion object {
        private lateinit var sut: DjangoSutController

        @BeforeAll
        @JvmStatic
        fun startDjango() {
            sut = DjangoSutController()
            sut.start()
            configureSut(sut.baseUrl, sut.schemaPath)
        }

        @AfterAll
        @JvmStatic
        fun stopDjango() {
            if (::sut.isInitialized) {
                sut.stop()
            }
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testItemsCrud(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(outputFormat, "django_items", 20, 5) { args ->
            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/items", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/items", null)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testSecureAuth(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(outputFormat, "django_secure", 20, 5) { args ->
            setOption(args, "configPath", "src/test/resources/config/python_auth.yaml")
            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/secure", "granted")
        }
    }
}

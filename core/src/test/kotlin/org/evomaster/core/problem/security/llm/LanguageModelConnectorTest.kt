package org.evomaster.core.problem.security.llm

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll

class LanguageModelConnectorTest {

    private lateinit var config: EMConfig
    private lateinit var languageModelConnector: LanguageModelConnector

    @BeforeAll
    fun skipOnGA() {
        CIUtils.skipIfOnGA()
    }

    @Test
    fun testLlm() {

        val injector: Injector = LifecycleInjector.builder()
            .withModules(BaseModule())
            .build().createInjector()

        config = injector.getInstance(EMConfig::class.java)
        languageModelConnector = injector.getInstance(LanguageModelConnector::class.java)

        languageModelConnector.initialise()

        val answer = languageModelConnector.query("1+1? respond only the answer.")

        assertEquals(answer, "2")
    }
}

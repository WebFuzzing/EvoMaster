package org.evomaster.e2etests.spring.openapi.v3

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.instrumentation.InputProperties
import org.evomaster.client.java.instrumentation.InstrumentingAgent
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.BeforeAll

/**
 * Created by arcuri82 on 03-Mar-20.
 */
abstract class SpringTestBase : RestTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun initAgent() {
            /*
                needed because kotlin.jvm.internal.Intrinsics gets loaded in
                TaintKotlinEqualController before agent is initialized
             */
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0")
            InstrumentedSutStarter.loadAgent()
            InstrumentingAgent.changePackagesToInstrument("com.foo.")
        }
    }

}

package org.evomaster.e2etests.spring.multidb

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.instrumentation.InputProperties
import org.evomaster.client.java.instrumentation.InstrumentingAgent
import org.evomaster.core.sql.multidb.MultiDbUtils
import org.evomaster.driver.multidb.SpringController
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

abstract class MultiDbParameterizedE2ETemplate : RestTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun initAgent() {
            /*
                needed due to Kotlin loading before agent is initialized
             */
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0")
            InstrumentedSutStarter.loadAgent()
            InstrumentingAgent.changePackagesToInstrument("com.foo.")
        }

        @AfterAll
        @JvmStatic
        fun shutDown(){
            MultiDbUtils.stopAllDatabases()
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["MYSQL","POSTGRES","H2"])
    fun testRunEM(databaseType: DatabaseType) {

        val c = instantiateNewController()
        c.changeDatabaseType(databaseType)
        EnterpriseTestBase.initClass(c)

        runEM(databaseType)
    }


    protected abstract fun runEM(databaseType: DatabaseType)

    protected abstract fun instantiateNewController() : SpringController
}
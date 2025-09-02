package org.evomaster.core.problem.enterprise.service

import com.google.inject.AbstractModule
import org.evomaster.core.languagemodel.service.LanguageModelConnector
import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.evomaster.core.problem.security.service.HttpCallbackVerifier

abstract class EnterpriseModule : AbstractModule() {

    override fun configure() {
        super.configure()

        bind(WFCReportWriter::class.java)
            .asEagerSingleton()

        bind(LanguageModelConnector::class.java)
            .asEagerSingleton()

        bind(HttpCallbackVerifier::class.java)
            .asEagerSingleton()

        bind(SSRFAnalyser::class.java)
            .asEagerSingleton()
    }
}

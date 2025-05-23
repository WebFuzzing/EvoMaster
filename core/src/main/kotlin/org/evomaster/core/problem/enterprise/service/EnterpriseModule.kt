package org.evomaster.core.problem.enterprise.service

import com.google.inject.AbstractModule
import org.evomaster.core.languagemodel.service.LanguageModelConnector

abstract class EnterpriseModule :  AbstractModule() {

    override fun configure() {
        super.configure()

        bind(WFCReportWriter::class.java)
            .asEagerSingleton()

        bind(LanguageModelConnector::class.java)
            .asEagerSingleton()
    }
}

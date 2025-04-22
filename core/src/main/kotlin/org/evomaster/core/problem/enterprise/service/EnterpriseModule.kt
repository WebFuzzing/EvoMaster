package org.evomaster.core.problem.enterprise.service

import com.google.inject.AbstractModule

abstract class EnterpriseModule :  AbstractModule() {

    override fun configure() {
        super.configure()

        bind(WFCReportWriter::class.java)
            .asEagerSingleton()
    }
}
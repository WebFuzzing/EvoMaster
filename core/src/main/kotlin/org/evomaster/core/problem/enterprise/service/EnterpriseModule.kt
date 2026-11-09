package org.evomaster.core.problem.enterprise.service

import com.google.inject.AbstractModule
import org.evomaster.core.llm.service.DictionaryService
import org.evomaster.core.llm.service.LlmService
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.evomaster.core.problem.security.service.HttpCallbackVerifier
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation

abstract class EnterpriseModule : AbstractModule() {

    override fun configure() {
        super.configure()

        binder().requireExplicitBindings()

        bind(WFCReportWriter::class.java)
            .asEagerSingleton()

        bind(HttpCallbackVerifier::class.java)
            .asEagerSingleton()

        bind(OracleApplicability::class.java)
            .asEagerSingleton()

        bind(HttpWsExternalServiceHandler::class.java)
            .asEagerSingleton()

        bind(HarvestActualHttpWsResponseHandler::class.java)
            .asEagerSingleton()

    }

    /**
     * This is not on by default, as subclasses for black-box will not use it
     */
    fun bindRemoteController(){
        bind(RemoteController::class.java)
            .to(RemoteControllerImplementation::class.java)

        bind(RemoteControllerImplementation::class.java)
            .asEagerSingleton()
    }
}

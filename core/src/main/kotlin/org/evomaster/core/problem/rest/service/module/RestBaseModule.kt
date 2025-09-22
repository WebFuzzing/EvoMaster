package org.evomaster.core.problem.rest.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.problem.rest.service.HttpSemanticsService
import org.evomaster.core.problem.rest.service.RestIndividualBuilder
import org.evomaster.core.problem.rest.service.SecurityRest
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.seeding.service.rest.PirToRest

open class RestBaseModule : EnterpriseModule() {

    override fun configure() {
        super.configure()

        bind(TestCaseWriter::class.java)
            .to(RestTestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()

        bind(SecurityRest::class.java)
            .asEagerSingleton()

        bind(PirToRest::class.java)
            .asEagerSingleton()

        bind(RestIndividualBuilder::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<RestIndividual>>(){})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>(){})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<RestIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<RestIndividual>>() {})
            .asEagerSingleton()

        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<RestIndividual>>() {})
            .asEagerSingleton()

        bind(HttpSemanticsService::class.java)
            .asEagerSingleton()

        bind(AIResponseClassifier::class.java)
            .asEagerSingleton()

        bind(CallGraphService::class.java)
            .asEagerSingleton()
    }
}

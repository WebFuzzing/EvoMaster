package org.evomaster.core.problem.rest.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.seeding.service.rest.PirToRest

open class RestBaseModule : AbstractModule() {

    override fun configure() {

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

    }
}
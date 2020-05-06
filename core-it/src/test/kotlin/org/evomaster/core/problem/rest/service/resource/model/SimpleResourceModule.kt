package org.evomaster.core.problem.rest.service.resource.model

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.ResourceDepManageService
import org.evomaster.core.problem.rest.service.ResourceManageService
import org.evomaster.core.problem.rest.service.ResourceSampleMethodController
import org.evomaster.core.problem.rest.service.RestResourceStructureMutator
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.StructureMutator

class SimpleResourceModule : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(SimpleResourceSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(SimpleResourceSampler::class.java)
                .asEagerSingleton()

        bind(SimpleResourceSampler::class.java)
                .asEagerSingleton()

        bind(ResourceSampleMethodController::class.java)
                .asEagerSingleton()

        bind(ResourceManageService::class.java)
                .asEagerSingleton()

        bind(ResourceDepManageService::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(RestResourceStructureMutator::class.java)
                .asEagerSingleton()
    }

}
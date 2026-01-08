package org.evomaster.core.problem.rest.service.resource.model

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.rest.service.mutator.ResourceRestStructureMutator
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
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

        bind(AbstractRestSampler::class.java)
            .to(SimpleResourceSampler::class.java)
            .asEagerSingleton()


        bind(ResourceSampler::class.java)
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
                .to(ResourceRestStructureMutator::class.java)
                .asEagerSingleton()

        bind(TestCaseWriter::class.java)
                .to(RestTestCaseWriter::class.java)
                .asEagerSingleton()
    }

}
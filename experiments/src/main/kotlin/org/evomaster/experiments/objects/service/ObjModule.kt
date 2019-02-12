package org.evomaster.experiments.objects.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.*
import org.evomaster.experiments.objects.ObjIndividual


class ObjModule : AbstractModule(){

        override fun configure() {
            bind(object : TypeLiteral<Sampler<ObjIndividual>>() {})
                    .to(ObjRestSampler::class.java)
                    .asEagerSingleton()

            bind(ObjRestSampler::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<FitnessFunction<ObjIndividual>>() {})
                    .to(ObjFitness::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<Archive<ObjIndividual>>() {})
                    .asEagerSingleton()

            bind(object : TypeLiteral<Archive<*>>() {})
                    .to(object : TypeLiteral<Archive<ObjIndividual>>() {})

            bind(RemoteController::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<Mutator<ObjIndividual>>() {})
                    .to(object : TypeLiteral<StandardMutator<ObjIndividual>>() {})
                    .asEagerSingleton()

            bind(StructureMutator::class.java)
                    .to(ObjRestStructureMutator::class.java)
                    .asEagerSingleton()
        }
}
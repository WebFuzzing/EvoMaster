package org.evomaster.experiments.objects.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.EmptyStructureMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*
import org.evomaster.experiments.objects.ObjProblemDefinition
import org.evomaster.experiments.objects.ObjIndividual
import org.evomaster.experiments.objects.service.ObjFitness


class ObjModule : AbstractModule(){

        override fun configure() {
            bind(object : TypeLiteral<Sampler<ObjIndividual>>() {})
                    .to(ObjRestSampler::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<FitnessFunction<ObjIndividual>>() {})
                    .to(ObjFitness::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<Mutator<ObjIndividual>>() {})
                    .to(object : TypeLiteral<StandardMutator<ObjIndividual>>() {})
                    .asEagerSingleton()

            bind(object : TypeLiteral<Archive<ObjIndividual>>() {})
                    .asEagerSingleton()

            bind(ObjProblemDefinition::class.java)
                    .asEagerSingleton()

            bind(StructureMutator::class.java)
                    .to(EmptyStructureMutator::class.java)
                    .asEagerSingleton()
        }
}
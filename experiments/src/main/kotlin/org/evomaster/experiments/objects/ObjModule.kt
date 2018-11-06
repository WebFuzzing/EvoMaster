package org.evomaster.experiments.objects

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.ObjIndividual
import org.evomaster.core.problem.rest.service.ObjFitness
import org.evomaster.core.search.mutator.EmptyStructureMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*


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
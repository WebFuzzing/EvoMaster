package org.evomaster.experiments.pair

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.EmptyStructureMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*


class PairModule : AbstractModule(){

        override fun configure() {
            bind(object : TypeLiteral<Sampler<PairIndividual>>() {})
                    .to(PairSampler::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<FitnessFunction<PairIndividual>>() {})
                    .to(PairFitness::class.java)
                    .asEagerSingleton()

            bind(object : TypeLiteral<Mutator<PairIndividual>>() {})
                    .to(object : TypeLiteral<StandardMutator<PairIndividual>>() {})
                    .asEagerSingleton()

            bind(object : TypeLiteral<Archive<PairIndividual>>() {})
                    .asEagerSingleton()

            bind(PairProblemDefinition::class.java)
                    .asEagerSingleton()

            bind(StructureMutator::class.java)
                    .to(EmptyStructureMutator::class.java)
                    .asEagerSingleton()
        }
}
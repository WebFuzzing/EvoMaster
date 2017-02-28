package org.evomaster.core.search.algorithms.constant

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.EmptyStructureMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<ConstantIndividual>>() {})
                .to(ConstantSampler::class.java)
                .asEagerSingleton()

        bind(ConstantSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<ConstantIndividual>>() {})
                .to(ConstantFitness::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<Mutator<ConstantIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<ConstantIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<ConstantIndividual>>() {})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()
    }
}
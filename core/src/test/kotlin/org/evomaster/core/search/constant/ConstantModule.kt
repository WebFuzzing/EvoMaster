package org.evomaster.core.search.constant

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.CombinedMutator
import org.evomaster.core.search.mutator.RandomMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler

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

        bind(object : TypeLiteral<RandomMutator<ConstantIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<StandardMutator<ConstantIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<CombinedMutator<ConstantIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<ConstantIndividual>>() {})
                .asEagerSingleton()

    }
}
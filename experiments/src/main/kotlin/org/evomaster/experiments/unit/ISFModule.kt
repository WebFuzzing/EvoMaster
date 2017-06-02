package org.evomaster.experiments.unit

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*


class ISFModule(val className: String) : AbstractModule(){

    override fun configure() {

        bindConstant().annotatedWith(Names.named("className")).to(className);

        bind(object : TypeLiteral<Archive<ISFIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<ISFIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<ISFIndividual>>(){})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(ISFStructureMutator::class.java)
                .asEagerSingleton()



        bind(object : TypeLiteral<Sampler<ISFIndividual>>() {})
                .to(ISFSampler::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<FitnessFunction<ISFIndividual>>() {})
                .to(ISFFitness::class.java)
                .asEagerSingleton()
    }

}
package org.evomaster.experiments.carfast

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.name.Named
import com.google.inject.name.Names
import org.evomaster.core.search.mutator.EmptyStructureMutator
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
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()



        bind(object : TypeLiteral<Sampler<ISFIndividual>>() {})
                .to(ISFSampler::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<FitnessFunction<ISFIndividual>>() {})
                .to(ISFFitness::class.java)
                .asEagerSingleton()
    }

//    @Provides @Singleton
//    fun sampler(@Named("className") name: String): Sampler<ISFIndividual>{
//        return ISFSampler(name)
//    }
//
//    @Provides @Singleton
//    fun fitness(): FitnessFunction<ISFIndividual>{
//        return ISFFitness(className)
//    }
}
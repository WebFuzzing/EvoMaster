package org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable.IndepStableStringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class ADepDynamicStringModule : AbstractModule(){

    override fun configure() {


        bind(object : TypeLiteral<ArchiveProblemDefinition<StringIndividual>>() {})
                .to(ADepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StringProblemDefinition>() {})
                .to(IndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(ADepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

    }
}
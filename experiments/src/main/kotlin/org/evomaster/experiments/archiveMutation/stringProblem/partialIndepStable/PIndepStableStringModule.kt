package org.evomaster.experiments.archiveMutation.stringProblem.partialIndepStable

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class PIndepStableStringModule : AbstractModule(){

    override fun configure() {

        bind(object : TypeLiteral<ArchiveProblemDefinition<StringIndividual>>() {})
                .to(PIndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StringProblemDefinition>() {})
                .to(PIndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(PIndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

    }
}
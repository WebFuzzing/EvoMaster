package org.evomaster.experiments.archiveMutation.stringProblem.allIndepStable

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemConfig
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class IndepStableStringModule (
        val numTarget : Int = 1,
        val sLength : Int = 16,
        val maxLength: Int = 16,
        val rateOfImpact : Double = 1.0): AbstractModule(){

    override fun configure() {

        bind(object : TypeLiteral<ArchiveProblemDefinition<StringIndividual>>() {})
                .to(IndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StringProblemDefinition>() {})
                .to(IndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(IndepStableStringProblemDefinition::class.java)
                .asEagerSingleton()

    }

    @Provides
    @Singleton
    fun getStringProblemConfig() : StringProblemConfig {
        return StringProblemConfig(numTarget, sLength, maxLength, rateOfImpact)
    }
}
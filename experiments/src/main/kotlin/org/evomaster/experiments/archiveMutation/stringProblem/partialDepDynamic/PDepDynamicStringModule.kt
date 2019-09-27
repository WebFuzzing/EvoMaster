package org.evomaster.experiments.archiveMutation.stringProblem.partialDepDynamic

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import org.evomaster.core.search.service.mutator.geneMutation.CharPool
import org.evomaster.experiments.archiveMutation.ArchiveProblemDefinition
import org.evomaster.experiments.archiveMutation.stringProblem.StringIndividual
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemConfig
import org.evomaster.experiments.archiveMutation.stringProblem.StringProblemDefinition

/**
 * created by manzh on 2019-09-16
 */
class PDepDynamicStringModule(
        val numTarget : Int = 1,
        val sLength : Int = 16,
        val maxLength: Int = 16,
        val rateOfImpact : Double = 0.4,
        val charPool: CharPool = CharPool.ALL) : AbstractModule(){

    override fun configure() {

        bind(object : TypeLiteral<ArchiveProblemDefinition<StringIndividual>>() {})
                .to(PDepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StringProblemDefinition>() {})
                .to(PDepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(PDepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

    }
    @Provides
    @Singleton
    fun getStringProblemConfig() : StringProblemConfig {
        return StringProblemConfig(numTarget, sLength, maxLength, rateOfImpact, charPool)
    }
}
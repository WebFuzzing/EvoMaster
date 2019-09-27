package org.evomaster.experiments.archiveMutation.stringProblem.allDepDynamic

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
class ADepDynamicStringModule(
        val numTarget : Int,
        val sLength : Int,
        val maxLength: Int ,
        val rateOfImpact : Double ) : AbstractModule(){

    override fun configure() {


        bind(object : TypeLiteral<ArchiveProblemDefinition<StringIndividual>>() {})
                .to(ADepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StringProblemDefinition>() {})
                .to(ADepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(ADepDynamicStringProblemDefinition::class.java)
                .asEagerSingleton()

    }

    @Provides
    @Singleton
    fun getStringProblemConfig() : StringProblemConfig {
        return StringProblemConfig(numTarget, sLength, maxLength, rateOfImpact)
    }
}
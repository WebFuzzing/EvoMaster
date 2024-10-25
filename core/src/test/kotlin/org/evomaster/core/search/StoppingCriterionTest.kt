package org.evomaster.core.search

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.service.SearchTimeController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class StoppingCriterionTest {

    val injector: Injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()


    @Test
    fun testActionEvaluations(){
        val config = injector.getInstance(EMConfig::class.java)
        config.maxEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

        val stc = injector.getInstance(SearchTimeController::class.java)

        val numberOfActionEvaluation = 5

        stc.startSearch()
        do {
            stc.newActionEvaluation(numberOfActionEvaluation)
            stc.newIndividualEvaluation()
        } while (stc.shouldContinueSearch())
        Assertions.assertEquals(config.maxEvaluations, stc.evaluatedActions);
        Assertions.assertEquals(config.maxEvaluations / numberOfActionEvaluation, stc.evaluatedIndividuals);
    }

    @Test
    fun testIndividualEvaluations(){
        val config = injector.getInstance(EMConfig::class.java)
        config.maxEvaluations = 1000
        config.stoppingCriterion = EMConfig.StoppingCriterion.INDIVIDUAL_EVALUATIONS

        val stc = injector.getInstance(SearchTimeController::class.java)

        val numberOfActionEvaluation = 5

        stc.startSearch()
        do {
            stc.newActionEvaluation(numberOfActionEvaluation)
            stc.newIndividualEvaluation()
        } while (stc.shouldContinueSearch())
        Assertions.assertEquals(config.maxEvaluations * numberOfActionEvaluation, stc.evaluatedActions);
        Assertions.assertEquals(config.maxEvaluations, stc.evaluatedIndividuals);
    }
}